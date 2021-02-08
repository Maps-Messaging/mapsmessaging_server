package org.maps.network.io.connection.state;

import org.maps.network.io.connection.EndPointConnection;

public class Established extends State {

  public Established(EndPointConnection connection) {
    super(connection);
  }

  @Override
  public void execute() {
    // No Op, nothing to do until we disconnect
  }
}
