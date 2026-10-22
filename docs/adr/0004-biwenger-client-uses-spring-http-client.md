# 0004. biwenger-client may use Spring's HTTP client

- **Status:** Accepted
- **Date:** 2026-10-22
- **Issue:** #8

## Context

Supersedes the "doesn't depend on Spring Web" part of [0003](0003-monorepo-layout.md). The rest of 0003 still stands.

ADR 0003 kept `biwenger-client` free of Spring Web so that the unofficial Biwenger API stays isolated: a change in the API touches one module, and the rest of the app works with our own domain model. Issue #8 enforced this in the build by banning the `spring-web` artifact.

That artifact bundles two different things:

- Spring's HTTP **client**: `RestClient`, request interceptors, JSON message conversion and error handlers.
- Shared web types that Spring MVC's server side builds on. The server itself (controllers, `DispatcherServlet`) is in `spring-webmvc`.

Banning `spring-web` does little for isolation, because what isolates the API is the mapping layer between Biwenger's records and our domain, not the HTTP library. It does cost us `RestClient`, though. With the JDK's `java.net.http.HttpClient` we would hand-write the bearer-token header, JSON mapping, error mapping and the retry and rate-limit hooks.

## Options considered

1. **Keep the ban and use `java.net.http.HttpClient`.** The client is free of Spring entirely, but more hand-written HTTP code, for a single-user app.
2. **Allow `spring-web` only.** The client uses `RestClient` and stays a library: no Spring Boot, no auto-configuration, no web server, no database.
3. **Allow Spring Boot in the client too.** The client could configure itself with auto-configuration, but it would no longer be a plain library and would be harder to test without a Spring context.

## Decision

We will allow `org.springframework:spring-web` in `biwenger-client`, so the client uses `RestClient`.

The client still must not depend on:

- `spring-webmvc` or `spring-webflux`: no server side, no reactive stack
- Spring Boot
- Spring Data or any database library (JDBC, JPA, Hibernate, Flyway, PostgreSQL)

The `app` module builds and configures the `RestClient` (base URL, timeouts, User-Agent) and passes it to the client.

Biwenger response records are still mapped to our domain model at one boundary in `app`, as 0003 says.

The enforcer rule in `backend/biwenger-client/pom.xml` keeps these bans in the build, not only in this document.

## Consequences

- The Biwenger client gets interceptors (auth headers, re-login on 401), JSON conversion and error handlers from Spring instead of custom code.
- Client tests stay plain JUnit + WireMock. A `RestClient` can be built in a test without a Spring context.
- The client can't be reused outside a Spring project without pulling in `spring-web`, a cost we accept.
