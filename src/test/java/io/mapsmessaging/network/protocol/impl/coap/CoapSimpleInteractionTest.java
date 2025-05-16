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
import org.eclipse.californium.core.coap.CoAP.Code;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.coap.OptionSet;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.elements.exception.ConnectorException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class CoapSimpleInteractionTest extends BaseCoapTest {

  @Test
  void simplePing() {
    CoapClient client = new CoapClient(getUri());
    Assertions.assertTrue(client.ping());
    client.shutdown();
  }

  @Test
  void simplePutGetCheck() throws ConnectorException, IOException {
    CoapClient client = new CoapClient(getUri());
    CoapResponse response =client.put("this is simply bytes".getBytes(), 0);
    Assertions.assertNotNull(response);
    Assertions.assertEquals(SUCCESS_RESPONSE.value, response.getCode().codeClass);
    response = client.get();
    Assertions.assertNotNull(response);
    Assertions.assertEquals(SUCCESS_RESPONSE.value, response.getCode().codeClass);
    Assertions.assertArrayEquals("this is simply bytes".getBytes(), response.getPayload());
    client.shutdown();
  }

  @Test
  void simplePutDeleteCheck() throws ConnectorException, IOException {
    CoapClient client = new CoapClient(getUri());
    CoapResponse response =  client.put("this is simply bytes".getBytes(), 0);
    Assertions.assertNotNull(response);
    Assertions.assertEquals(SUCCESS_RESPONSE.value, response.getCode().codeClass);
    response = client.get();
    Assertions.assertNotNull(response);
    Assertions.assertEquals(SUCCESS_RESPONSE.value, response.getCode().codeClass);
    Assertions.assertArrayEquals("this is simply bytes".getBytes(), response.getPayload());
    response = client.delete();
    Assertions.assertNotNull(response);
    Assertions.assertEquals(SUCCESS_RESPONSE.value, response.getCode().codeClass);
    client.shutdown();
  }

  @Test
  void simplePutIfMatchCheck() throws ConnectorException, IOException {
    CoapClient client = new CoapClient(getUri());
    client.put("this is simply bytes".getBytes(), 0);
    Request request = new Request(Code.PUT);
    request.setPayload("this is simply bytes".getBytes());
    OptionSet optionSet = new OptionSet();
    optionSet.setContentFormat(0);
    optionSet.addETag("tag1".getBytes());
    optionSet.addETag("tag2".getBytes());
    optionSet.addETag("tag3".getBytes());
    request.setOptions(optionSet);
    CoapResponse response = client.advanced(request);
    Assertions.assertNotNull(response);
    Assertions.assertEquals(SUCCESS_RESPONSE.value, response.getCode().codeClass);
    delay(100);
    response = client.get();
    Assertions.assertNotNull(response);
    Assertions.assertEquals(SUCCESS_RESPONSE.value, response.getCode().codeClass);
    Assertions.assertArrayEquals("this is simply bytes".getBytes(), response.getPayload());
    delay(100);
    response = client.putIfMatch("This should NOT change", 0, "tag5".getBytes());
    Assertions.assertNotNull(response);
    Assertions.assertEquals(ResponseCode.PRECONDITION_FAILED.codeClass, response.getCode().codeClass);
    Assertions.assertEquals(ResponseCode.PRECONDITION_FAILED.codeDetail, response.getCode().codeDetail);

    delay(100);
    response = client.get();
    Assertions.assertNotNull(response);
    Assertions.assertEquals(SUCCESS_RESPONSE.value, response.getCode().codeClass);
    Assertions.assertArrayEquals("this is simply bytes".getBytes(), response.getPayload());

    delay(100);
    client.putIfMatch("This should change", 0, "tag2".getBytes());
    Assertions.assertNotNull(response);
    Assertions.assertEquals(SUCCESS_RESPONSE.value, response.getCode().codeClass);
    delay(100);
    response = client.get();
    Assertions.assertNotNull(response);
    Assertions.assertEquals(SUCCESS_RESPONSE.value, response.getCode().codeClass);
    Assertions.assertArrayEquals("This should change".getBytes(), response.getPayload());
    delay(100);
    response = client.delete();
    Assertions.assertNotNull(response);
    Assertions.assertEquals(SUCCESS_RESPONSE.value, response.getCode().codeClass);
    delay(100);
    client.shutdown();
  }


  @Test
  void simplePutIfNoneMatchCheck() throws ConnectorException, IOException {
    CoapClient client = new CoapClient(getUri());
    CoapResponse response = client.put("this is simply bytes".getBytes(), 0);
    Assertions.assertNotNull(response);
    Assertions.assertEquals(SUCCESS_RESPONSE.value, response.getCode().codeClass);
    delay(100);

    response = client.get();
    Assertions.assertNotNull(response);
    Assertions.assertEquals(SUCCESS_RESPONSE.value, response.getCode().codeClass);
    Assertions.assertArrayEquals("this is simply bytes".getBytes(), response.getPayload());

    delay(100);
    response = client.putIfNoneMatch("This should NOT change", 0);
    Assertions.assertNotNull(response);
    Assertions.assertEquals(ResponseCode.PRECONDITION_FAILED.codeClass, response.getCode().codeClass);
    Assertions.assertEquals(ResponseCode.PRECONDITION_FAILED.codeDetail, response.getCode().codeDetail);

    delay(100);
    response = client.get();
    Assertions.assertNotNull(response);
    Assertions.assertEquals(SUCCESS_RESPONSE.value, response.getCode().codeClass);
    Assertions.assertArrayEquals("this is simply bytes".getBytes(), response.getPayload());


    delay(100);
    response = client.delete();
    Assertions.assertNotNull(response);
    Assertions.assertEquals(SUCCESS_RESPONSE.value, response.getCode().codeClass);


    delay(1000);
    client.putIfNoneMatch("This should change since it was deleted", 0);
    Assertions.assertNotNull(response);
    Assertions.assertEquals(SUCCESS_RESPONSE.value, response.getCode().codeClass);

    delay(100);
    response = client.get();
    Assertions.assertNotNull(response);
    Assertions.assertEquals(SUCCESS_RESPONSE.value, response.getCode().codeClass);
    Assertions.assertArrayEquals("This should change since it was deleted".getBytes(), response.getPayload());


    delay(100);
    response = client.delete();
    Assertions.assertNotNull(response);
    Assertions.assertEquals(SUCCESS_RESPONSE.value, response.getCode().codeClass);
    delay(100);
    client.shutdown();
  }
}
