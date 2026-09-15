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

package io.mapsmessaging.network.protocol.impl.mavlink;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.mapsmessaging.dto.rest.config.protocol.impl.MavlinkConfigDTO;
import io.mapsmessaging.mavlink.ProcessedFrame;
import io.mapsmessaging.mavlink.message.Frame;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MavlinkFeedbackTargetFilterTest {

  private static final int LOCAL_SYSTEM_ID = 103;
  private static final int LOCAL_COMPONENT_ID = 190;

  @Test
  void acceptsMissionFeedbackAddressedToLocalIdentity() {
    assertTrue(MavlinkFeedbackTargetFilter.allow(
        frame(40, Map.of("target_system", LOCAL_SYSTEM_ID, "target_component", LOCAL_COMPONENT_ID)),
        config()));
  }

  @Test
  void rejectsMissionFeedbackAddressedToOtherGroundControl() {
    assertFalse(MavlinkFeedbackTargetFilter.allow(
        frame(40, Map.of("target_system", 250, "target_component", 194)),
        config()));
    assertFalse(MavlinkFeedbackTargetFilter.allow(
        frame(47, Map.of("target_system", 250, "target_component", 194)),
        config()));
    assertFalse(MavlinkFeedbackTargetFilter.allow(
        frame(51, Map.of("target_system", 250, "target_component", 194)),
        config()));
    assertFalse(MavlinkFeedbackTargetFilter.allow(
        frame(77, Map.of("target_system", 250, "target_component", 194)),
        config()));
  }

  @Test
  void acceptsBroadcastFeedbackForCompatibility() {
    assertTrue(MavlinkFeedbackTargetFilter.allow(
        frame(47, Map.of("target_system", 0, "target_component", 0)),
        config()));
  }

  @Test
  void feedbackWithoutTargetExtensionIsAcceptedForMavlinkOneCompatibility() {
    assertTrue(MavlinkFeedbackTargetFilter.allow(frame(77, Map.of()), config()));
  }

  @Test
  void nonFeedbackTelemetryIsNotFilteredByTargetFields() {
    assertTrue(MavlinkFeedbackTargetFilter.allow(
        frame(42, Map.of("target_system", 250, "target_component", 194)),
        config()));
  }

  @Test
  void listenOnlyInterfaceDoesNotApplyGroundControlTargetFilter() {
    MavlinkConfigDTO config = new MavlinkConfigDTO();
    assertTrue(MavlinkFeedbackTargetFilter.allow(
        frame(40, Map.of("target_system", 250, "target_component", 194)),
        config));
  }

  private MavlinkConfigDTO config() {
    MavlinkConfigDTO config = new MavlinkConfigDTO();
    config.setSystemId(LOCAL_SYSTEM_ID);
    config.setComponentId(LOCAL_COMPONENT_ID);
    return config;
  }

  private ProcessedFrame frame(int messageId, Map<String, Object> fields) {
    ProcessedFrame processedFrame = mock(ProcessedFrame.class);
    Frame frame = mock(Frame.class);
    when(frame.getMessageId()).thenReturn(messageId);
    when(processedFrame.getFrame()).thenReturn(frame);
    when(processedFrame.getFields()).thenReturn(fields);
    return processedFrame;
  }
}
