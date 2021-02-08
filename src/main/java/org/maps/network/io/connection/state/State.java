package org.maps.network.io.connection.state;

import org.maps.network.io.connection.EndPointConnection;

public abstract class State implements Runnable {

  protected final EndPointConnection endPointConnection;
  private final long timeout;

  public State(EndPointConnection connection){
    this.endPointConnection = connection;
    timeout = System.currentTimeMillis() + 60000;
  }

  public boolean hasTimedOut(){
    return timeout < System.currentTimeMillis();
  }

  public void setState(State state){
    endPointConnection.setState(state);
  }

  public abstract void execute();

  public void run(){
    execute();
  }
}
