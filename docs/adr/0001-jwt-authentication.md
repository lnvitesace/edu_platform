# ADR-001: JWT Dual-Token Authentication

## Status

Accepted

## Context

The online education platform needs a secure and scalable authentication approach. The main considerations are:

- Stateless authentication for a microservices architecture
- User experience, especially avoiding frequent logins
- Security, including controlling token leakage risk
- Authentication propagation across services

## Decision

Adopt a JWT dual-token model using an Access Token and a Refresh Token.

**Access Token**

- Expiration: 24 hours
- Purpose: API request authentication
- Storage: frontend `localStorage`, backend Redis cache
- Claims: `userId`, `username`, `role`

**Refresh Token**

- Expiration: 7 days
- Purpose: issue a new Access Token
- Storage: database, to support revocation

**Authentication flow**

```text
1. Login -> return accessToken + refreshToken
2. API request -> Authorization: Bearer {accessToken}
3. Token expires -> use refreshToken to obtain a new accessToken
4. Logout -> delete Redis cache + database refreshToken
```

## Alternatives Considered

| Option | Pros | Cons |
|---|---|---|
| Session + Cookie | Simple to implement | Stateful, poor scalability |
| Single JWT Token | Stateless | Hard to revoke, long lifetime is less secure |
| OAuth 2.0 | Standardized, supports third-party login | More complex, over-engineered for the current stage |

## Consequences

**Benefits**

- Stateless authentication supports horizontal scaling
- Short-lived Access Tokens reduce leakage risk
- Refresh Tokens can be revoked because they are stored in the database
- Redis caching improves validation performance

**Drawbacks**

- Dual-token handling increases client complexity
- Token refresh logic must be implemented
- Redis adds infrastructure cost

## Technical Implementation

- JWT library: JJWT `0.13.0`
- Signing algorithm: `HS512`
- Secret management: environment variables

## Related Files

- `user-service/src/main/java/com/edu/platform/security/JwtTokenProvider.java`
- `user-service/src/main/java/com/edu/platform/security/JwtAuthenticationFilter.java`
- `gateway-service/src/main/java/com/edu/gateway/filter/JwtAuthenticationFilter.java`
