package org.maps.network.io.connection.state;

import org.maps.network.io.connection.EndPointConnection;

public class Connecting extends State {

  public Connecting(EndPointConnection connection) {
    super(connection);
  }

  @Override
  public void execute() {
    // Need to wait for the protocol to be established before we can move on
  }
}
