package io.mapsmessaging.hardware.trigger;

import org.junit.jupiter.api.Test;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;

import static org.mockito.Mockito.*;

class CronJobTest {

  @Test
  void executeRunsActionsOnConfiguredTrigger() throws Exception {
    Trigger trigger = mock(Trigger.class);
    JobDataMap data = new JobDataMap();
    data.put("trigger", trigger);
    JobExecutionContext context = mock(JobExecutionContext.class);
    when(context.getMergedJobDataMap()).thenReturn(data);

    new CronJob().execute(context);

    verify(trigger).runActions();
  }
}
