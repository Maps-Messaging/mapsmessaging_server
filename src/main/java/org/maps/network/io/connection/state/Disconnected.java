package org.maps.network.io.connection.state;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.ThreadContext;
import org.maps.network.io.EndPoint;
import org.maps.network.io.connection.EndPointConnection;
import org.maps.network.protocol.ProtocolFactory;
import org.maps.network.protocol.ProtocolImpl;
import org.maps.network.protocol.ProtocolImplFactory;
import org.maps.utilities.threads.SimpleTaskScheduler;

public class Disconnected extends State {

  public Disconnected(EndPointConnection connection) {
    super(connection);
  }

  public ProtocolImpl establishConnection() throws IOException {
    EndPoint endPoint = endPointConnection.getEndPointConnectionFactory().connect(endPointConnection.getUrl(), endPointConnection.getSelectorLoadManager(), endPointConnection, endPointConnection.getJMXPath());
    return accept(endPoint);
  }

  public ProtocolImpl accept(EndPoint endpoint) throws IOException {
    ThreadContext.put("endpoint", endPointConnection.getUrl().toString());
    String protocol = endPointConnection.getProperties().getProperty("protocol");
    ProtocolFactory protocolFactory = new ProtocolFactory(protocol);
    ProtocolImplFactory protocolImplFactory = protocolFactory.getBoundedProtocol();
    return protocolImplFactory.connect(endpoint);
  }

  @Override
  public void execute() {
    try {
      ProtocolImpl protocol = establishConnection();
      endPointConnection.setConnection(protocol);
      setState(new Connecting(endPointConnection));
    } catch (Throwable ioException) {
      setState(new Delayed(endPointConnection));
      SimpleTaskScheduler.getInstance().schedule(endPointConnection.getState(), 10, TimeUnit.SECONDS);
    }
  }
}
