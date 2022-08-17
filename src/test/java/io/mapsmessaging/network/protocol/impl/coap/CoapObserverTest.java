package io.mapsmessaging.network.protocol.impl.coap;

import java.io.IOException;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapObserveRelation;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.elements.exception.ConnectorException;
import org.junit.jupiter.api.Test;

class CoapObserverTest extends BaseCoapTest {


  @Test
  void testObserver() throws ConnectorException, IOException, InterruptedException {
    CoapClient client = new CoapClient(getUri());
    client.put("Test payload...".getBytes(), 0);

    CoapObserveRelation relation = client.observe(new CoapHandler() {
      @Override
      public void onLoad(CoapResponse coapResponse) {
        System.err.println("onload");
      }

      @Override
      public void onError() {
        System.err.println("on-error");
      }
    });

    relation.setNotificationListener((request, response) -> System.err.println(request + " >> " + response));
    relation.waitForResponse(10000);
    client.put("Test payload...".getBytes(), 0);
    client.put("Test payload...".getBytes(), 0);
    client.put("Test payload...".getBytes(), 0);
    Thread.sleep(2000);
    client.shutdown();
  }
}
