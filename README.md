<div align="center">

# 🌿 Trellis Core

**A lightweight, enterprise-grade architectural foundation for Spring Boot 3 + Java 21**

[![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen?logo=springboot)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/github/license/niksensei2000/trellis)](LICENSE)
[![GitHub Packages](https://img.shields.io/badge/Published%20on-GitHub%20Packages-blue?logo=github)](https://github.com/niksensei2000/trellis/packages)

*Stop wrestling with try-catch spaghetti and unreliable dual writes. Trellis encodes three battle-tested patterns into one reusable library — keeping your systems predictable, testable, and safe.*

</div>

---

## ✨ The Three Pillars

### 1. Railway Oriented Programming (ROP)

Using exceptions for expected business failures (e.g., "User Not Found") makes control flow hard to read and test. Trellis introduces a strongly-typed `Result<Success, Failure>` object. Workflows return an explicit outcome, forcing you to handle failures gracefully — making domain logic composable and trivial to map to HTTP or messaging responses.

### 2. Transactional Result Aspect

In standard Spring, `@Transactional` rolls back only when an exception is thrown. With ROP, you return `Result.failure()` instead. Trellis bridges this gap with an AOP Aspect that monitors your `@Transactional` methods — if a method returns `Result.failure()`, Trellis **automatically marks the transaction as rollback-only**. Your database state stays perfectly aligned with your business logic.

### 3. Transactional Outbox Pattern

Writing to a database and publishing to a broker (Kafka, RabbitMQ) in the same method is the classic "Dual Write" problem. If the broker is down, you're in trouble. Trellis provides primitives to save domain mutations and an `OutboxEvent` in the **exact same transaction**. A separate dispatcher then publishes events asynchronously with **at-least-once delivery guarantees**.

---

## 📦 Modules

`com.github.niksensei2000:trellis-core` ships four packages:

| Package | Contents |
|---|---|
| `execution` | Core ROP engine — `Result`, `DomainError`, `Workflow`, `Operation`, `TransactionalResultAspect` |
| `outbox` | Outbox pattern interfaces — `OutboxEvent`, `OutboxStore`, `OutboxOperation` |
| `event` | Event primitives — `Event`, `CommitToken`, `EventPublisher`, `EventSubscriber` |
| `util` | Shared utilities — `MapperUtility` (standard Jackson configuration) |

---

## 🚀 Getting Started

### Installation

> Replace `${trellis.version}` with the latest published release.

**Maven**

```xml
<dependency>
   <groupId>com.github.niksensei2000</groupId>
   <artifactId>trellis-core</artifactId>
   <version>${trellis.version}</version>
</dependency>
```

**Gradle**

```groovy
implementation 'com.github.niksensei2000:trellis-core:${trellis.version}'
```

### Spring Configuration

Tell Spring to scan the Trellis packages so the `TransactionalResultAspect` is picked up correctly:

```java
@Configuration
@ComponentScan(basePackages = {
        "com.your.company.package",
        "com.github.niksensei2000.trellis"
})
public class AppConfig { }
```

---

## 📖 Usage Guide

Trellis leans on **Java 21 pattern matching for `switch`** to unpack `Result` objects cleanly — no messy if-else chains.

### The Complete Workflow

The example below creates an `Incident`. It shows:
- Validating and persisting data
- Creating an outbox event
- Automatic rollback if any step fails — **zero exceptions thrown**

```java
@Service
public class CreateIncidentWorkflow implements Workflow<CreateIncidentCommand, IncidentId> {

   private final Operation<CreateIncidentCommand, Incident> persistIncident;
   private final OutboxOperation<Incident> incidentCreatedOutbox;
   private final OutboxStore outboxStore;

   @Override
   @Transactional
   public Result<IncidentId, DomainError> execute(CreateIncidentCommand input) {

      // Step 1: persist — any failure triggers automatic rollback via Trellis
      return switch (persistIncident.execute(input)) {

         case Result.Failure<?, DomainError> failure ->
                 Result.failure(failure.error());

         case Result.Success<Incident, DomainError> success -> {
            Incident incident = success.value();

            // Step 2: build the outbox event
            yield switch (incidentCreatedOutbox.execute(incident)) {

               case Result.Failure<?, DomainError> f ->
                       Result.failure(f.error());

               // Step 3: save outbox record and return success
               case Result.Success<OutboxEvent, DomainError> s -> {
                  outboxStore.save(s.value());
                  yield Result.success(new IncidentId(incident.id()));
               }
            };
         }
      };
   }
}
```

---

### The Persistence Operation

Operations encapsulate a single unit of work. Validation failures are explicit `DomainError` values — no exceptions.

```java
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
      // execute JDBC/JPA insert here

      return Result.success(new Incident(id, input.title(), input.severity()));
   }
}
```

### The Outbox Operation

Converts a domain object into an event envelope for the broker dispatcher to pick up later.

```java
@Service
class IncidentCreatedOutboxOperation implements OutboxOperation<Incident> {

   @Override
   public Result<OutboxEvent, DomainError> execute(Incident incident) {

      return switch (MapperUtility.serialize(incident)) {

         case Result.Failure<?, DomainError> failure ->
                 Result.failure(failure.error());

         case Result.Success<String, DomainError> success -> {
            OutboxEvent event = new OutboxEvent(
                    incident.id(),
                    "IncidentCreated.v1",
                    success.value(),
                    Map.of("schema", "IncidentCreated.v1"),
                    Instant.now()
            );
            yield Result.success(event);
         }
      };
   }
}
```

---

## 🛠️ Building & Development

Build and run all tests:

```bash
mvn clean verify
```

Install to your local Maven cache for testing with other projects:

```bash
mvn clean install
```

---

## 📦 Publishing

CI/CD runs on **GitHub Actions**. Releases are published to **GitHub Packages** via the *Publish Trellis Core* workflow, which triggers automatically on a new GitHub Release or can be run manually via `workflow_dispatch`.

Your `settings.xml` needs a GitHub Personal Access Token (PAT) with:
- `read:packages` — for consuming the library
- `write:packages` — for publishing releases