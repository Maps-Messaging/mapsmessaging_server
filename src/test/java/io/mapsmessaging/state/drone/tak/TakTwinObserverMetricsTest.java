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

package io.mapsmessaging.state.drone.tak;

import io.mapsmessaging.state.drone.core.TwinManager;
import io.mapsmessaging.state.drone.core.TwinUpdateContext;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.drone.model.GeoPosition;
import io.mapsmessaging.utilities.admin.JMXManager;
import io.mapsmessaging.utilities.configuration.ConfigurationManager;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import java.io.IOException;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TakTwinObserverMetricsTest {
  @Test
  void successful_publisher_handoff_completes_picture_and_mti_status() throws Exception {
    withObserver(observer -> {
      EventPublisher publisher = mock(EventPublisher.class);
      setField(observer, "eventPublisher", publisher);
      MtiStatusRegistry.setSnapshotSource(id -> new MtiStatusSnapshot("hold", Instant.now(), Instant.MAX));
      observer.onTwinAdded(positionedTwin(), context());
      PictureRecoveryTracker tracker = tracker(observer);
      assertEquals(1, tracker.stats(PictureRecoveryTracker.FailureType.RESTART,
          PictureRecoveryTracker.Stage.PICTURE).getCount());
      assertEquals(1, tracker.stats(PictureRecoveryTracker.FailureType.RESTART,
          PictureRecoveryTracker.Stage.STATUS).getCount());
      assertEquals(0, tracker.getOpenCount(PictureRecoveryTracker.FailureType.RESTART));
    });
  }

  @Test
  void publisher_failure_keeps_recovery_open() throws Exception {
    withObserver(observer -> {
      EventPublisher publisher = mock(EventPublisher.class);
      doThrow(new IOException("test publish failure")).when(publisher).publish(anyString());
      setField(observer, "eventPublisher", publisher);
      observer.onTwinAdded(positionedTwin(), context());
      assertPictureWaiting(observer);
    });
  }

  @Test
  void absent_outputs_do_not_complete_recovery() throws Exception {
    withObserver(observer -> {
      observer.onTwinAdded(positionedTwin(), context());
      assertPictureWaiting(observer);
    });
  }

  @Test
  void missing_position_does_not_count_composition_latency_or_complete_recovery() throws Exception {
    withObserver(observer -> {
      setField(observer, "eventPublisher", mock(EventPublisher.class));
      long before = TakOutputStats.getLatencyCount();
      observer.onTwinAdded(new DroneTwin("asset"), context());
      assertEquals(before, TakOutputStats.getLatencyCount());
      assertPictureWaiting(observer);
    });
  }

  @Test
  void registry_delete_stops_observer_waiting_for_status_after_picture_handoff() throws Exception {
    withObserver(observer -> {
      setField(observer, "eventPublisher", mock(EventPublisher.class));
      observer.onTwinAdded(positionedTwin(), context());
      assertEquals(1, tracker(observer).getOpenCount(PictureRecoveryTracker.FailureType.RESTART));
      MtiStatusRegistry.statusCleared("asset");
      assertEquals(0, tracker(observer).getOpenCount(PictureRecoveryTracker.FailureType.RESTART));
      assertEquals(0, tracker(observer).stats(PictureRecoveryTracker.FailureType.RESTART,
          PictureRecoveryTracker.Stage.STATUS).getNotRestoredCount());
    });
  }

  @Test
  void socket_handoff_completes_picture_without_an_internal_publisher() throws Exception {
    withObserver(observer -> {
      setField(observer, "takHost", "test-host");
      setField(observer, "takPort", 1234);
      TakTwinContext twinContext = new TakTwinContext();
      twinContext.setSocketConnection(mock(TakSocketConnection.class));
      @SuppressWarnings("unchecked")
      Map<String, TakTwinContext> contexts = (Map<String, TakTwinContext>) field(observer, "takContexts");
      contexts.put("asset", twinContext);
      observer.onTwinAdded(positionedTwin(), context());
      assertEquals(1, tracker(observer).stats(PictureRecoveryTracker.FailureType.RESTART,
          PictureRecoveryTracker.Stage.PICTURE).getCount());
      assertEquals(1, tracker(observer).getOpenCount(PictureRecoveryTracker.FailureType.RESTART));
    });
  }


  @Test
  void null_and_unidentified_updates_are_ignored_without_creating_contexts() throws Exception {
    withObserver(observer -> {
      @SuppressWarnings("unchecked")
      Map<String, TakTwinContext> contexts =
          (Map<String, TakTwinContext>) field(observer, "takContexts");

      observer.onTwinUpdated("asset", null, context());
      assertTrue(contexts.isEmpty());

      DroneTwin unidentified = mock(DroneTwin.class);
      when(unidentified.getTwinId()).thenReturn("");
      observer.onTwinUpdated(null, unidentified, context());
      assertTrue(contexts.isEmpty());
    });
  }

  @Test
  void blank_update_id_falls_back_to_twin_id_and_rate_limits_immediate_repeat() throws Exception {
    withObserver(observer -> {
      EventPublisher publisher = mock(EventPublisher.class);
      setField(observer, "eventPublisher", publisher);
      DroneTwin twin = positionedTwin();

      observer.onTwinUpdated("", twin, context());
      observer.onTwinUpdated("", twin, context());

      verify(publisher, times(1)).publish(anyString());
      @SuppressWarnings("unchecked")
      Map<String, TakTwinContext> contexts =
          (Map<String, TakTwinContext>) field(observer, "takContexts");
      assertTrue(contexts.containsKey("asset"));
    });
  }

  @Test
  void null_removal_detection_and_status_inputs_are_ignored() throws Exception {
    withObserver(observer -> {
      @SuppressWarnings("unchecked")
      Map<String, TakTwinContext> contexts =
          (Map<String, TakTwinContext>) field(observer, "takContexts");

      observer.onTwinRemoved(null, context());
      observer.onDetectionEvent(null, null, context());
      observer.onTwinStatusChanged("asset", null, null, null, context());

      assertTrue(contexts.isEmpty());
    });
  }

  private void assertPictureWaiting(TakTwinObserver observer) throws Exception {
    PictureRecoveryTracker tracker = tracker(observer);
    assertEquals(0, tracker.stats(PictureRecoveryTracker.FailureType.RESTART,
        PictureRecoveryTracker.Stage.PICTURE).getCount());
    assertEquals(1, tracker.getOpenCount(PictureRecoveryTracker.FailureType.RESTART));
  }

  private void withObserver(ObserverAction action) throws Exception {
    boolean enabled = JMXManager.isEnableJMX();
    JMXManager.setEnableJMX(false);
    ConfigurationManager config = mock(ConfigurationManager.class);
    try (MockedStatic<ConfigurationManager> mocked = mockStatic(ConfigurationManager.class)) {
      mocked.when(ConfigurationManager::getInstance).thenReturn(config);
      TakTwinObserver observer = new TakTwinObserver(new TwinManager());
      try {
        action.run(observer);
      } finally {
        observer.shutdown();
      }
    } finally {
      MtiStatusRegistry.setSnapshotSource(null);
      JMXManager.setEnableJMX(enabled);
    }
  }

  private DroneTwin positionedTwin() {
    DroneTwin twin = new DroneTwin("asset");
    twin.setGeoPosition(new GeoPosition(38.4, -9.1, 125.0, null, null));
    twin.setLastSeenAt(Instant.now());
    return twin;
  }

  private TwinUpdateContext context() {
    TwinUpdateContext context = new TwinUpdateContext();
    context.setReceivedTime(Instant.now());
    return context;
  }

  private PictureRecoveryTracker tracker(TakTwinObserver observer) throws Exception {
    return (PictureRecoveryTracker) field(observer, "pictureRecoveryTracker");
  }

  private Object field(Object target, String name) throws Exception {
    Field field = target.getClass().getDeclaredField(name);
    field.setAccessible(true);
    return field.get(target);
  }

  private void setField(Object target, String name, Object value) throws Exception {
    Field field = target.getClass().getDeclaredField(name);
    field.setAccessible(true);
    field.set(target, value);
  }

  private interface ObserverAction {
    void run(TakTwinObserver observer) throws Exception;
  }
}
