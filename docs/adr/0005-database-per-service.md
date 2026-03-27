# ADR-005: Database per Service

## Status

Accepted

## Context

Data management in a microservices architecture creates trade-offs:

- Shared database vs. independent databases
- Data consistency vs. service autonomy
- Development speed vs. long-term maintainability

## Decision

Adopt the Database per Service pattern so that each microservice owns its own database.

**Database allocation**

| Service | Database | Main Tables |
|---|---|---|
| user-service | edu_user | users, user_profiles, refresh_tokens |
| course-service | edu_course | courses, chapters, lessons |
| learning-service | edu_learning | learning_progress, favorites |

**Cross-service data access**

- Use REST APIs to fetch data owned by other services
- Provide internal endpoints such as `/internal/*` for service-to-service access
- Cache frequently accessed remote data when necessary

## Alternatives Considered

| Option | Pros | Cons |
|---|---|---|
| Shared database | Simple, strong consistency | Tight coupling, hard to deploy independently |
| One database per service | Service autonomy, independent scaling | Cross-service queries are more complex |
| Shared subset of tables | Compromise approach | Blurry boundaries, poor long-term maintainability |

## Consequences

**Benefits**

- Services are fully independent for deployment and scaling
- Each service can choose the most suitable database type, such as MySQL or MongoDB
- Database changes in one service do not directly affect others
- Service boundaries stay explicit

**Drawbacks**

- Cross-service queries require API aggregation
- Distributed transactions are harder to manage
- Some data duplication is unavoidable, such as cached usernames in multiple services

## Cross-Service Data Access Patterns

**1. API call, synchronous**

```java
// course-service fetches instructor information
@FeignClient("user-service")
public interface UserServiceClient {
    @GetMapping("/internal/users/{id}")
    UserDTO getUserById(@PathVariable Long id);
}
```

**2. Event-driven, asynchronous**

```java
// Course publication event
@RabbitListener(queues = "course.published")
public void handleCoursePublished(CourseEvent event) {
    // Update the search index, notify subscribers, and so on
}
```

**3. Data duplication, denormalized**

```sql
-- learning_progress stores a redundant course title
course_id BIGINT,
course_title VARCHAR(255),
```

## Consistency Strategy

**Eventual consistency**

- Use the Saga pattern for cross-service transactions
- Use the Outbox pattern for reliable message publishing
- Accept short-lived inconsistency where the business can tolerate it

**Compensation mechanism**

```text
Order creation saga:
1. Create order (pending)
2. Deduct inventory -> cancel order if it fails
3. Create payment record -> roll back inventory if it fails
4. Payment succeeds -> mark order complete
```

## Related Files

- `docker/mysql/init/01-init-databases.sql` - database initialization
- `user-service/src/main/resources/application.yml` - user-service datasource
- `course-service/src/main/resources/application.yml` - course-service datasource
