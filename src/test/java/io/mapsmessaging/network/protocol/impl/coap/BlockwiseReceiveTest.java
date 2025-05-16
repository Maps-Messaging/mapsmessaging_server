/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
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

package io.mapsmessaging.network.protocol.impl.coap;

import static org.eclipse.californium.core.coap.CoAP.CodeClass.SUCCESS_RESPONSE;

import java.io.IOException;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.elements.exception.ConnectorException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class BlockwiseReceiveTest extends BaseCoapTest {

  @ParameterizedTest
  @ValueSource(ints = {0,1,2,3,4,5,6})
  void basicReceiveTest(int size) throws ConnectorException, IOException {
    CoapClient client = new CoapClient(getUri());
    byte[] tmp = new byte[256];
    for (int x = 0; x < tmp.length; x++) {
      tmp[x] = (byte) (32 + (x % 36));
    }
    CoapResponse response = client.put(tmp, 0);
    Assertions.assertNotNull(response);
    Assertions.assertEquals(SUCCESS_RESPONSE.value, response.getCode().codeClass);

    delay(100);
    Request request = Request.newGet();
    request.getOptions().setBlock2(size, false, 0);
    CoapResponse response1 = client.advanced(request);
    Assertions.assertNotNull(response1); // Should not be null
    Assertions.assertArrayEquals(tmp, response1.getPayload());
    delay(100);
    client.shutdown();
  }

}
