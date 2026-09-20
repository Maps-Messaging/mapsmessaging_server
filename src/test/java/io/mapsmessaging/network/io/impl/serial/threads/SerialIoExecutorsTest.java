package io.mapsmessaging.network.io.impl.serial.threads;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class SerialIoExecutorsTest {

  @Test
  void canonicalPortNameReusesSamePoolHandle() {
    SerialIoExecutors executors = SerialIoExecutors.getInstance();
    SerialIoPoolHandle first = executors.acquire("  MSG322-port-a  ");
    SerialIoPoolHandle second = executors.acquire("MSG322-port-a");

    try {
      assertSame(first, second);
      executors.release("MSG322-port-a");
      assertSame(first, executors.acquire("MSG322-port-a"));
    } finally {
      first.getReadExecutor().shutdownNow();
      first.getWriteExecutor().shutdownNow();
    }
  }

  @Test
  void readAndWriteExecutorsUseNamedDaemonThreads() throws Exception {
    SerialIoPoolHandle handle =
        SerialIoExecutors.getInstance().acquire("MSG322-port-b");

    try {
      String readName =
          handle.getReadExecutor().submit(() -> Thread.currentThread().getName())
              .get(1, TimeUnit.SECONDS);
      String writeName =
          handle.getWriteExecutor().submit(() -> Thread.currentThread().getName())
              .get(1, TimeUnit.SECONDS);
      boolean readDaemon =
          handle.getReadExecutor().submit(() -> Thread.currentThread().isDaemon())
              .get(1, TimeUnit.SECONDS);

      assertTrue(readName.startsWith("SerialRead-MSG322-port-b-"));
      assertTrue(writeName.startsWith("SerialWrite-MSG322-port-b-"));
      assertTrue(readDaemon);
    } finally {
      handle.getReadExecutor().shutdownNow();
      handle.getWriteExecutor().shutdownNow();
    }
  }

  @Test
  void nullAndBlankPortNamesAreRejected() {
    SerialIoExecutors executors = SerialIoExecutors.getInstance();

    assertThrows(NullPointerException.class, () -> executors.acquire(null));
    assertThrows(IllegalArgumentException.class, () -> executors.acquire("   "));
  }
}
