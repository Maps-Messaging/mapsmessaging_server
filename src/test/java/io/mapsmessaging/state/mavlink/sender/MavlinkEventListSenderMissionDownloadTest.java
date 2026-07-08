/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with
 *  the License. You may obtain a copy of the License at:
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

package io.mapsmessaging.state.mavlink.sender;

import io.mapsmessaging.state.mavlink.messages.MavlinkMessage;
import io.mapsmessaging.state.mavlink.model.UxvModelCommandSet;
import io.mapsmessaging.state.mavlink.model.UxvOperation;
import io.mapsmessaging.state.mavlink.packet.MavlinkPacket;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import static io.mapsmessaging.state.mavlink.sender.MavlinkSendResult.Status.FAILED;
import static io.mapsmessaging.state.mavlink.sender.MavlinkSendResult.Status.SUCCESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MavlinkEventListSenderMissionDownloadTest {

  @Test
  void missionDownloadStyleRequestSequenceSendsRequestedItemsAndCompletes() throws Exception {
    Fixture fixture = fixture(3);

    MavlinkEventListSender sender = fixture.newSender();
    sender.start();
    sender.onMavlinkMessage(fixture.requestItemPacket(1));
    sender.onMavlinkMessage(fixture.requestItemPacket(2));
    sender.onMavlinkMessage(fixture.requestItemPacket(3));
    sender.onMavlinkMessage(fixture.completionPacket);

    InOrder inOrder = inOrder(fixture.sender);
    inOrder.verify(fixture.sender).send(fixture.missionCountMessage);
    inOrder.verify(fixture.sender).send(fixture.itemMessages.get(0));
    inOrder.verify(fixture.sender).send(fixture.itemMessages.get(1));
    inOrder.verify(fixture.sender).send(fixture.itemMessages.get(2));

    assertEquals(1, fixture.results.size());
    assertEquals(SUCCESS, fixture.results.get(0).status());
    assertEquals(3, fixture.results.get(0).index());
    assertEquals(4, fixture.results.get(0).total());
    assertSame(fixture.itemMessages.get(2), fixture.results.get(0).sentMessage());
    assertSame(fixture.completionPacket, fixture.results.get(0).receivedMessage());
    assertTrue(fixture.results.get(0).isSuccess());
  }

  @Test
  void missionDownloadFailsWhenFirstRequestSkipsAnItem() throws Exception {
    Fixture fixture = fixture(3);

    MavlinkEventListSender sender = fixture.newSender();
    sender.start();
    sender.onMavlinkMessage(fixture.requestItemPacket(2));

    verify(fixture.sender).send(fixture.missionCountMessage);
    verify(fixture.sender, never()).send(fixture.itemMessages.get(0));
    verify(fixture.sender, never()).send(fixture.itemMessages.get(1));
    verify(fixture.sender, never()).send(fixture.itemMessages.get(2));

    assertEquals(1, fixture.results.size());
    assertEquals(FAILED, fixture.results.get(0).status());
    assertEquals(0, fixture.results.get(0).index());
    assertSame(fixture.missionCountMessage, fixture.results.get(0).sentMessage());
    assertEquals("Mission requested MAVLink message index 2 but expected 1", fixture.results.get(0).reason());
  }

  @Test
  void missionDownloadFailsWhenRequestMovesBackwards() throws Exception {
    Fixture fixture = fixture(3);

    MavlinkEventListSender sender = fixture.newSender();
    sender.start();
    sender.onMavlinkMessage(fixture.requestItemPacket(1));
    sender.onMavlinkMessage(fixture.requestItemPacket(2));
    sender.onMavlinkMessage(fixture.requestItemPacket(1));

    InOrder inOrder = inOrder(fixture.sender);
    inOrder.verify(fixture.sender).send(fixture.missionCountMessage);
    inOrder.verify(fixture.sender).send(fixture.itemMessages.get(0));
    inOrder.verify(fixture.sender).send(fixture.itemMessages.get(1));
    verify(fixture.sender, never()).send(fixture.itemMessages.get(2));

    assertEquals(1, fixture.results.size());
    assertEquals(FAILED, fixture.results.get(0).status());
    assertEquals(2, fixture.results.get(0).index());
    assertSame(fixture.itemMessages.get(1), fixture.results.get(0).sentMessage());
    assertEquals("Mission requested MAVLink message index 1 but expected 3", fixture.results.get(0).reason());
  }

  @Test
  void missionDownloadFailsWhenRequestRepeatsTheCurrentItem() throws Exception {
    Fixture fixture = fixture(3);

    MavlinkEventListSender sender = fixture.newSender();
    sender.start();
    sender.onMavlinkMessage(fixture.requestItemPacket(1));
    sender.onMavlinkMessage(fixture.requestItemPacket(1));

    InOrder inOrder = inOrder(fixture.sender);
    inOrder.verify(fixture.sender).send(fixture.missionCountMessage);
    inOrder.verify(fixture.sender).send(fixture.itemMessages.get(0));
    verify(fixture.sender, never()).send(fixture.itemMessages.get(1));
    verify(fixture.sender, never()).send(fixture.itemMessages.get(2));

    assertEquals(1, fixture.results.size());
    assertEquals(FAILED, fixture.results.get(0).status());
    assertEquals(1, fixture.results.get(0).index());
    assertSame(fixture.itemMessages.get(0), fixture.results.get(0).sentMessage());
    assertEquals("Mission requested MAVLink message index 1 but expected 2", fixture.results.get(0).reason());
  }

  @Test
  void missionDownloadFailsWhenCompletionArrivesBeforeAllItemsWereRequested() throws Exception {
    Fixture fixture = fixture(3);

    MavlinkEventListSender sender = fixture.newSender();
    sender.start();
    sender.onMavlinkMessage(fixture.requestItemPacket(1));
    sender.onMavlinkMessage(fixture.completionPacket);

    InOrder inOrder = inOrder(fixture.sender);
    inOrder.verify(fixture.sender).send(fixture.missionCountMessage);
    inOrder.verify(fixture.sender).send(fixture.itemMessages.get(0));
    verify(fixture.sender, never()).send(fixture.itemMessages.get(1));
    verify(fixture.sender, never()).send(fixture.itemMessages.get(2));

    assertEquals(1, fixture.results.size());
    assertEquals(FAILED, fixture.results.get(0).status());
    assertEquals(1, fixture.results.get(0).index());
    assertSame(fixture.itemMessages.get(0), fixture.results.get(0).sentMessage());
    assertEquals("Mission completed before all requested items were sent", fixture.results.get(0).reason());
  }

  @Test
  void missionDownloadIgnoresUnrelatedPacketsWhileWaitingForNextRequest() throws Exception {
    Fixture fixture = fixture(2);
    MavlinkPacket unrelatedPacket = mock(MavlinkPacket.class);

    MavlinkEventListSender sender = fixture.newSender();
    sender.start();
    sender.onMavlinkMessage(unrelatedPacket);
    sender.onMavlinkMessage(fixture.requestItemPacket(1));
    sender.onMavlinkMessage(fixture.requestItemPacket(2));
    sender.onMavlinkMessage(fixture.completionPacket);

    InOrder inOrder = inOrder(fixture.sender);
    inOrder.verify(fixture.sender).send(fixture.missionCountMessage);
    inOrder.verify(fixture.sender).send(fixture.itemMessages.get(0));
    inOrder.verify(fixture.sender).send(fixture.itemMessages.get(1));

    assertEquals(1, fixture.results.size());
    assertEquals(SUCCESS, fixture.results.get(0).status());
  }

  private Fixture fixture(int itemCount) {
    return new Fixture(itemCount);
  }

  private static UxvModelCommandSet commandSet(List<MavlinkMessage> messages) {
    UxvModelCommandSet commandSet = mock(UxvModelCommandSet.class);
    when(commandSet.operation()).thenReturn(UxvOperation.BUILD_MISSION);
    when(commandSet.modelName()).thenReturn("mission");
    when(commandSet.messages()).thenReturn(messages);
    return commandSet;
  }

  private static class Fixture {

    private final MavlinkMessage missionCountMessage;
    private final List<MavlinkMessage> itemMessages;
    private final List<MavlinkMessage> messages;
    private final List<MavlinkSendResult> results;
    private final MavlinkEventSender sender;
    private final MavlinkPacket completionPacket;
    private final MissionDownloadAcknowledgementHandler acknowledgementHandler;

    private Fixture(int itemCount) {
      this.missionCountMessage = mock(MavlinkMessage.class);
      this.itemMessages = new ArrayList<>();
      this.messages = new ArrayList<>();
      this.results = new ArrayList<>();
      this.sender = mock(MavlinkEventSender.class);
      this.completionPacket = mock(MavlinkPacket.class);

      messages.add(missionCountMessage);
      for (int i = 0; i < itemCount; i++) {
        MavlinkMessage itemMessage = mock(MavlinkMessage.class);
        itemMessages.add(itemMessage);
        messages.add(itemMessage);
      }

      this.acknowledgementHandler = new MissionDownloadAcknowledgementHandler(messages, completionPacket);
    }

    private MavlinkEventListSender newSender() {
      return new MavlinkEventListSender(commandSet(messages), sender, acknowledgementHandler, results::add);
    }

    private MavlinkPacket requestItemPacket(int messageIndex) {
      MavlinkPacket packet = mock(MavlinkPacket.class);
      acknowledgementHandler.addRequest(packet, messageIndex);
      return packet;
    }
  }

  private static class MissionDownloadAcknowledgementHandler implements MavlinkAcknowledgementHandler {

    private final List<MavlinkMessage> messages;
    private final MavlinkPacket completionPacket;
    private final Map<MavlinkPacket, Integer> requestedMessageIndexes;
    private int expectedNextMessageIndex;

    private MissionDownloadAcknowledgementHandler(List<MavlinkMessage> messages, MavlinkPacket completionPacket) {
      this.messages = messages;
      this.completionPacket = completionPacket;
      this.requestedMessageIndexes = new IdentityHashMap<>();
      this.expectedNextMessageIndex = 1;
    }

    @Override
    public boolean requiresAcknowledgement(MavlinkMessage sentMessage) {
      return true;
    }

    @Override
    public Acknowledgement acknowledge(MavlinkMessage sentMessage, MavlinkPacket receivedMessage) {
      if (receivedMessage == completionPacket) {
        return acknowledgeCompletion(sentMessage);
      }

      Integer requestedMessageIndex = requestedMessageIndexes.get(receivedMessage);
      if (requestedMessageIndex == null) {
        return Acknowledgement.notRelated();
      }

      if (requestedMessageIndex != expectedNextMessageIndex) {
        return Acknowledgement.fail("Mission requested MAVLink message index " + requestedMessageIndex + " but expected " + expectedNextMessageIndex);
      }

      expectedNextMessageIndex++;
      return Acknowledgement.sendIndex(requestedMessageIndex);
    }

    private Acknowledgement acknowledgeCompletion(MavlinkMessage sentMessage) {
      if (expectedNextMessageIndex != messages.size() || sentMessage != messages.get(messages.size() - 1)) {
        return Acknowledgement.fail("Mission completed before all requested items were sent");
      }

      return Acknowledgement.complete();
    }

    private void addRequest(MavlinkPacket packet, int requestedMessageIndex) {
      requestedMessageIndexes.put(packet, requestedMessageIndex);
    }
  }
}
