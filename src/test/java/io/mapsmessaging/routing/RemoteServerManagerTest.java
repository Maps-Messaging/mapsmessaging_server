package io.mapsmessaging.routing;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.concurrent.ScheduledFuture;

import static org.junit.jupiter.api.Assertions.*;

class RemoteServerManagerTest {

  @Test
  void pauseAndResumeControlScheduledPollingLifecycle() throws Exception {
    RemoteServerManager manager =
        new RemoteServerManager("http://127.0.0.1:1", false);

    try {
      assertNotNull(scheduledFuture(manager));

      manager.pause();
      assertNull(scheduledFuture(manager));

      manager.resume();
      ScheduledFuture<?> resumed = scheduledFuture(manager);
      assertNotNull(resumed);
      assertFalse(resumed.isCancelled());

      manager.stop();
      assertNull(scheduledFuture(manager));
      assertTrue(resumed.isCancelled());
    } finally {
      manager.stop();
    }
  }

  private static ScheduledFuture<?> scheduledFuture(RemoteServerManager manager)
      throws Exception {
    Field field = RemoteServerManager.class.getDeclaredField("scheduledFuture");
    field.setAccessible(true);
    return (ScheduledFuture<?>) field.get(manager);
  }
}
