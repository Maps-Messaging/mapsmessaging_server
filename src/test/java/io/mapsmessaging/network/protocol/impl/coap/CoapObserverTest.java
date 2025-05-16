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

import io.mapsmessaging.test.WaitForState;
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


    for(int x=0;x<4;x++) {
      putAndWait(client, counter, x+1);
    }

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

    for(int x=0;x<4;x++) {
      putAndWait(client, counter, x+1);
    }

    observeRelation.proactiveCancel();
    TimeUnit.SECONDS.sleep(1);
    counter.set(0);
    client.put("Test payload...".getBytes(), 0);
    client.put("Test payload...".getBytes(), 0);
    client.put("Test payload...".getBytes(), 0);
    client.put("Test payload...".getBytes(), 0);
    int count = 0;
    while(counter.get() == 0 && count < 2){
      TimeUnit.SECONDS.sleep(1);
      count++;
    }
    Assertions.assertEquals(0, counter.get());

    client.shutdown();
  }


  private void putAndWait(CoapClient client, AtomicLong counter, long expected) throws ConnectorException, IOException {
    client.put("Test payload...".getBytes(), 0);
    WaitForState.waitFor(100, TimeUnit.MILLISECONDS, () -> counter.get() == expected);
    Assertions.assertEquals(expected, counter.get());

  }

  @Test
  void testLargePublishRateSubscriber() throws ConnectorException, IOException, InterruptedException {
    String testUri = getUri();
    CoapClient client = new CoapClient(testUri);
    CoapClient publisher = new CoapClient(testUri);
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
    }, 10000);

    for(int x=0;x<1000;x++) {
      publisher.put("Test payload...".getBytes(), 0);
   }
    TimeUnit.SECONDS.sleep(1);
    long count = 0;
    AtomicLong init = new AtomicLong(counter.get()+1);
    while(count < 10){
      publisher.put("Test payload...".getBytes(), 0);
      WaitForState.waitFor(5, TimeUnit.SECONDS, () -> counter.get() == init.get());
      Assertions.assertEquals(init.get(), counter.get());
      init.incrementAndGet();
      count++;
    }
    client.shutdown();
    publisher.shutdown();
  }
}
