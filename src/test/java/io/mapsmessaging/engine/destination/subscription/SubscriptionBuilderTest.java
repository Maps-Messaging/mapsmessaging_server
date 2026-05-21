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

package io.mapsmessaging.engine.destination.subscription;

import io.mapsmessaging.api.features.ClientAcknowledgement;
import io.mapsmessaging.api.features.CreditHandler;
import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.destination.subscription.transaction.AcknowledgementController;
import io.mapsmessaging.engine.destination.subscription.transaction.AutoAcknowledgementController;
import io.mapsmessaging.engine.destination.subscription.transaction.ClientAcknowledgementController;
import io.mapsmessaging.engine.destination.subscription.transaction.ClientCreditManager;
import io.mapsmessaging.engine.destination.subscription.transaction.CreditManager;
import io.mapsmessaging.engine.destination.subscription.transaction.FixedCreditManager;
import io.mapsmessaging.engine.destination.subscription.transaction.IndividualAcknowledgementController;
import io.mapsmessaging.engine.session.SessionImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;

class SubscriptionBuilderTest {

  @Test
  void compileParser_nullOrEmptySelector_resultsInNullParserExecutor() throws IOException {
    DestinationImpl destination = Mockito.mock(DestinationImpl.class);

    SubscriptionContext nullSelectorContext = Mockito.mock(SubscriptionContext.class);
    Mockito.when(nullSelectorContext.getSelector()).thenReturn(null);

    SubscriptionContext emptySelectorContext = Mockito.mock(SubscriptionContext.class);
    Mockito.when(emptySelectorContext.getSelector()).thenReturn("");

    TestSubscriptionBuilder builderNull = new TestSubscriptionBuilder(destination, nullSelectorContext);
    Assertions.assertNull(builderNull.getParserExecutor());

    TestSubscriptionBuilder builderEmpty = new TestSubscriptionBuilder(destination, emptySelectorContext);
    Assertions.assertNull(builderEmpty.getParserExecutor());
  }

  @Test
  void compileParser_invalidSelector_throwsIOException() {
    DestinationImpl destination = Mockito.mock(DestinationImpl.class);

    SubscriptionContext badSelectorContext = Mockito.mock(SubscriptionContext.class);
    Mockito.when(badSelectorContext.getSelector()).thenReturn("and and and"); // deliberately garbage

    IOException exception = Assertions.assertThrows(IOException.class, () -> new TestSubscriptionBuilder(destination, badSelectorContext));
    Assertions.assertNotNull(exception.getMessage());
  }

  @Test
  void combineSelectors_bothNullOrEmpty_returnsEmptyString() throws IOException {
    DestinationImpl destination = Mockito.mock(DestinationImpl.class);

    SubscriptionContext child = Mockito.mock(SubscriptionContext.class);
    Mockito.when(child.getSelector()).thenReturn(null);

    SubscriptionContext parent = Mockito.mock(SubscriptionContext.class);
    Mockito.when(parent.getSelector()).thenReturn("");

    TestSubscriptionBuilder builder = new TestSubscriptionBuilder(destination, child, parent);

    String combined = builder.combineSelectorsForTest(null, "");
    Assertions.assertEquals("", combined);
  }

  @Test
  void combineSelectors_onlyLeft_present_returnsLeft() throws IOException {
    DestinationImpl destination = Mockito.mock(DestinationImpl.class);
    SubscriptionContext child = Mockito.mock(SubscriptionContext.class);
    Mockito.when(child.getSelector()).thenReturn("a = 1");

    SubscriptionContext parent = Mockito.mock(SubscriptionContext.class);
    Mockito.when(parent.getSelector()).thenReturn("");

    TestSubscriptionBuilder builder = new TestSubscriptionBuilder(destination, child, parent);

    String combined = builder.combineSelectorsForTest("a = 1", "");
    Assertions.assertEquals("a = 1", combined.trim());
  }

  @Test
  void combineSelectors_onlyRight_present_returnsRight() throws IOException {
    DestinationImpl destination = Mockito.mock(DestinationImpl.class);
    SubscriptionContext child = Mockito.mock(SubscriptionContext.class);
    Mockito.when(child.getSelector()).thenReturn("");

    SubscriptionContext parent = Mockito.mock(SubscriptionContext.class);
    Mockito.when(parent.getSelector()).thenReturn("b = 2");

    TestSubscriptionBuilder builder = new TestSubscriptionBuilder(destination, child, parent);

    String combined = builder.combineSelectorsForTest("", "b = 2");
    Assertions.assertEquals("b = 2", combined);
  }

  @Test
  void combineSelectors_bothPresent_joinsWithAnd() throws IOException {
    DestinationImpl destination = Mockito.mock(DestinationImpl.class);
    SubscriptionContext child = Mockito.mock(SubscriptionContext.class);
    Mockito.when(child.getSelector()).thenReturn("a = 1");

    SubscriptionContext parent = Mockito.mock(SubscriptionContext.class);
    Mockito.when(parent.getSelector()).thenReturn("b = 2");

    TestSubscriptionBuilder builder = new TestSubscriptionBuilder(destination, child, parent);

    String combined = builder.combineSelectorsForTest("a = 1", "b = 2");
    Assertions.assertEquals("a = 1 and b = 2", combined);
  }

