package io.mapsmessaging.network.protocol.impl.stomp;

import io.mapsmessaging.test.WaitForState;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import javax.security.auth.login.LoginException;
import net.ser1.stomp.Client;
import net.ser1.stomp.Listener;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ExpiredEventTest extends StompBaseTest implements Listener{

  private static final int EVENT_COUNT = 10;

  private LongAdder adder = new LongAdder();

  @Test
  void expiryEventTest() throws IOException, LoginException, InterruptedException {
    adder.reset();
    Client client = new Client("127.0.0.1", 8675, null, null);
    Assertions.assertTrue(client.isConnected());
    Map<String, String> map = new HashMap<>();
    map.put("id", "subscribe1");
    String topicName = getTopicName();
    client.subscribeW(topicName, this, map);
    Map<String, String> publishMap = new HashMap<>();
    publishMap.put("expiry", "1");
    for(int x=0;x<EVENT_COUNT;x++){
      String json = "{\"temperature\":28, \"count\":"+x+", \"testName\":simpleSelectorTest, \"odd\":"+x%2+"}";
      client.send(topicName, json, publishMap);
    }
    WaitForState.waitFor(4, TimeUnit.SECONDS, () -> adder.sum() == EVENT_COUNT);

    Thread.sleep(10000);
    client.disconnect();
    Assertions.assertEquals(EVENT_COUNT, adder.sum());
  }

  @Override
  public void message(Map map, String s) {
    adder.increment();
  }
}
