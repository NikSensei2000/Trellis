# Trellis Core Architecture

Trellis Core is an open-source, enterprise-grade architectural foundation for Spring Boot&nbsp;3
applications on Java&nbsp;21. It encodes three ideas that keep large systems predictable:

1. **Railway Oriented Programming (ROP)** ? Business workflows return an explicit `Result` instead
   of using exceptions for expected failure. That makes control flow composable, testable, and easy
   to translate into HTTP or messaging responses.

2. **Transactional outbox** ? Domain mutations and outbox rows commit together, so you never rely
   on ?best effort? dual writes to a database and a broker. A dispatcher publishes later from the
   outbox with at-least-once guarantees.

3. **Transactional `Result` aspect** ? Spring rolls back on many exceptions, but not when a method
   returns `Result.failure(...)` normally. Trellis marks the transaction rollback-only when a
   transactional method returns a failure outcome, aligning database state with the ROP story.

## Modules in this repository

This repository publishes `com.github.niksensei2000:trellis-core`, containing:

- `com.github.niksensei2000.trellis.execution` ? `Result`, `DomainError`, `Workflow`, `Operation`, and
  `TransactionalResultAspect`
- `com.github.niksensei2000.trellis.outbox` ? `OutboxEvent`, `OutboxStore`, `OutboxOperation`
- `com.github.niksensei2000.trellis.event` ? `Event`, `CommitToken`, `EventPublisher`, `EventSubscriber`
- `com.github.niksensei2000.trellis.util` ? `MapperUtility` (shared Jackson configuration)

Enable component scanning for `com.github.niksensei2000.trellis` (or register `TransactionalResultAspect` as a
bean) so the aspect participates in your application context.

## Example: `CreateIncidentWorkflow`

The following example shows a workflow that persists an incident, builds an outbox event, saves it
in the same transaction, and returns `Result.success`. If any step returns `Result.failure`, the
transactional aspect rolls the transaction back even though no exception was thrown.

```java
package com.example.incident;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.niksensei2000.trellis.execution.DomainError;
import com.github.niksensei2000.trellis.execution.ErrorCategory;
import com.github.niksensei2000.trellis.execution.Operation;
import com.github.niksensei2000.trellis.execution.Result;
import com.github.niksensei2000.trellis.execution.Workflow;
import com.github.niksensei2000.trellis.outbox.OutboxEvent;
import com.github.niksensei2000.trellis.outbox.OutboxOperation;
import com.github.niksensei2000.trellis.outbox.OutboxStore;
import com.github.niksensei2000.trellis.util.MapperUtility;

@Service
public class CreateIncidentWorkflow implements Workflow<CreateIncidentCommand, IncidentId> {

    private final Operation<CreateIncidentCommand, Incident> persistIncident;
    private final OutboxOperation<Incident> incidentCreatedOutbox;
    private final OutboxStore outboxStore;

    public CreateIncidentWorkflow(
            Operation<CreateIncidentCommand, Incident> persistIncident,
            OutboxOperation<Incident> incidentCreatedOutbox,
            OutboxStore outboxStore
    ) {
        this.persistIncident = persistIncident;
        this.incidentCreatedOutbox = incidentCreatedOutbox;
        this.outboxStore = outboxStore;
    }

    @Override
    @Transactional
    public Result<IncidentId, DomainError> execute(CreateIncidentCommand input) {
        return switch (persistIncident.execute(input)) {
            case Result.Failure<?, DomainError> failure -> Result.failure(failure.error());
            case Result.Success<Incident, DomainError> success -> {
                Incident incident = success.value();
                yield switch (incidentCreatedOutbox.execute(incident)) {
                    case Result.Failure<?, DomainError> f -> Result.failure(f.error());
                    case Result.Success<OutboxEvent, DomainError> s -> {
                        outboxStore.save(s.value());
                        yield Result.success(new IncidentId(incident.id()));
                    }
                };
            }
        };
    }
}

/** Command handled by the workflow (example). */
public record CreateIncidentCommand(String title, String severity) {
}

/** Persisted aggregate (example). */
public record Incident(String id, String title, String severity) {
}

/** Strongly typed identifier returned to callers (example). */
public record IncidentId(String value) {
}

/** DB operation: validates and inserts the incident (example implementation). */
@Service
class PersistIncidentOperation implements Operation<CreateIncidentCommand, Incident> {
    @Override
    public Result<Incident, DomainError> execute(CreateIncidentCommand input) {
        if (input.title() == null || input.title().isBlank()) {
            return Result.failure(new DomainError(
                    "incident.title.required",
                    "Title is required",
                    ErrorCategory.VALIDATION
            ));
        }
        String id = UUID.randomUUID().toString();
        // ... JDBC/JPA repository save here ...
        return Result.success(new Incident(id, input.title(), input.severity()));
    }
}

/** Builds the outbox envelope for IncidentCreated (example implementation). */
@Service
class IncidentCreatedOutboxOperation implements OutboxOperation<Incident> {
    @Override
    public Result<OutboxEvent, DomainError> execute(Incident incident) {
        return switch (MapperUtility.serialize(incident)) {
            case Result.Failure<?, DomainError> failure -> Result.failure(failure.error());
            case Result.Success<String, DomainError> success -> {
                String json = success.value();
                OutboxEvent event = new OutboxEvent(
                        incident.id(),
                        "IncidentCreated.v1",
                        json,
                        Map.of("schema", "IncidentCreated.v1"),
                        Instant.now()
                );
                yield Result.success(event);
            }
        };
    }
}
```

If `PersistIncidentOperation` returns a validation failure, `CreateIncidentWorkflow` returns that
failure and `TransactionalResultAspect` marks the transaction rollback-only; no partial rows remain.
The same happens if outbox serialization fails or the outbox event cannot be constructed.

## Building locally

```bash
mvn -q verify
```

## Publishing

Releases are published to GitHub Packages via the ?Publish Trellis Core? workflow when a GitHub
Release is published, or when the workflow is run manually.
