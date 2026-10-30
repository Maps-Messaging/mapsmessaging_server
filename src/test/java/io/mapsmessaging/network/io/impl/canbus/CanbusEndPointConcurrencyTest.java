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

package io.mapsmessaging.network.io.impl.canbus;

import com.fazecast.jSerialComm.SerialPort;
import io.mapsmessaging.canbus.device.CanCapabilities;
import io.mapsmessaging.canbus.device.CanDevice;
import io.mapsmessaging.canbus.device.SerialCanDevice;
import io.mapsmessaging.canbus.device.frames.CanFrame;
import io.mapsmessaging.dto.rest.config.network.impl.CanbusConfigDTO;
import io.mapsmessaging.network.admin.EndPointJMX;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.EOFException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CanbusEndPointConcurrencyTest {

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void grouped_frames_remain_contiguous_when_competing_with_another_writer(boolean competingGroup) throws Exception {
    BlockingRecordingDevice device = new BlockingRecordingDevice();
    CanbusEndPoint endpoint = endpoint(device);
    ExecutorService writers = Executors.newFixedThreadPool(2);
    CountDownLatch competitorStarted = new CountDownLatch(1);

    try {
      Future<Void> firstGroup = writers.submit(() -> {
        endpoint.writeFrames(List.of(frame(10), frame(11), frame(12)));
        return null;
      });
      assertTrue(device.firstFrameWritten.await(5, TimeUnit.SECONDS));
      Future<Void> competitor = writers.submit(() -> {
        competitorStarted.countDown();
        if (competingGroup) {
          endpoint.writeFrames(List.of(frame(20), frame(21)));
        } else {
          endpoint.writeFrame(frame(20));
        }
        return null;
      });

      assertTrue(competitorStarted.await(5, TimeUnit.SECONDS));
      assertFalse(device.competitorWritten.await(200, TimeUnit.MILLISECONDS), "Another writer must wait for the entire active group");
      device.releaseFirstGroup.countDown();
      firstGroup.get(5, TimeUnit.SECONDS);
      competitor.get(5, TimeUnit.SECONDS);

      List<Integer> expected = competingGroup ? List.of(10, 11, 12, 20, 21) : List.of(10, 11, 12, 20);
      assertEquals(expected, device.identifiers);
    } finally {
      device.releaseFirstGroup.countDown();
      writers.shutdownNow();
      try {
        assertTrue(writers.awaitTermination(5, TimeUnit.SECONDS));
      } finally {
        endpoint.close();
      }
    }
  }

  private static CanFrame frame(int identifier) {
    return new CanFrame(identifier, true, 8, new byte[8]);
  }

  private static final class BlockingRecordingDevice implements CanDevice {

    private final List<Integer> identifiers = new CopyOnWriteArrayList<>();
    private final CountDownLatch firstFrameWritten = new CountDownLatch(1);
    private final CountDownLatch releaseFirstGroup = new CountDownLatch(1);
    private final CountDownLatch competitorWritten = new CountDownLatch(1);

    @Override
    public CanFrame readFrame() {
      return null;
    }

    @Override
    public void writeFrame(CanFrame frame) throws IOException {
      identifiers.add(frame.canIdentifier());
      if (frame.canIdentifier() == 10) {
        firstFrameWritten.countDown();
        try {
          if (!releaseFirstGroup.await(5, TimeUnit.SECONDS)) {
            throw new IOException("Timed out waiting to release the first group");
          }
        } catch (InterruptedException exception) {
          Thread.currentThread().interrupt();
          throw new IOException(exception);
        }
      } else if (frame.canIdentifier() == 20) {
        competitorWritten.countDown();
      }
    }

    @Override
    public CanCapabilities getCanCapabilities() {
      return new CanCapabilities(false, false, 8, 8);
    }

    @Override
    public void close() {
    }
  }

  @Test
  void eof_from_replaced_device_preserves_current_binding() throws Exception {
    SerialCanDevice oldDevice = mock(SerialCanDevice.class);
    CanDevice replacement = mock(CanDevice.class);
    CanbusEndPoint endpoint = endpoint(oldDevice);
    CanbusConfigDTO serialConfig = new CanbusConfigDTO();
    serialConfig.setSerialConfig(mock(io.mapsmessaging.dto.rest.config.network.impl.SerialConfigDTO.class,
        RETURNS_DEEP_STUBS));
    set(endpoint, "config", serialConfig);
    SerialPort oldPort = port("old");
    SerialPort newPort = port("new");
    set(endpoint, "activeSerialPort", oldPort);
    set(endpoint, "activeSerialPortName", "old");
    CountDownLatch readStarted = new CountDownLatch(1);
    CountDownLatch replacementInstalled = new CountDownLatch(1);
    when(oldDevice.readFrame()).thenAnswer(invocation -> {
      readStarted.countDown();
      assertTrue(replacementInstalled.await(5, TimeUnit.SECONDS));
      throw new EOFException();
    });

    try (var executor = Executors.newSingleThreadExecutor()) {
      var read = executor.submit(endpoint::readFrame);
      try {
        assertTrue(readStarted.await(5, TimeUnit.SECONDS));
        synchronized (get(endpoint, "deviceLock")) {
          set(endpoint, "canDevice", replacement);
          set(endpoint, "activeSerialPort", newPort);
          set(endpoint, "activeSerialPortName", "new");
        }
      } finally {
        replacementInstalled.countDown();
      }
      assertNull(read.get(5, TimeUnit.SECONDS));
    }

    CanFrame frame = new CanFrame(1, false, 1, new byte[]{1});
    endpoint.writeFrame(frame);
    verify(replacement).writeFrame(frame);
    verify(replacement, never()).close();
    verify(newPort, never()).closePort();
  }

  @Test
  void concurrent_frame_and_batch_writes_do_not_overlap() throws Exception {
    CanDevice device = mock(CanDevice.class);
    CanbusEndPoint endpoint = endpoint(device);
    CanFrame frame = new CanFrame(1, false, 1, new byte[]{1});
    CountDownLatch firstEntered = new CountDownLatch(1);
    CountDownLatch releaseFirst = new CountDownLatch(1);
    CountDownLatch secondEntered = new CountDownLatch(1);
    CountDownLatch secondStarted = new CountDownLatch(1);
    doAnswer(invocation -> {
      firstEntered.countDown();
      assertTrue(releaseFirst.await(5, TimeUnit.SECONDS));
      return null;
    }).when(device).writeFrame(frame);
    doAnswer(invocation -> {
      secondEntered.countDown();
      return null;
    }).when(device).writeFrames(anyList());

    try (var executor = Executors.newFixedThreadPool(2)) {
      var first = executor.submit(() -> { endpoint.writeFrame(frame); return null; });
      assertTrue(firstEntered.await(5, TimeUnit.SECONDS));
      var second = executor.submit(() -> {
        secondStarted.countDown();
        endpoint.writeFrames(java.util.List.of(frame));
        return null;
      });
      try {
        assertTrue(secondStarted.await(5, TimeUnit.SECONDS));
        assertFalse(secondEntered.await(200, TimeUnit.MILLISECONDS), "A CAN batch must wait for the active frame write");
      } finally {
        releaseFirst.countDown();
      }
      first.get(5, TimeUnit.SECONDS);
      second.get(5, TimeUnit.SECONDS);
      assertEquals(0, secondEntered.getCount());
    }
  }

  private static CanbusEndPoint endpoint(CanDevice device) throws Exception {
    CanbusEndPoint endpoint = mock(CanbusEndPoint.class, CALLS_REAL_METHODS);
    set(endpoint, "closed", new AtomicBoolean(false));
    set(endpoint, "deviceLock", new Object());
    set(endpoint, "canDevice", device);
    set(endpoint, "mbean", mock(EndPointJMX.class));
    set(endpoint, "config", mock(CanbusConfigDTO.class));
    return endpoint;
  }

  private static SerialPort port(String name) {
    SerialPort port = mock(SerialPort.class);
    when(port.getSystemPortName()).thenReturn(name);
    return port;
  }

  private static void set(Object target, String name, Object value) throws Exception {
    Field field = CanbusEndPoint.class.getDeclaredField(name);
    field.setAccessible(true);
    field.set(target, value);
  }

  private static Object get(Object target, String name) throws Exception {
    Field field = CanbusEndPoint.class.getDeclaredField(name);
    field.setAccessible(true);
    return field.get(target);
  }
}
