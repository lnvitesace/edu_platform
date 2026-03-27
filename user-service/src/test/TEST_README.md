# User Service Test Suite

This directory contains comprehensive unit and integration tests for the User Service module.

## Test Structure

```
src/test/java/com/edu/platform/
├── service/
│   ├── UserServiceTest.java              # Unit tests (Mockito)
│   └── UserServiceIntegrationTest.java   # Integration tests (Spring Boot)
└── dto/
    └── DTOValidationTest.java            # DTO validation tests
```

## Test Coverage

### Unit Tests (UserServiceTest.java)

**Total Tests: 34**

Coverage includes:

#### Register Tests (6 tests)
- ✅ Successful registration with all fields
- ✅ Fail when username already exists
- ✅ Fail when email already exists
- ✅ Default to STUDENT role when role is null
- ✅ Password encoding before saving
- ✅ Different user roles (STUDENT, INSTRUCTOR, ADMIN)

#### Login Tests (4 tests)
- ✅ Successful login with valid credentials
- ✅ Updates last login time
- ✅ Caches session in Redis
- ✅ Throws exception when user not found

#### Refresh Token Tests (5 tests)
- ✅ Success with valid token
- ✅ Throws exception for invalid token
- ✅ Throws exception for expired token
- ✅ Deletes old token and creates new one
- ✅ Updates Redis cache with new token

#### Logout Tests (2 tests)
- ✅ Deletes refresh tokens and removes session
- ✅ Handles null userId gracefully

#### Get User Tests (3 tests)
- ✅ Returns user successfully
- ✅ Throws exception when user not found
- ✅ GetCurrentUser returns correct user

#### Update Profile Tests (5 tests)
- ✅ Updates all fields successfully
- ✅ Updates only provided fields (partial update)
- ✅ Throws exception when user not found
- ✅ Throws exception when profile not found
- ✅ Updates all profile fields

#### Edge Cases (9 tests)
- ✅ Handles null profile gracefully
- ✅ Creates refresh token with correct expiration
- ✅ Various role assignments
- ✅ Redis caching verification

### Integration Tests (UserServiceIntegrationTest.java)

**Total Tests: 10**

Real database and Spring context tests:

- ✅ Complete registration flow (DB + Redis + Tokens)
- ✅ Complete login flow after registration
- ✅ Login fails with wrong password
- ✅ Complete token refresh flow
- ✅ Registration with duplicate username fails
- ✅ Registration with duplicate email fails
- ✅ Complete logout flow
- ✅ Login with email instead of username
- ✅ Multiple users can register and login
- ✅ End-to-end user lifecycle

### DTO Validation Tests (DTOValidationTest.java)

**Total Tests: 12**

Jakarta Bean Validation tests:

- ✅ RegisterRequest validation
- ✅ LoginRequest validation
- ✅ UpdateProfileRequest validation
- ✅ AuthResponse builder tests
- ✅ UserResponse with nested profile tests

## Running Tests

### Prerequisites

Ensure you have:
- Java 17 or higher
- Maven 3.6+
- Redis running (for integration tests)

### Run All Tests

```bash
cd user-service
mvn test
```

### Run Specific Test Class

```bash
# Unit tests only
mvn test -Dtest=UserServiceTest

# Integration tests only
mvn test -Dtest=UserServiceIntegrationTest

# DTO validation tests only
mvn test -Dtest=DTOValidationTest
```

### Run Specific Test Method

```bash
mvn test -Dtest=UserServiceTest#register_Success
mvn test -Dtest=UserServiceIntegrationTest#testCompleteRegistrationFlow
```

### Run with Coverage Report

```bash
mvn clean test jacoco:report
```

Coverage report will be generated at: `target/site/jacoco/index.html`

### Run Tests with Verbose Output

```bash
mvn test -X
```

## Test Configuration

### Test Profile

Tests use the `test` profile with configuration in:
- `src/test/resources/application-test.yml`

Key test configurations:
- **Database**: H2 in-memory database (MySQL mode)
- **Redis**: Local Redis instance (port 6379)
- **JWT Secret**: Test-specific secret key
- **Logging**: DEBUG level for troubleshooting

### H2 Test Database

The test suite uses H2 in-memory database that:
- Mimics MySQL behavior
- Auto-creates schema from JPA entities
- Drops schema after each test class
- No manual setup required

### Redis for Tests

Integration tests require Redis running locally:

```bash
# Start Redis
redis-server

# Or with Docker
docker run -d -p 6379:6379 redis:7-alpine

# Verify Redis is running
redis-cli ping
```

## Mocking Strategy

