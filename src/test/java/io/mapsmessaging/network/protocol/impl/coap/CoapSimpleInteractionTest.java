package io.mapsmessaging.network.protocol.impl.coap;

import static org.eclipse.californium.core.coap.CoAP.CodeClass.SUCCESS_RESPONSE;

import java.io.IOException;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP.Code;
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

    response = client.get();
    Assertions.assertNotNull(response);
    Assertions.assertEquals(SUCCESS_RESPONSE.value, response.getCode().codeClass);
    Assertions.assertArrayEquals("this is simply bytes".getBytes(), response.getPayload());

    client.putIfMatch("This should NOT change", 0, "tag5".getBytes());
    Assertions.assertNotNull(response);
    Assertions.assertEquals(SUCCESS_RESPONSE.value, response.getCode().codeClass);

    response = client.get();
    Assertions.assertNotNull(response);
    Assertions.assertEquals(SUCCESS_RESPONSE.value, response.getCode().codeClass);
    Assertions.assertArrayEquals("this is simply bytes".getBytes(), response.getPayload());


    client.putIfMatch("This should change", 0, "tag2".getBytes());
    Assertions.assertNotNull(response);
    Assertions.assertEquals(SUCCESS_RESPONSE.value, response.getCode().codeClass);

    response = client.get();
    Assertions.assertNotNull(response);
    Assertions.assertEquals(SUCCESS_RESPONSE.value, response.getCode().codeClass);
    Assertions.assertArrayEquals("This should change".getBytes(), response.getPayload());

    response = client.delete();
    Assertions.assertNotNull(response);
    Assertions.assertEquals(SUCCESS_RESPONSE.value, response.getCode().codeClass);
    client.shutdown();
  }

}
