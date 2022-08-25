package io.mapsmessaging.network.protocol.impl.coap;

import java.io.IOException;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
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
    byte[] tmp = new byte[1024];
    for(int x=0;x<tmp.length;x++){
      tmp[x] = (byte)((32+x)%10);
    }
    int blockSize = 1<<(size+4);
    int id = 0;
    byte[] buf = new byte[blockSize];
    for(int x=0;x<tmp.length;x+=blockSize){
      Request put = Request.newPut();
      System.arraycopy(tmp, x, buf, 0, buf.length);
      put.setPayload(buf);
      put.getOptions().setBlock1(size, (x+blockSize == tmp.length), id);
      CoapResponse response1 = client.advanced(put);
      Assertions.assertNotNull(response1);
      id++;
    }

    CoapResponse getResponse = client.get();

    Assertions.assertNotNull(getResponse); // Should not be null
    Assertions.assertArrayEquals(tmp, getResponse.getPayload());
    client.shutdown();
  }

}
