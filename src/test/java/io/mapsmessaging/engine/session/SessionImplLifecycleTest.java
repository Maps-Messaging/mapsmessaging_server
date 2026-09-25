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

package io.mapsmessaging.engine.session;

import io.mapsmessaging.engine.destination.DestinationFactory;
import io.mapsmessaging.engine.destination.subscription.SubscriptionController;
import io.mapsmessaging.engine.session.security.SecurityContext;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class SessionImplLifecycleTest {

  @Test
  void closeReleasesSessionResourcesWithoutDestroyingSubscriptionController() {
    SessionContext context = mock(SessionContext.class);
    SecurityContext securityContext = mock(SecurityContext.class);
    DestinationFactory destinationManager = mock(DestinationFactory.class);
    SubscriptionController controller = mock(SubscriptionController.class);
    ClientConnection clientConnection = mock(ClientConnection.class);

    when(context.getId()).thenReturn("session-close-ownership");
    when(context.getExpiry()).thenReturn(0L);
    when(context.getClientConnection()).thenReturn(clientConnection);
    when(clientConnection.getTimeOut()).thenReturn(0L);

    SessionImpl session = new SessionImpl(context, securityContext, destinationManager, controller);

    session.close();

    verify(securityContext).logout();
    verify(controller, never()).close(anyBoolean());
    verify(controller, never()).shutdown();
  }
}
