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

package io.mapsmessaging.engine.destination.subscription.transaction;

import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.engine.destination.subscription.OutstandingEventDetails;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

class AutoAcknowledgementControllerTest {

  @Test
  void type_isAuto() {
    CreditManager creditManager = new FixedCreditManager(10);
    AutoAcknowledgementController controller = new AutoAcknowledgementController(creditManager);

    Assertions.assertEquals("auto", controller.getType());
  }

  @Test
  void sent_addsOutstanding_andDoesNotChangeFixedCredit() {
    CreditManager creditManager = new FixedCreditManager(5);
    AutoAcknowledgementController controller = new AutoAcknowledgementController(creditManager);

    Message message = createMessage(123L, 7);
    controller.sent(message);

    Assertions.assertEquals(1, controller.size());
    Assertions.assertEquals(5, creditManager.getCurrentCredit());

    List<OutstandingEventDetails> outstanding = controller.getOutstanding();
    Assertions.assertEquals(1, outstanding.size());
    Assertions.assertEquals(123L, outstanding.get(0).getId());
    Assertions.assertEquals(7, outstanding.get(0).getPriority());
  }

  @Test
  void ack_removesFirstOutstanding_ignoresMessageId() {
    CreditManager creditManager = new FixedCreditManager(10);
    AutoAcknowledgementController controller = new AutoAcknowledgementController(creditManager);

    controller.sent(createMessage(1L, 1));
    controller.sent(createMessage(2L, 1));
    controller.sent(createMessage(3L, 1));

    Assertions.assertEquals(3, controller.size());

    controller.ack(9999L);

    Assertions.assertEquals(2, controller.size());
    Assertions.assertEquals(2L, controller.getOutstanding().get(0).getId());
  }

  @Test
  void rollback_delegatesToAck() {
    CreditManager creditManager = new FixedCreditManager(10);
    AutoAcknowledgementController controller = new AutoAcknowledgementController(creditManager);

    controller.sent(createMessage(10L, 1));
    controller.sent(createMessage(11L, 1));

    controller.rollback(10L);

    Assertions.assertEquals(1, controller.size());
    Assertions.assertEquals(11L, controller.getOutstanding().get(0).getId());
  }

  @Test
  void messageSent_removesFirstOutstanding_returnsId_andDoesNotChangeFixedCredit() {
    CreditManager creditManager = new FixedCreditManager(2);
    AutoAcknowledgementController controller = new AutoAcknowledgementController(creditManager);

    controller.sent(createMessage(100L, 1));
    controller.sent(createMessage(200L, 1));

    Assertions.assertEquals(2, creditManager.getCurrentCredit());

    long first = controller.messageSent();
    Assertions.assertEquals(100L, first);
    Assertions.assertEquals(2, creditManager.getCurrentCredit());
    Assertions.assertEquals(1, controller.size());
    Assertions.assertEquals(200L, controller.getOutstanding().get(0).getId());

    long second = controller.messageSent();
    Assertions.assertEquals(200L, second);
    Assertions.assertEquals(2, creditManager.getCurrentCredit());
    Assertions.assertEquals(0, controller.size());
  }

  @Test
  void messageSent_whenEmpty_returnsMinusOne_andDoesNotChangeCredit() {
    CreditManager creditManager = new FixedCreditManager(3);
    AutoAcknowledgementController controller = new AutoAcknowledgementController(creditManager);

    long result = controller.messageSent();

    Assertions.assertEquals(-1L, result);
    Assertions.assertEquals(3, creditManager.getCurrentCredit());
  }

  @Test
  void canSend_isBasedOnOutstandingSizeComparedToCurrentCredit_fixedCredit() {
    CreditManager creditManager = new FixedCreditManager(3);
    AutoAcknowledgementController controller = new AutoAcknowledgementController(creditManager);

    Assertions.assertTrue(controller.canSend());

    controller.sent(createMessage(1L, 1));
    Assertions.assertTrue(controller.canSend());

    controller.sent(createMessage(2L, 1));
    Assertions.assertTrue(controller.canSend());

    controller.sent(createMessage(3L, 1));
    Assertions.assertFalse(controller.canSend());
  }

  @Test
  void setMaxOutstanding_updatesCredit_andReturnsCanSend() {
    CreditManager creditManager = new FixedCreditManager(1);
    AutoAcknowledgementController controller = new AutoAcknowledgementController(creditManager);

    controller.sent(createMessage(1L, 1));

    boolean canSendAfterSet = controller.setMaxOutstanding(10);

    Assertions.assertEquals(10, creditManager.getCurrentCredit());
    Assertions.assertTrue(canSendAfterSet);
  }

  @Test
  void close_clearsOutstanding() {
    CreditManager creditManager = new FixedCreditManager(10);
    AutoAcknowledgementController controller = new AutoAcknowledgementController(creditManager);

    controller.sent(createMessage(1L, 1));
    controller.sent(createMessage(2L, 1));

    Assertions.assertEquals(2, controller.size());

    controller.close();

    Assertions.assertEquals(0, controller.size());
    Assertions.assertTrue(controller.getOutstanding().isEmpty());
  }

  private Message createMessage(long id, int priorityValue) {
    Message message = Mockito.mock(Message.class, Mockito.RETURNS_DEEP_STUBS);
    Mockito.when(message.getIdentifier()).thenReturn(id);
    Mockito.when(message.getPriority().getValue()).thenReturn(priorityValue);
    return message;
  }
}
