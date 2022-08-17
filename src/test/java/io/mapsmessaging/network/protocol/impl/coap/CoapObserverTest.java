package io.mapsmessaging.network.protocol.impl.coap;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapObserveRelation;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.elements.exception.ConnectorException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class CoapObserverTest extends BaseCoapTest {


  @Test
  void testObserver() throws ConnectorException, IOException, InterruptedException {
    CoapClient client = new CoapClient(getUri());
    AtomicLong counter = new AtomicLong(0);
    client.observe(new CoapHandler() {
      @Override
      public void onLoad(CoapResponse coapResponse) {
        counter.incrementAndGet();
      }

      @Override
      public void onError() {
        System.err.println("on-error");
      }
    }, 4);

    client.put("Test payload...".getBytes(), 0);
    client.put("Test payload...".getBytes(), 0);
    client.put("Test payload...".getBytes(), 0);
    client.put("Test payload...".getBytes(), 0);
    int count = 0;
    while(counter.get() != 4 && count < 10){
      TimeUnit.SECONDS.sleep(1);
      count++;
    }
    Assertions.assertEquals(4, counter.get());
    client.shutdown();
  }

  @Test
  void testObserverAndClear() throws ConnectorException, IOException, InterruptedException {
    CoapClient client = new CoapClient(getUri());
    AtomicLong counter = new AtomicLong(0);
    CoapObserveRelation observeRelation = client.observe(new CoapHandler() {
      @Override
      public void onLoad(CoapResponse coapResponse) {
        counter.incrementAndGet();
      }

      @Override
      public void onError() {
        System.err.println("on-error");
      }
    }, 8);

    client.put("Test payload...".getBytes(), 0);
    client.put("Test payload...".getBytes(), 0);
    client.put("Test payload...".getBytes(), 0);
    client.put("Test payload...".getBytes(), 0);
    int count = 0;
    while(counter.get() != 4 && count < 10){
      TimeUnit.SECONDS.sleep(1);
      count++;
    }
    Assertions.assertEquals(4, counter.get());
    observeRelation.proactiveCancel();
    TimeUnit.SECONDS.sleep(1);
    counter.set(0);
    client.put("Test payload...".getBytes(), 0);
    client.put("Test payload...".getBytes(), 0);
    client.put("Test payload...".getBytes(), 0);
    client.put("Test payload...".getBytes(), 0);
    count = 0;
    while(counter.get() == 0 && count < 2){
      TimeUnit.SECONDS.sleep(1);
      count++;
    }
    Assertions.assertEquals(0, counter.get());

    client.shutdown();
  }
}
