/**
 * Biwenger's JSON as Java records, one per object we read (docs/biwenger-api/). They mirror the
 * JSON: names follow it, money is whole euros, times are Unix seconds, a value Biwenger leaves out
 * is {@code null} (marked {@code @Nullable}) and a list it leaves out is empty. The app maps them
 * to its own domain in {@code app.biwenger} (ADR 0003).
 */
@NullMarked
package io.github.davidms.biwenger.client.model;

import org.jspecify.annotations.NullMarked;
