package io.github.davidms.biwenger.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.davidms.biwenger.client.BiwengerClientResilienceTest.FakeTime;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class RateLimiterTest {

  private final FakeTime time = new FakeTime();
  private final RateLimiter limiter = new RateLimiter(Duration.ofMillis(500), time, time);

  @Test
  void waitsOnlyForWhatIsLeftOfTheInterval() throws IOException {
    limiter.acquire();
    time.advance(Duration.ofMillis(300));
    limiter.acquire();

    assertThat(time.pauses).containsExactly(Duration.ofMillis(200));
  }

  @Test
  void doesNotWaitAfterAQuietPeriod() throws IOException {
    limiter.acquire();
    time.advance(Duration.ofSeconds(5));
    limiter.acquire();

    assertThat(time.pauses).isEmpty();
  }

  @Test
  void anInterruptStopsTheWaitAndIsKept() throws IOException {
    var interrupted =
        new RateLimiter(
            Duration.ofMillis(500),
            () -> 0,
            duration -> {
              throw new InterruptedException();
            });
    interrupted.acquire();

    assertThatThrownBy(interrupted::acquire).isInstanceOf(InterruptedIOException.class);
    assertThat(Thread.interrupted()).isTrue(); // also clears the flag for the next test
  }
}