### Unit Tests (Mockito)

- Mock all external dependencies
- Verify method calls and arguments
- Test business logic in isolation
- Fast execution (~1-2 seconds)

**Mocked Components:**
- UserRepository
- UserProfileRepository
- RefreshTokenRepository
- PasswordEncoder
- AuthenticationManager
- JwtTokenProvider
- RedisTemplate

### Integration Tests (Spring Boot)

- Use real Spring context
- Use H2 in-memory database
- Use local Redis instance
- Test complete user flows
- Slower execution (~5-10 seconds)

**Real Components:**
- Spring Security
- JPA/Hibernate
- Database transactions
- Redis caching

## Test Assertions

Tests use AssertJ for fluent assertions:

```java
// Good assertion style
assertThat(response).isNotNull();
assertThat(response.getAccessToken()).isNotEmpty();
assertThat(user.getRole()).isEqualTo(User.UserRole.STUDENT);

// Verify mock interactions
verify(userRepository).save(any(User.class));
verify(tokenProvider, never()).generateAccessToken(any());
```

## Test Data Builders

Each test uses consistent test data setup in `@BeforeEach`:

```java
@BeforeEach
void setUp() {
    testUser = User.builder()
            .id(1L)
            .username("testuser")
            .email("test@example.com")
            // ... other fields
            .build();
}
```

## Common Test Patterns

### Testing Success Path

```java
@Test
void operation_Success() {
    // Arrange - Set up mocks and test data
    when(repository.findById(1L)).thenReturn(Optional.of(testUser));

    // Act - Call the method under test
    UserResponse response = userService.getUserById(1L);

    // Assert - Verify results
    assertThat(response).isNotNull();
    verify(repository).findById(1L);
}
```

### Testing Error Scenarios

```java
@Test
void operation_NotFound_ThrowsException() {
    // Arrange
    when(repository.findById(999L)).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> userService.getUserById(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("User");
}
```

### Testing with ArgumentCaptor

```java
@Test
void operation_SavesCorrectData() {
    ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
    when(repository.save(captor.capture())).thenReturn(testUser);

    userService.someOperation();

    User savedUser = captor.getValue();
    assertThat(savedUser.getUsername()).isEqualTo("expected");
}
```

## Troubleshooting

### Redis Connection Issues

If integration tests fail with Redis connection errors:

```bash
# Check if Redis is running
redis-cli ping

# Start Redis if not running
redis-server

# Or skip integration tests
mvn test -Dtest=UserServiceTest
```

### H2 Database Issues

If you see H2 SQL errors:

1. Check `application-test.yml` for correct H2 configuration
2. Verify H2 dependency in `pom.xml`
3. Enable SQL logging to see generated queries:
   ```yaml
   spring.jpa.show-sql: true
   ```

### Mock Verification Failures

If mock verifications fail:

1. Check if method was called with correct arguments
2. Use `verify(mock, times(1))` to specify expected call count
3. Use `verify(mock, never())` to ensure method was not called
4. Enable Mockito verbose mode for debugging

## Continuous Integration

### GitHub Actions Example

```yaml
name: Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest

    services:
      redis:
        image: redis:7-alpine
        ports:
          - 6379:6379

    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '25'
          distribution: 'temurin'

      - name: Run tests
        run: mvn clean test

      - name: Generate coverage report
        run: mvn jacoco:report

      - name: Upload coverage
        uses: codecov/codecov-action@v3
```

## Test Metrics

### Expected Coverage

- **Line Coverage**: > 85%
- **Branch Coverage**: > 80%
- **Method Coverage**: > 90%

### Execution Time

- Unit tests: ~2 seconds
- Integration tests: ~10 seconds
- Total suite: ~12 seconds

## Best Practices

1. **Isolation**: Each test should be independent
2. **Clarity**: Test names should describe what they test
3. **Arrange-Act-Assert**: Follow AAA pattern
4. **One Assertion**: Test one thing per test
5. **Fast**: Keep tests fast for quick feedback
6. **Deterministic**: Tests should always produce same result
7. **Clean Up**: Use `@BeforeEach` and `@AfterEach` properly

## Adding New Tests

When adding new functionality:

1. Write unit test first (TDD)
2. Write integration test for complete flow
3. Add validation tests for new DTOs
4. Update this README if needed
5. Ensure all tests pass before committing

## Resources

- [JUnit 5 Documentation](https://junit.org/junit5/docs/current/user-guide/)
- [Mockito Documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
- [AssertJ Documentation](https://assertj.github.io/doc/)
- [Spring Boot Testing](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing)