  @Test
  void createCreditManager_client_returnsClientCreditManager() throws IOException {
    DestinationImpl destination = Mockito.mock(DestinationImpl.class);

    SubscriptionContext context = Mockito.mock(SubscriptionContext.class);
    Mockito.when(context.getSelector()).thenReturn(null);
    Mockito.when(context.getReceiveMaximum()).thenReturn(123);
    Mockito.when(context.getCreditHandler()).thenReturn(CreditHandler.CLIENT);

    TestSubscriptionBuilder builder = new TestSubscriptionBuilder(destination, context);

    CreditManager creditManager = builder.createCreditManagerForTest(context);
    Assertions.assertInstanceOf(ClientCreditManager.class, creditManager);
  }

  @Test
  void createCreditManager_auto_returnsFixedCreditManager() throws IOException {
    DestinationImpl destination = Mockito.mock(DestinationImpl.class);

    SubscriptionContext context = Mockito.mock(SubscriptionContext.class);
    Mockito.when(context.getSelector()).thenReturn(null);
    Mockito.when(context.getReceiveMaximum()).thenReturn(456);
    Mockito.when(context.getCreditHandler()).thenReturn(CreditHandler.AUTO);

    TestSubscriptionBuilder builder = new TestSubscriptionBuilder(destination, context);

    CreditManager creditManager = builder.createCreditManagerForTest(context);
    Assertions.assertInstanceOf(FixedCreditManager.class, creditManager);
  }

  @Test
  void createAcknowledgementController_individual_returnsIndividualController() throws IOException {
    DestinationImpl destination = Mockito.mock(DestinationImpl.class);

    SubscriptionContext context = Mockito.mock(SubscriptionContext.class);
    Mockito.when(context.getSelector()).thenReturn(null);
    Mockito.when(context.getReceiveMaximum()).thenReturn(10);
    Mockito.when(context.getCreditHandler()).thenReturn(CreditHandler.AUTO);

    TestSubscriptionBuilder builder = new TestSubscriptionBuilder(destination, context);

    AcknowledgementController controller = builder.createAcknowledgementControllerForTest(ClientAcknowledgement.INDIVIDUAL);
    Assertions.assertInstanceOf(IndividualAcknowledgementController.class, controller);
  }

  @Test
  void createAcknowledgementController_block_returnsClientAckController() throws IOException {
    DestinationImpl destination = Mockito.mock(DestinationImpl.class);

    SubscriptionContext context = Mockito.mock(SubscriptionContext.class);
    Mockito.when(context.getSelector()).thenReturn(null);
    Mockito.when(context.getReceiveMaximum()).thenReturn(10);
    Mockito.when(context.getCreditHandler()).thenReturn(CreditHandler.AUTO);

    TestSubscriptionBuilder builder = new TestSubscriptionBuilder(destination, context);

    AcknowledgementController controller = builder.createAcknowledgementControllerForTest(ClientAcknowledgement.BLOCK);
    Assertions.assertInstanceOf(ClientAcknowledgementController.class, controller);
  }

  @Test
  void createAcknowledgementController_auto_returnsAutoController() throws IOException {
    DestinationImpl destination = Mockito.mock(DestinationImpl.class);

    SubscriptionContext context = Mockito.mock(SubscriptionContext.class);
    Mockito.when(context.getSelector()).thenReturn(null);
    Mockito.when(context.getReceiveMaximum()).thenReturn(10);
    Mockito.when(context.getCreditHandler()).thenReturn(CreditHandler.AUTO);

    TestSubscriptionBuilder builder = new TestSubscriptionBuilder(destination, context);

    AcknowledgementController controller = builder.createAcknowledgementControllerForTest(ClientAcknowledgement.AUTO);
    Assertions.assertInstanceOf(AutoAcknowledgementController.class, controller);
  }

  private static final class TestSubscriptionBuilder extends SubscriptionBuilder {

    TestSubscriptionBuilder(DestinationImpl destination, SubscriptionContext context) throws IOException {
      super(destination, context);
    }

    TestSubscriptionBuilder(DestinationImpl destination, SubscriptionContext context, SubscriptionContext parent) throws IOException {
      super(destination, context, parent);
    }

    @Override
    public Subscription construct(SessionImpl session, String sessionId, String uniqueSessionId, long sessionUniqueId) {
      throw new UnsupportedOperationException("Not required for builder unit tests.");
    }

    Object getParserExecutor() {
      return parserExecutor;
    }

    String combineSelectorsForTest(String lhs, String rhs) {
      return super.combineSelectors(lhs, rhs);
    }

    CreditManager createCreditManagerForTest(SubscriptionContext context) {
      return super.createCreditManager(context);
    }

    AcknowledgementController createAcknowledgementControllerForTest(ClientAcknowledgement ack) {
      return super.createAcknowledgementController(ack);
    }
  }
}
