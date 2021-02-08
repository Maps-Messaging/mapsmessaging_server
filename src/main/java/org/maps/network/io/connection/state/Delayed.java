package org.maps.network.io.connection.state;

import java.util.concurrent.TimeUnit;
import org.maps.network.io.connection.EndPointConnection;
import org.maps.utilities.threads.SimpleTaskScheduler;

public class Delayed extends State {

  public Delayed(EndPointConnection connection) {
    super(connection);
  }

  @Override
  public void execute() {
    setState(new Disconnected(endPointConnection));
    SimpleTaskScheduler.getInstance().schedule(endPointConnection.getState(), 10, TimeUnit.SECONDS);
  }
}
