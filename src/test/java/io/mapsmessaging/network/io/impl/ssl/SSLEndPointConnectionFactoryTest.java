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

package io.mapsmessaging.network.io.impl.ssl;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SSLEndPointConnectionFactoryTest {

  @Test
  void outbound_engine_enables_hostname_verification_by_default() throws Exception {
    SSLContext context = SSLContext.getInstance("TLS");
    context.init(null, null, null);

    SSLEngine sslEngine = SSLEndPointConnectionFactory.createClientEngine(context, "broker.example", 8883, true);

    Assertions.assertTrue(sslEngine.getUseClientMode());
    Assertions.assertEquals("broker.example", sslEngine.getPeerHost());
    Assertions.assertEquals(8883, sslEngine.getPeerPort());
    Assertions.assertEquals("HTTPS", sslEngine.getSSLParameters().getEndpointIdentificationAlgorithm());
  }

  @Test
  void outbound_engine_can_disable_hostname_verification() throws Exception {
    SSLContext context = SSLContext.getInstance("TLS");
    context.init(null, null, null);

    SSLEngine sslEngine = SSLEndPointConnectionFactory.createClientEngine(context, "broker.example", 8883, false);

    Assertions.assertNull(sslEngine.getSSLParameters().getEndpointIdentificationAlgorithm());
  }
}
