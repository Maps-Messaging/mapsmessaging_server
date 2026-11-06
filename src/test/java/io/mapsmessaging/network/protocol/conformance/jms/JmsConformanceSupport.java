package io.mapsmessaging.network.protocol.conformance.jms;

import io.mapsmessaging.network.protocol.impl.amqp.jms.BaseConnection;
import javax.naming.Context;
import javax.naming.NamingException;

abstract class JmsConformanceSupport extends BaseConnection {

  protected CloseableNamingContext namingContext() throws Exception {
    return new CloseableNamingContext(loadContext());
  }

  protected static final class CloseableNamingContext implements AutoCloseable {
    private final Context delegate;

    private CloseableNamingContext(Context delegate) {
      this.delegate = delegate;
    }

    Object lookup(String name) throws NamingException {
      return delegate.lookup(name);
    }

    @Override
    public void close() throws NamingException {
      delegate.close();
    }
  }
}
