package io.github.davidms.biwenger.client;

/** Every Biwenger response except the login wraps its payload as {@code {status, data}}. */
record Envelope<T>(int status, T data) {}
