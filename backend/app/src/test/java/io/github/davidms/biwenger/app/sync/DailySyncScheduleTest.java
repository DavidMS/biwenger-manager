package io.github.davidms.biwenger.app.sync;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.davidms.biwenger.app.TestcontainersConfiguration;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.config.ScheduledTaskHolder;

/** The schedule comes from configuration, and can be switched off. */
class DailySyncScheduleTest {

  @Nested
  @SpringBootTest(properties = "biwenger.sync.cron=0 15 8 * * *")
  @Import(TestcontainersConfiguration.class)
  class Configured {

    @Autowired ApplicationContext context;

    @Test
    void runsAtTheConfiguredTime() {
      var crons =
          context.getBeansOfType(ScheduledTaskHolder.class).values().stream()
              .flatMap(holder -> holder.getScheduledTasks().stream())
              .map(scheduled -> scheduled.getTask())
              .filter(CronTask.class::isInstance)
              .map(task -> ((CronTask) task).getExpression())
              .toList();

      assertThat(crons).containsExactly("0 15 8 * * *");
    }
  }

  @Nested
  @SpringBootTest(properties = "biwenger.sync.enabled=false")
  @Import(TestcontainersConfiguration.class)
  class Disabled {

    @Autowired ApplicationContext context;

    @Test
    void hasNoScheduler() {
      assertThat(context.getBeansOfType(DailySyncScheduler.class)).isEmpty();
    }
  }
}
