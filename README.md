Trellis Core Architecture
Trellis Core is a lightweight, enterprise-grade architectural foundation designed for Spring Boot 3 applications running on Java 21.
As codebases grow, handling failures, database transactions, and event publishing often becomes a tangled mess of try-catch blocks and unreliable "dual writes." Trellis Core solves this by encoding three powerful software design patterns into a simple, reusable library, keeping your large systems predictable, testable, and safe.
✨ Core Concepts (The Three Pillars)
1. Railway Oriented Programming (ROP)
Using Exceptions for expected business failures (e.g., "User Not Found" or "Invalid Data") makes control flow hard to read and test.
Trellis introduces a strongly-typed Result<Success, Failure> object. Workflows return an explicit outcome, forcing developers to handle failures gracefully. This makes your domain logic highly composable and extremely easy to translate into HTTP APIs or messaging responses.
2. Transactional Result Aspect
In standard Spring, a @Transactional method rolls back only if an exception is thrown. But with ROP, we aren't throwing exceptions for business failures—we return Result.failure().
Trellis bridges this gap. It includes an AOP Aspect that monitors your @Transactional methods. If a method returns a Result.failure(), Trellis automatically marks the transaction as rollback-only. Your database state is always perfectly aligned with your business logic outcomes.
3. The Transactional Outbox Pattern
Writing to a database and publishing an event to a broker (like Kafka/RabbitMQ) in the same method is known as the "Dual Write" problem. If the broker is down, the system fails.
Trellis provides primitives to save your domain mutations and an OutboxEvent in the exact same database transaction. A separate dispatcher can then read this outbox and publish events asynchronously with at-least-once delivery guarantees.
📦 Modules Included
This repository publishes com.github.niksensei2000:trellis-core, containing:
execution: The core ROP engine (Result, DomainError, Workflow, Operation, and TransactionalResultAspect).
outbox: Interfaces for the outbox pattern (OutboxEvent, OutboxStore, OutboxOperation).
event: Event primitives (Event, CommitToken, EventPublisher, EventSubscriber).
util: Shared utilities (e.g., MapperUtility for standard Jackson configuration).

🚀 Getting Started
Installation
Add the dependency to your project. (Note: Replace ${trellis.version} with the latest published release version).
Maven:
<dependency>
    <groupId>com.github.niksensei2000</groupId>
    <artifactId>trellis-core</artifactId>
    <version>${trellis.version}</version> 
</dependency>
Gradle:
implementation 'com.github.niksensei2000:trellis-core:${trellis.version}'
Spring Configuration
To ensure the TransactionalResultAspect functions correctly, you must tell Spring to scan the Trellis packages. Add this to your main application class or a configuration class:
@Configuration
@ComponentScan(basePackages = {
    "com.your.company.package", 
    "com.github.niksensei2000.trellis" // Enables Trellis Aspects and Utilities
})
public class AppConfig { }
📖 Usage Guide & Examples
Trellis relies heavily on Java 21 Pattern Matching for switch. This allows you to elegantly unpack Result objects without messy if-else chains.
Example: The Complete Workflow
Here is an example of a workflow that creates an "Incident". It demonstrates:
Validating and persisting data.
Creating an Outbox event.
Automatically rolling back if any step fails, without throwing a single exception.

package com.example.incident;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.github.niksensei2000.trellis.execution.*;
import com.github.niksensei2000.trellis.outbox.*;

@Service
public class CreateIncidentWorkflow implements Workflow<CreateIncidentCommand, IncidentId> {

    private final Operation<CreateIncidentCommand, Incident> persistIncident;
    private final OutboxOperation<Incident> incidentCreatedOutbox;
    private final OutboxStore outboxStore;

    // ... Constructor injection omitted for brevity ...

    @Override
    @Transactional
    public Result<IncidentId, DomainError> execute(CreateIncidentCommand input) {
        
        // Step 1: Execute the database persistence operation
        return switch (persistIncident.execute(input)) {
            
            // If persistence fails, immediately return the failure.
            // Trellis will automatically rollback the transaction.
            case Result.Failure<?, DomainError> failure -> Result.failure(failure.error());
            
            // If successful, proceed to create the Outbox event
            case Result.Success<Incident, DomainError> success -> {
                Incident incident = success.value();
                
                // Step 2: Create the Outbox Event payload
                yield switch (incidentCreatedOutbox.execute(incident)) {
                    
                    // If outbox creation fails, return failure and rollback the incident DB insert
                    case Result.Failure<?, DomainError> f -> Result.failure(f.error());
                    
                    // Step 3: Save Outbox event and return final Success
                    case Result.Success<OutboxEvent, DomainError> s -> {
                        outboxStore.save(s.value());
                        yield Result.success(new IncidentId(incident.id()));
                    }
                };
            }
        };
    }
}


Breaking Down the Components
Behind the workflow above, you define smaller, single-purpose Operations.
1. The Persistence Operation
Operations encapsulate single units of work. Notice how validation errors are returned as explicit DomainError objects.
@Service
class PersistIncidentOperation implements Operation<CreateIncidentCommand, Incident> {
    @Override
    public Result<Incident, DomainError> execute(CreateIncidentCommand input) {
        // Explicitly handle expected failures
        if (input.title() == null || input.title().isBlank()) {
            return Result.failure(new DomainError(
                    "incident.title.required",
                    "Title is required",
                    ErrorCategory.VALIDATION
            ));
        }
        String id = UUID.randomUUID().toString();
        // ... (Execute JDBC/JPA insert here) ...
        
        return Result.success(new Incident(id, input.title(), input.severity()));
    }
}


2. The Outbox Operation
This operation safely converts the domain object into an event envelope to be picked up by your broker dispatcher later.
@Service
class IncidentCreatedOutboxOperation implements OutboxOperation<Incident> {
    @Override
    public Result<OutboxEvent, DomainError> execute(Incident incident) {
        // Trellis MapperUtility handles Jackson serialization cleanly
        return switch (MapperUtility.serialize(incident)) {
            case Result.Failure<?, DomainError> failure -> Result.failure(failure.error());
            case Result.Success<String, DomainError> success -> {
                OutboxEvent event = new OutboxEvent(
                        incident.id(),
                        "IncidentCreated.v1", // Schema/Topic routing key
                        success.value(),      // The JSON payload
                        Map.of("schema", "IncidentCreated.v1"),
                        Instant.now()
                );
                yield Result.success(event);
            }
        };
    }
}
🛠️ Building & Development
To build the library locally and run all tests, use Maven:
mvn clean verify
To install it into your local Maven cache (~/.m2) for local testing with other projects:
mvn clean install
📦 Publishing
This library uses GitHub Actions for CI/CD.
Releases are published directly to GitHub Packages via the Publish Trellis Core workflow. This workflow triggers automatically when a new GitHub Release is published on the repository. It can also be triggered manually via the workflow_dispatch option in the Actions tab.
Ensure your settings.xml has the proper GitHub Personal Access Token (PAT) configured with read:packages (for consuming) and write:packages (for publishing) scopes.




