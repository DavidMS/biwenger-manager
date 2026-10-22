package io.github.davidms.biwenger.client.model;

import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Why a player didn't play a match: {@code injured}, {@code doubt}, {@code sanctioned} or {@code
 * ok}.
 */
public record ReportStatus(String status, @Nullable String statusInfo) {

  public ReportStatus {
    Objects.requireNonNull(status, "status");
  }
}
