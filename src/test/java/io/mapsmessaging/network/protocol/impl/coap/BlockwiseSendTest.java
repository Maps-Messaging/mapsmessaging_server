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

package io.mapsmessaging.network.protocol.impl.coap;

import java.io.IOException;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.elements.exception.ConnectorException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class BlockwiseSendTest extends BaseCoapTest {

  @ParameterizedTest
  @ValueSource(ints = {0,1,2,3,4,5,6})
  void basicSendTest(int size) throws ConnectorException, IOException {
    CoapClient client = new CoapClient(getUri());
    client.setTimeout(10_000L);
    try {
      byte[] payload = new byte[1024];
      for (int index = 0; index < payload.length; index++) {
        payload[index] = (byte) (32 + (index % 36));
      }
      int blockSize = 1 << (size + 4);
      int blockNumber = 0;
      byte[] blockPayload = new byte[blockSize];
      for (int offset = 0; offset < payload.length; offset += blockSize) {
        boolean more = offset + blockSize != payload.length;
        Request put = Request.newPut();
        System.arraycopy(payload, offset, blockPayload, 0, blockPayload.length);
        put.setPayload(blockPayload);
        put.getOptions().setBlock1(size, more, blockNumber);
        CoapResponse response = client.advanced(put);
        Assertions.assertNotNull(response, "No response for block " + blockNumber);
        if (more) {
          Assertions.assertEquals(CoAP.ResponseCode.CONTINUE, response.getCode(), "Intermediate block " + blockNumber);
        } else {
          Assertions.assertTrue(response.getCode() == CoAP.ResponseCode.CREATED || response.getCode() == CoAP.ResponseCode.CHANGED,
              "Unexpected final PUT response: " + response.getCode());
        }
        blockNumber++;
      }

      CoapResponse response = client.get();
      Assertions.assertNotNull(response, "GET did not receive the completed blockwise PUT");
      Assertions.assertArrayEquals(payload, response.getPayload());
    } finally {
      client.shutdown();
    }
  }

}
