/**
 * Client for the unofficial Biwenger API: HTTP calls, authentication, rate limiting and the
 * response records. It uses Spring's {@code RestClient} but no Spring Boot, server side or database
 * (ADR 0004), and the rest of the app depends on our own domain model, not on Biwenger's JSON (ADR
 * 0003).
 *
 * <p>Implemented in Phase 2. See {@code docs/biwenger-api/} and the {@code biwenger-api} skill.
 */
package io.github.davidms.biwenger.client;
