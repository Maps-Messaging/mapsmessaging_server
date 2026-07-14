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

import io.mapsmessaging.state.mavlink.messages.MavlinkCommandLong;
import io.mapsmessaging.state.mavlink.messages.MavlinkMessage;
import io.mapsmessaging.state.mavlink.model.UxvModelCommandSet;
import io.mapsmessaging.state.mavlink.model.UxvOperation;
import io.mapsmessaging.state.mavlink.packet.CommandAckPacket;
import io.mapsmessaging.state.mavlink.packet.MavlinkPacket;
import io.mapsmessaging.state.mavlink.sender.MavlinkAcknowledgementHandler.Acknowledgement;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static io.mapsmessaging.state.mavlink.sender.MavlinkSendResult.Status.CANCELLED;
import static io.mapsmessaging.state.mavlink.sender.MavlinkSendResult.Status.CLOSED;
import static io.mapsmessaging.state.mavlink.sender.MavlinkSendResult.Status.FAILED;
import static io.mapsmessaging.state.mavlink.sender.MavlinkSendResult.Status.SUCCESS;
import static io.mapsmessaging.state.mavlink.sender.MavlinkSendResult.Status.TIMEOUT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class MavlinkEventListSenderTest {

  @Test
  void constructorRejectsNullCommandSet() {
    assertThrows(NullPointerException.class, () -> new MavlinkEventListSender(null, mock(MavlinkEventSender.class), mock(MavlinkAcknowledgementHandler.class), result -> {}));
  }

  @Test
  void constructorRejectsNullSender() {
    assertThrows(NullPointerException.class, () -> new MavlinkEventListSender(commandSet(List.of()), null, mock(MavlinkAcknowledgementHandler.class), result -> {}));
  }

  @Test
  void constructorRejectsNullAcknowledgementHandler() {
    assertThrows(NullPointerException.class, () -> new MavlinkEventListSender(commandSet(List.of()), mock(MavlinkEventSender.class), null, result -> {}));
  }

  @Test
  void constructorRejectsNullCompletionHandler() {
    assertThrows(NullPointerException.class, () -> new MavlinkEventListSender(commandSet(List.of()), mock(MavlinkEventSender.class), mock(MavlinkAcknowledgementHandler.class), null));
  }

  @Test
  void startWithEmptyMessageListCompletesSuccessfully() {
    Fixture fixture = fixture();

    fixture.newSender().start();

    assertEquals(1, fixture.results.size());
    assertEquals(SUCCESS, fixture.results.get(0).status());
    assertEquals(0, fixture.results.get(0).index());
    assertEquals(0, fixture.results.get(0).total());
    assertTrue(fixture.results.get(0).isSuccess());
    verifyNoInteractions(fixture.sender);
  }

  @Test
  void startSendsAllMessagesThatDoNotRequireAcknowledgementAndCompletesSuccessfully() throws Exception {
    Fixture fixture = fixture();
    MavlinkMessage first = fixture.message(false);
    MavlinkMessage second = fixture.message(false);
    MavlinkMessage third = fixture.message(false);
    fixture.messages.addAll(List.of(first, second, third));

    fixture.newSender().start();

    InOrder inOrder = inOrder(fixture.sender);
    inOrder.verify(fixture.sender).send(first);
    inOrder.verify(fixture.sender).send(second);
    inOrder.verify(fixture.sender).send(third);
    assertEquals(1, fixture.results.size());
    assertEquals(SUCCESS, fixture.results.get(0).status());
    assertEquals(3, fixture.results.get(0).index());
    assertEquals(3, fixture.results.get(0).total());
  }

  @Test
  void startSendsOnlyFirstAcknowledgedMessageAndWaits() throws Exception {
    Fixture fixture = fixture();
    MavlinkMessage first = fixture.message(true);
    MavlinkMessage second = fixture.message(false);
    fixture.messages.addAll(List.of(first, second));

    fixture.newSender().start();

    verify(fixture.sender).send(first);
    verify(fixture.sender, never()).send(second);
    assertTrue(fixture.results.isEmpty());
  }
  @Test
  void acceptedAcknowledgementAdvancesAndContinuesWalking() throws Exception {
    Fixture fixture = fixture();
    MavlinkMessage first = fixture.message(true);
    MavlinkMessage second = fixture.message(false);
    MavlinkPacket packet = mock(MavlinkPacket.class);
    fixture.messages.addAll(List.of(first, second));
    when(fixture.acknowledgementHandler.acknowledge(first, packet)).thenReturn(Acknowledgement.advance());

    MavlinkEventListSender sender = fixture.newSender();
    sender.start();
    sender.onMavlinkMessage(packet);

    InOrder inOrder = inOrder(fixture.sender);
    inOrder.verify(fixture.sender).send(first);
    inOrder.verify(fixture.sender).send(second);

    assertEquals(1, fixture.results.size());
    assertEquals(SUCCESS, fixture.results.get(0).status());
    assertEquals(2, fixture.results.get(0).index());
    assertEquals(2, fixture.results.get(0).total());
    assertNull(fixture.results.get(0).sentMessage());
    assertNull(fixture.results.get(0).receivedMessage());
  }

  @Test
  void notRelatedAcknowledgementDoesNotAdvanceOrComplete() throws Exception {
    Fixture fixture = fixture();
    MavlinkMessage first = fixture.message(true);
    MavlinkMessage second = fixture.message(false);
    MavlinkPacket packet = mock(MavlinkPacket.class);
    fixture.messages.addAll(List.of(first, second));
    when(fixture.acknowledgementHandler.acknowledge(first, packet)).thenReturn(Acknowledgement.notRelated());

    MavlinkEventListSender sender = fixture.newSender();
    sender.start();
    sender.onMavlinkMessage(packet);

    verify(fixture.sender).send(first);
    verify(fixture.sender, never()).send(second);
    assertTrue(fixture.results.isEmpty());
  }

  @Test
  void nullAcknowledgementIsTreatedAsNotRelated() throws Exception {
    Fixture fixture = fixture();
    MavlinkMessage first = fixture.message(true);
    MavlinkMessage second = fixture.message(false);
    MavlinkPacket packet = mock(MavlinkPacket.class);
    fixture.messages.addAll(List.of(first, second));
    when(fixture.acknowledgementHandler.acknowledge(first, packet)).thenReturn(null);

    MavlinkEventListSender sender = fixture.newSender();
    sender.start();
    sender.onMavlinkMessage(packet);

    verify(fixture.sender).send(first);
    verify(fixture.sender, never()).send(second);
    assertTrue(fixture.results.isEmpty());
  }

  @Test
  void waitAcknowledgementDoesNotAdvanceOrComplete() throws Exception {
    Fixture fixture = fixture();
    MavlinkMessage first = fixture.message(true);
    MavlinkMessage second = fixture.message(false);
    MavlinkPacket packet = mock(MavlinkPacket.class);
    fixture.messages.addAll(List.of(first, second));
    when(fixture.acknowledgementHandler.acknowledge(first, packet)).thenReturn(Acknowledgement.waitForMore());

    MavlinkEventListSender sender = fixture.newSender();
    sender.start();
    sender.onMavlinkMessage(packet);

    verify(fixture.sender).send(first);
    verify(fixture.sender, never()).send(second);
    assertTrue(fixture.results.isEmpty());
  }

  @Test
  void completeAcknowledgementCompletesSuccessfullyImmediately() throws Exception {
    Fixture fixture = fixture();
    MavlinkMessage first = fixture.message(true);
    MavlinkMessage second = fixture.message(false);
    MavlinkPacket packet = mock(MavlinkPacket.class);
    fixture.messages.addAll(List.of(first, second));
    when(fixture.acknowledgementHandler.acknowledge(first, packet)).thenReturn(Acknowledgement.complete());

    MavlinkEventListSender sender = fixture.newSender();
    sender.start();
    sender.onMavlinkMessage(packet);

    verify(fixture.sender).send(first);
    verify(fixture.sender, never()).send(second);
    assertEquals(1, fixture.results.size());
    assertEquals(SUCCESS, fixture.results.get(0).status());
    assertEquals(0, fixture.results.get(0).index());
    assertSame(first, fixture.results.get(0).sentMessage());
    assertSame(packet, fixture.results.get(0).receivedMessage());
  }

  @Test
  void failAcknowledgementCompletesFailedWithReason() throws Exception {
    Fixture fixture = fixture();
    MavlinkMessage first = fixture.message(true);
    MavlinkPacket packet = mock(MavlinkPacket.class);
    fixture.messages.add(first);
    when(fixture.acknowledgementHandler.acknowledge(first, packet)).thenReturn(Acknowledgement.fail("rejected"));

    MavlinkEventListSender sender = fixture.newSender();
    sender.start();
    sender.onMavlinkMessage(packet);

    assertEquals(1, fixture.results.size());
    assertEquals(FAILED, fixture.results.get(0).status());
    assertEquals("rejected", fixture.results.get(0).reason());
    assertSame(first, fixture.results.get(0).sentMessage());
    assertSame(packet, fixture.results.get(0).receivedMessage());
    assertFalse(fixture.results.get(0).isSuccess());
  }

  @Test
  void failAcknowledgementWithBlankReasonUsesDefaultReason() throws Exception {
    Fixture fixture = fixture();
    MavlinkMessage first = fixture.message(true);
    MavlinkPacket packet = mock(MavlinkPacket.class);
    fixture.messages.add(first);
    when(fixture.acknowledgementHandler.acknowledge(first, packet)).thenReturn(Acknowledgement.fail("  "));

    MavlinkEventListSender sender = fixture.newSender();
    sender.start();
    sender.onMavlinkMessage(packet);

    assertEquals(FAILED, fixture.results.get(0).status());
    assertEquals("MAVLink acknowledgement failed", fixture.results.get(0).reason());
  }

  @Test
  void sendIndexAcknowledgementSendsRequestedMessageAndContinuesAfterIt() throws Exception {
    Fixture fixture = fixture();
    MavlinkMessage first = fixture.message(true);
    MavlinkMessage second = fixture.message(false);
    MavlinkMessage third = fixture.message(false);
    MavlinkPacket packet = mock(MavlinkPacket.class);
    fixture.messages.addAll(List.of(first, second, third));
    when(fixture.acknowledgementHandler.acknowledge(first, packet)).thenReturn(Acknowledgement.sendIndex(2));

    MavlinkEventListSender sender = fixture.newSender();
    sender.start();
    sender.onMavlinkMessage(packet);

    InOrder inOrder = inOrder(fixture.sender);
    inOrder.verify(fixture.sender).send(first);
    inOrder.verify(fixture.sender).send(third);
    verify(fixture.sender, never()).send(second);
    assertEquals(1, fixture.results.size());
    assertEquals(SUCCESS, fixture.results.get(0).status());
    assertEquals(3, fixture.results.get(0).index());
  }

  @Test
  void sendIndexAcknowledgementFailsWhenRequestedIndexIsNegative() throws Exception {
    Fixture fixture = fixture();
    MavlinkMessage first = fixture.message(true);
    MavlinkPacket packet = mock(MavlinkPacket.class);
    fixture.messages.add(first);
    when(fixture.acknowledgementHandler.acknowledge(first, packet)).thenReturn(Acknowledgement.sendIndex(-1));

    MavlinkEventListSender sender = fixture.newSender();
    sender.start();
    sender.onMavlinkMessage(packet);

    assertEquals(FAILED, fixture.results.get(0).status());
    assertEquals(-1, fixture.results.get(0).index());
    assertEquals("Acknowledgement requested an invalid MAVLink message index", fixture.results.get(0).reason());
  }

  @Test
  void sendIndexAcknowledgementFailsWhenRequestedIndexIsPastEnd() throws Exception {
    Fixture fixture = fixture();
    MavlinkMessage first = fixture.message(true);
    MavlinkPacket packet = mock(MavlinkPacket.class);
    fixture.messages.add(first);
    when(fixture.acknowledgementHandler.acknowledge(first, packet)).thenReturn(Acknowledgement.sendIndex(1));

    MavlinkEventListSender sender = fixture.newSender();
    sender.start();
    sender.onMavlinkMessage(packet);

    assertEquals(FAILED, fixture.results.get(0).status());
    assertEquals(1, fixture.results.get(0).index());
    assertEquals("Acknowledgement requested an invalid MAVLink message index", fixture.results.get(0).reason());
  }

  @Test
  void sendFailureCompletesFailedWithCause() throws Exception {
    Fixture fixture = fixture();
    RuntimeException failure = new RuntimeException("send failed");
    MavlinkMessage first = fixture.message(false);
    fixture.messages.add(first);
    doThrow(failure).when(fixture.sender).send(first);

    fixture.newSender().start();

    assertEquals(1, fixture.results.size());
    assertEquals(FAILED, fixture.results.get(0).status());
    assertEquals(0, fixture.results.get(0).index());
    assertSame(first, fixture.results.get(0).sentMessage());
    assertSame(failure, fixture.results.get(0).cause());
    assertEquals("Failed to send MAVLink message", fixture.results.get(0).reason());
  }

  @Test
  void requestedSendFailureCompletesFailedWithCause() throws Exception {
    Fixture fixture = fixture();
    RuntimeException failure = new RuntimeException("requested send failed");
    MavlinkMessage first = fixture.message(true);
    MavlinkMessage second = fixture.message(false);
    MavlinkPacket packet = mock(MavlinkPacket.class);
    fixture.messages.addAll(List.of(first, second));
    when(fixture.acknowledgementHandler.acknowledge(first, packet)).thenReturn(Acknowledgement.sendIndex(1));
    doThrow(failure).when(fixture.sender).send(second);

    MavlinkEventListSender sender = fixture.newSender();
    sender.start();
    sender.onMavlinkMessage(packet);

    assertEquals(1, fixture.results.size());
    assertEquals(FAILED, fixture.results.get(0).status());
    assertEquals(1, fixture.results.get(0).index());
    assertSame(second, fixture.results.get(0).sentMessage());
    assertSame(failure, fixture.results.get(0).cause());
    assertEquals("Failed to send requested MAVLink message", fixture.results.get(0).reason());
  }

  @Test
  void inboundMessageIsIgnoredWhenSenderIsNotWaiting() throws Exception {
    Fixture fixture = fixture();
    MavlinkMessage first = fixture.message(false);
    MavlinkPacket packet = mock(MavlinkPacket.class);
    fixture.messages.add(first);

    MavlinkEventListSender sender = fixture.newSender();
    sender.start();
    sender.onMavlinkMessage(packet);

    verify(fixture.acknowledgementHandler, never()).acknowledge(first, packet);
    assertEquals(1, fixture.results.size());
    assertEquals(SUCCESS, fixture.results.get(0).status());
  }

  @Test
  void nullInboundMessageThrowsWhileActive() {
    Fixture fixture = fixture();
    MavlinkMessage first = fixture.message(true);
    fixture.messages.add(first);

    MavlinkEventListSender sender = fixture.newSender();
    sender.start();

    assertThrows(NullPointerException.class, () -> sender.onMavlinkMessage(null));
  }

  @Test
  void nullInboundMessageIsIgnoredAfterTerminalState() {
    Fixture fixture = fixture();
    MavlinkMessage first = fixture.message(false);
    fixture.messages.add(first);

    MavlinkEventListSender sender = fixture.newSender();
    sender.start();
    sender.onMavlinkMessage(null);

    assertEquals(1, fixture.results.size());
    assertEquals(SUCCESS, fixture.results.get(0).status());
  }

  @Test
  void startIsIgnoredWhenAlreadyStarted() throws Exception {
    Fixture fixture = fixture();
    MavlinkMessage first = fixture.message(true);
    fixture.messages.add(first);

    MavlinkEventListSender sender = fixture.newSender();
    sender.start();
    sender.start();

    verify(fixture.sender).send(first);
    verifyNoMoreInteractions(fixture.sender);
    assertTrue(fixture.results.isEmpty());
  }

  @Test
  void startIsIgnoredAfterTerminalState() throws Exception {
    Fixture fixture = fixture();
    MavlinkMessage first = fixture.message(true);
    fixture.messages.add(first);

    MavlinkEventListSender sender = fixture.newSender();
    sender.start();
    sender.cancel();
    sender.start();

    verify(fixture.sender).send(first);
    verifyNoMoreInteractions(fixture.sender);
    assertEquals(1, fixture.results.size());
    assertEquals(CANCELLED, fixture.results.get(0).status());
  }

  @Test
  void commandTimeoutRetransmitsThenAcceptedCommandAckCompletesExactlyOnce() throws Exception {
    MavlinkCommandLong command = mock(MavlinkCommandLong.class);
    when(command.getCommand()).thenReturn(192);
    when(command.getTargetSystem()).thenReturn(2);
    when(command.getTargetComponent()).thenReturn(1);

    MavlinkEventSender transport = mock(MavlinkEventSender.class);
    List<MavlinkSendResult> results = new ArrayList<>();
    MavlinkEventListSender sender =
        new MavlinkEventListSender(commandSet(List.of(command)), transport, new MavlinkCommandAcknowledgementHandler(), results::add, 2);

    sender.start();
    sender.timeout();

    verify(transport, times(2)).send(command);
    assertTrue(results.isEmpty());
    assertSame(command, sender.getWaitingMessage());
    assertEquals(1, sender.getRetryCount());

    CommandAckPacket acknowledgement = mock(CommandAckPacket.class);
    when(acknowledgement.isValid()).thenReturn(true);
    when(acknowledgement.getCommand()).thenReturn(192);
    when(acknowledgement.isAccepted()).thenReturn(true);
    when(acknowledgement.isTargetComponentPresent()).thenReturn(false);

    sender.onMavlinkMessage(acknowledgement);

    assertEquals(1, results.size());
    assertEquals(SUCCESS, results.get(0).status());
    assertNull(results.get(0).sentMessage());
    assertNull(results.get(0).receivedMessage());

    sender.timeout();
    sender.cancel();
    sender.close();

    verify(transport, times(2)).send(command);
    assertEquals(1, results.size());
  }

  @Test
  void repeatedTimeoutCallbacksExhaustRetryBudgetThenRemainTerminal() throws Exception {
    Fixture fixture = fixture();
    MavlinkMessage first = fixture.message(true);
    fixture.messages.add(first);

    MavlinkEventListSender sender = fixture.newSender(2);
    sender.start();

    sender.timeout();
    sender.timeout();

    verify(fixture.sender, times(3)).send(first);
    assertTrue(fixture.results.isEmpty());

    sender.timeout();

    assertEquals(1, fixture.results.size());
    assertEquals(TIMEOUT, fixture.results.get(0).status());
    assertSame(first, fixture.results.get(0).sentMessage());
    assertEquals(0, sender.getRetryCount());
    assertNull(sender.getWaitingMessage());
    assertEquals(-1, sender.getWaitingIndex());

    sender.timeout();
    sender.timeout();

    verify(fixture.sender, times(3)).send(first);
    assertEquals(1, fixture.results.size());
  }

  @Test
  void timeoutRetriesWaitingMessageUntilRetryBudgetIsExhausted() throws Exception {
    Fixture fixture = fixture();
    MavlinkMessage first = fixture.message(true);
    fixture.messages.add(first);

    MavlinkEventListSender sender = fixture.newSender(2);
    sender.start();
    sender.timeout();
    sender.timeout();

    verify(fixture.sender, times(3)).send(first);
    assertTrue(fixture.results.isEmpty());
    assertEquals(2, sender.getRetryCount());

    sender.timeout();

    assertEquals(1, fixture.results.size());
    assertEquals(TIMEOUT, fixture.results.get(0).status());
    assertEquals(0, fixture.results.get(0).index());
    assertSame(first, fixture.results.get(0).sentMessage());
    assertEquals("MAVLink event list sender timed out", fixture.results.get(0).reason());
  }

  @Test
  void timeoutCompletesImmediatelyWhenRetriesAreDisabled() {
    Fixture fixture = fixture();
    MavlinkMessage first = fixture.message(true);
    fixture.messages.add(first);

    MavlinkEventListSender sender = fixture.newSender(0);
    sender.start();
    sender.timeout();

    assertEquals(1, fixture.results.size());
    assertEquals(TIMEOUT, fixture.results.get(0).status());
    assertEquals(0, fixture.results.get(0).index());
    assertSame(first, fixture.results.get(0).sentMessage());
  }

  @Test
  void timeoutRetryFailureCompletesAsFailed() throws Exception {
    Fixture fixture = fixture();
    MavlinkMessage first = fixture.message(true);
    RuntimeException failure = new RuntimeException("retry failed");
    fixture.messages.add(first);

    doAnswer(
        invocation -> {
          if (fixture.senderSendCount++ > 0) {
            throw failure;
          }
          return null;
        }).when(fixture.sender)
        .send(first);

    MavlinkEventListSender sender = fixture.newSender(2);
    sender.start();
    sender.timeout();

    assertEquals(1, fixture.results.size());
    assertEquals(FAILED, fixture.results.get(0).status());
    assertSame(first, fixture.results.get(0).sentMessage());
    assertSame(failure, fixture.results.get(0).cause());
    assertEquals("Failed to resend MAVLink message after timeout", fixture.results.get(0).reason());
  }

  @Test
  void acknowledgementArrivingDuringSendIsProcessed() throws Exception {
    Fixture fixture = fixture();
    MavlinkMessage first = fixture.message(true);
    MavlinkMessage second = fixture.message(false);
    MavlinkPacket acknowledgementPacket = mock(MavlinkPacket.class);
    AtomicReference<MavlinkEventListSender> senderReference = new AtomicReference<>();

    fixture.messages.addAll(List.of(first, second));
    when(fixture.acknowledgementHandler.acknowledge(first, acknowledgementPacket)).thenReturn(Acknowledgement.advance());

    doAnswer(
        invocation -> {
          senderReference.get().onMavlinkMessage(acknowledgementPacket);
          return null;
        }).when(fixture.sender)
        .send(first);

    MavlinkEventListSender sender = fixture.newSender();
    senderReference.set(sender);
    sender.start();

    InOrder inOrder = inOrder(fixture.sender);
    inOrder.verify(fixture.sender).send(first);
    inOrder.verify(fixture.sender).send(second);

    assertEquals(1, fixture.results.size());
    assertEquals(SUCCESS, fixture.results.get(0).status());
  }

  @Test
  void acknowledgementProgressResetsRetryBudget() throws Exception {
    Fixture fixture = fixture();
    MavlinkMessage first = fixture.message(true);
    MavlinkMessage second = fixture.message(true);
    MavlinkPacket firstAck = mock(MavlinkPacket.class);

    fixture.messages.addAll(List.of(first, second));
    when(fixture.acknowledgementHandler.acknowledge(first, firstAck)).thenReturn(Acknowledgement.advance());

    MavlinkEventListSender sender = fixture.newSender(1);
    sender.start();
    sender.timeout();

    assertEquals(1, sender.getRetryCount());

    sender.onMavlinkMessage(firstAck);

    assertSame(second, sender.getWaitingMessage());
    assertEquals(0, sender.getRetryCount());

    sender.timeout();

    verify(fixture.sender, times(2)).send(first);
    verify(fixture.sender, times(2)).send(second);
    assertTrue(fixture.results.isEmpty());
  }

  @Test
  void cancelCompletesUsingCurrentState() {
    Fixture fixture = fixture();
    MavlinkMessage first = fixture.message(true);
    fixture.messages.add(first);

    MavlinkEventListSender sender = fixture.newSender();
    sender.start();
    sender.cancel();

    assertEquals(1, fixture.results.size());
    assertEquals(CANCELLED, fixture.results.get(0).status());
    assertEquals(0, fixture.results.get(0).index());
    assertSame(first, fixture.results.get(0).sentMessage());
    assertEquals("MAVLink event list sender cancelled", fixture.results.get(0).reason());
  }

  @Test
  void closeCompletesUsingCurrentState() {
    Fixture fixture = fixture();
    MavlinkMessage first = fixture.message(true);
    fixture.messages.add(first);

    MavlinkEventListSender sender = fixture.newSender();
    sender.start();
    sender.close();

    assertEquals(1, fixture.results.size());
    assertEquals(CLOSED, fixture.results.get(0).status());
    assertEquals(0, fixture.results.get(0).index());
    assertSame(first, fixture.results.get(0).sentMessage());
    assertEquals("MAVLink event list sender closed", fixture.results.get(0).reason());
  }

  @Test
  void onlyFirstTerminalTransitionNotifiesCompletionHandler() {
    Fixture fixture = fixture();
    MavlinkMessage first = fixture.message(true);
    fixture.messages.add(first);

    MavlinkEventListSender sender = fixture.newSender(0);
    sender.start();
    sender.timeout();
    sender.cancel();
    sender.close();

    assertEquals(1, fixture.results.size());
    assertEquals(TIMEOUT, fixture.results.get(0).status());
  }

  @Test
  void completionHandlerFailureIsSwallowed() {
    Fixture fixture = fixture();
    MavlinkMessage first = fixture.message(false);
    fixture.messages.add(first);
    MavlinkSendCompletionHandler completionHandler = result -> {
      throw new RuntimeException("completion failed");
    };

    MavlinkEventListSender sender = new MavlinkEventListSender(commandSet(fixture.messages), fixture.sender, fixture.acknowledgementHandler, completionHandler);

    sender.start();
  }

  @Test
  void terminalSenderDoesNotProcessFurtherInboundPackets() throws Exception {
    Fixture fixture = fixture();
    MavlinkMessage first = fixture.message(true);
    MavlinkPacket packet = mock(MavlinkPacket.class);
    fixture.messages.add(first);

    MavlinkEventListSender sender = fixture.newSender();
    sender.start();
    sender.cancel();
    sender.onMavlinkMessage(packet);

    verify(fixture.acknowledgementHandler, never()).acknowledge(first, packet);
    assertEquals(1, fixture.results.size());
    assertEquals(CANCELLED, fixture.results.get(0).status());
  }

  @Test
  void acknowledgementActionNullNormalisesToNotRelated() {
    Acknowledgement acknowledgement = new Acknowledgement(null, 0, "ignored");

    assertEquals(MavlinkAcknowledgementHandler.Action.NOT_RELATED, acknowledgement.action());
  }

  private Fixture fixture() {
    return new Fixture();
  }

  private static UxvModelCommandSet commandSet(List<MavlinkMessage> messages) {
    UxvModelCommandSet commandSet = mock(UxvModelCommandSet.class);
    when(commandSet.operation()).thenReturn(UxvOperation.ORBIT);
    when(commandSet.modelName()).thenReturn("model");
    when(commandSet.messages()).thenReturn(messages);
    return commandSet;
  }

  private static class Fixture {

    private final List<MavlinkMessage> messages = new ArrayList<>();
    private final List<MavlinkSendResult> results = new ArrayList<>();
    private final MavlinkEventSender sender = mock(MavlinkEventSender.class);
    private final MavlinkAcknowledgementHandler acknowledgementHandler = mock(MavlinkAcknowledgementHandler.class);
    private int senderSendCount;

    private MavlinkEventListSender newSender() {
      return new MavlinkEventListSender(commandSet(messages), sender, acknowledgementHandler, results::add);
    }

    private MavlinkEventListSender newSender(int maxRetries) {
      return new MavlinkEventListSender(commandSet(messages), sender, acknowledgementHandler, results::add, maxRetries);
    }

    private MavlinkMessage message(boolean requiresAcknowledgement) {
      MavlinkMessage message = mock(MavlinkMessage.class);
      when(acknowledgementHandler.requiresAcknowledgement(message)).thenReturn(requiresAcknowledgement);
      return message;
    }
  }
}