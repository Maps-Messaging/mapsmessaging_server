package org.maps.network.io.connection.state;

import java.io.IOException;
import org.maps.network.io.connection.EndPointConnection;

public class ShutdownState extends State {

  public ShutdownState(EndPointConnection connection) {
    super(connection);
  }

  @Override
  public void execute() {
    if(endPointConnection.getConnection() != null){
      try {
        endPointConnection.getConnection().close();
      } catch (IOException ioException) {
        ioException.printStackTrace();
      }
    }
  }
}
