/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      https://commonsclause.com/
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.mapsmessaging.ha;

import io.mapsmessaging.utilities.GsonFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class FileLockManagerTest {

  @TempDir
  Path tempDir;

  @Test
  void tryAcquireLockWithTakeover_createsLockFile_andHeartbeatFile_andWritesPid() throws Exception {
    Path lockFile = tempDir.resolve("maps.lock");

    AtomicInteger exitCode = new AtomicInteger(Integer.MIN_VALUE);

    try (FileLockManager manager = new FileLockManager(lockFile, 300, exitCode::set)) {
      boolean acquired = manager.tryAcquireLockWithTakeover();
      assertTrue(acquired);
      assertTrue(manager.isLocked());
      assertEquals(Integer.MIN_VALUE, exitCode.get(), "exit should not be called on normal acquisition");

      assertTrue(Files.exists(lockFile), "lock file should exist");

      Path heartbeat = Path.of(lockFile + ".heartbeat");
      assertTrue(waitUntilExists(heartbeat, 2_000), "heartbeat file should appear");

      String content = Files.readString(heartbeat).trim();
      assertFalse(content.isEmpty(), "heartbeat should contain JSON");

      LockInfo info = GsonFactory.getInstance().getSimpleGson().fromJson(content, LockInfo.class);
      assertNotNull(info);

      assertEquals(ProcessHandle.current().pid(), info.getPid(), "heartbeat should contain our pid");
      assertNotNull(info.getHostname());
      assertNotNull(info.getStarted());
      assertNotNull(info.getVersion());
      assertNotNull(info.getBuildDate());
      assertNotNull(info.getLastHeartbeat());

      OffsetDateTime.parse(info.getLastHeartbeat());
    }
  }


  @Test
  void close_releasesLock_andDeletesHeartbeat_andDeletesStopSignal() throws Exception {
    Path lockFile = tempDir.resolve("maps.lock");

    FileLockManager manager = new FileLockManager(lockFile, 300, code ->{});
    assertTrue(manager.tryAcquireLockWithTakeover());

    Path heartbeat = Path.of(lockFile + ".heartbeat");
    assertTrue(waitUntilExists(heartbeat, 2_000));

    Path stopSignal = Path.of(lockFile + ".stop");
    Files.writeString(stopSignal, "stop");
    assertTrue(Files.exists(stopSignal));

    manager.close();

    assertFalse(manager.isLocked());
    assertFalse(Files.exists(heartbeat), "heartbeat should be deleted on close");
    assertFalse(Files.exists(stopSignal), "stop signal should be deleted on close");
  }

  @Test
  void secondManager_inSameJvm_shouldNotCrash_andShouldReturnFalseWhenShutdown_requested() throws Exception {
    Path lockFile = tempDir.resolve("maps.lock");

    FileLockManager manager1 = new FileLockManager(lockFile, 2_000, code ->{});
    assertTrue(manager1.tryAcquireLockWithTakeover());
    assertTrue(manager1.isLocked());

    FileLockManager manager2 = new FileLockManager(lockFile, 2_000, code ->{});

    AtomicReference<Throwable> thrown = new AtomicReference<>();
    AtomicReference<Boolean> result = new AtomicReference<>(null);
    CountDownLatch started = new CountDownLatch(1);
    Thread t = new Thread(() -> {
      started.countDown();
      try {
        result.set(manager2.tryAcquireLockWithTakeover());
      } catch (Throwable t1) {
        thrown.set(t1);
      }
    }, "lock-attempt-2");
    t.setDaemon(true);
    t.start();

    assertTrue(started.await(2, TimeUnit.SECONDS));
    Thread.sleep(200);
    manager2.shutdown();

    t.join(3_000);

    // This is the key enforcement:
    // A second attempt in the same JVM frequently throws OverlappingFileLockException.
    // That should NOT blow up the manager. It should behave like "lock not available".
    Throwable t1 = thrown.get();
    if (t1 != null) {
      // Make the failure loud and specific so it’s obvious what needs fixing.
      if (t1 instanceof OverlappingFileLockException) {
        fail("Second lock attempt in same JVM threw OverlappingFileLockException. " +
            "tryAcquireLockWithTakeover() should treat it like 'lock unavailable' and retry/sleep.");
      }
      fail("Unexpected exception from second manager: " + t1.getClass().getName() + " " + t1.getMessage());
    }

    assertNotNull(result.get(), "second manager should have returned");
    assertFalse(result.get(), "second manager should not acquire while first holds");
    assertFalse(manager2.isLocked());

    manager2.close();
    manager1.close();
  }

  @Test
  void tryAcquireLockWithTakeover_deletesStaleStopSignalFile_whenAcquired() throws Exception {
    Path lockFile = tempDir.resolve("maps.lock");
    Path stopSignal = Path.of(lockFile + ".stop");

    Files.writeString(stopSignal, "stale");
    assertTrue(Files.exists(stopSignal));

    try (FileLockManager manager = new FileLockManager(lockFile, 300, code -> { })) {
      assertTrue(manager.tryAcquireLockWithTakeover());
      assertFalse(Files.exists(stopSignal), "stale stop file should be deleted on successful acquisition");
    }
  }

  private static boolean waitUntilExists(Path file, long timeoutMillis) throws InterruptedException {
    long end = System.currentTimeMillis() + timeoutMillis;
    while (System.currentTimeMillis() < end) {
      if (Files.exists(file)) {
        return true;
      }
      Thread.sleep(25);
    }
    return false;
  }
}
