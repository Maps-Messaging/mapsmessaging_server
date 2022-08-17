package io.mapsmessaging.network.protocol.impl.coap;

import io.mapsmessaging.test.BaseTestConfig;
import java.util.concurrent.atomic.AtomicLong;

public class BaseCoapTest extends BaseTestConfig {

  private static final String uri = "coap://127.0.0.1:5683/fred-";
  private static final AtomicLong counter = new AtomicLong(0);

  protected static String getUri() {
    return uri + counter.incrementAndGet();
  }
}
