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

package io.mapsmessaging.engine.destination.subscription.impl;

import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.engine.destination.subscription.state.MessageStateManager;
import io.mapsmessaging.engine.destination.subscription.transaction.AcknowledgementController;
import io.mapsmessaging.engine.session.SessionImpl;
import io.mapsmessaging.utilities.admin.JMXManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DestinationSubscriptionTest {

  private boolean jmxEnabled;

  @BeforeEach
  void disableJmx() {
    jmxEnabled = JMXManager.isEnableJMX();
    JMXManager.setEnableJMX(false);
  }

  @AfterEach
  void restoreJmx() {
    JMXManager.setEnableJMX(jmxEnabled);
  }

  @Test
  void transient_subscription_is_not_registered_with_destination() {
    DestinationImpl destination = mock(DestinationImpl.class);
    SubscriptionContext context = mock(SubscriptionContext.class);
    SessionImpl session = mock(SessionImpl.class);
    AcknowledgementController acknowledgementController = mock(AcknowledgementController.class);
    MessageStateManager stateManager = mock(MessageStateManager.class);
    when(destination.getTypePath()).thenReturn(List.of("destination=orders"));

    new DestinationSubscription(destination, context, session, "browser", acknowledgementController, stateManager, false);

    verify(destination, never()).addSubscription(org.mockito.ArgumentMatchers.any());
  }
}
