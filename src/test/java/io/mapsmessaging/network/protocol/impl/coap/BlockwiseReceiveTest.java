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
      byte[] tmp = new byte[1024];
      for(int x=0;x<tmp.length;x++){
        tmp[x] = (byte)((32+x)%10);
      }
      CoapResponse response =client.put(tmp, 0);
      Assertions.assertNotNull(response);
      Assertions.assertEquals(SUCCESS_RESPONSE.value, response.getCode().codeClass);

      Request request = Request.newGet();
      request.getOptions().setBlock2(size, false, 0);
      CoapResponse response1 = client.advanced(request);
      Assertions.assertArrayEquals(tmp, response1.getPayload());
      client.shutdown();
  }

}
