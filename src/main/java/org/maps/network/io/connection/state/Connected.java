package org.maps.network.io.connection.state;

import java.util.List;
import org.maps.network.io.connection.EndPointConnection;
import org.maps.utilities.configuration.ConfigurationProperties;

public class Connected extends State {

  public Connected(EndPointConnection connection) {
    super(connection);
  }

  @Override
  public void execute() {
    try {
      List<ConfigurationProperties> properties = endPointConnection.getDestinationMappings();
      for(ConfigurationProperties property:properties) {
        String direction = property.getProperty("direction");
        String local =  property.getProperty("local_namespace");
        String remote =  property.getProperty("remote_namespace");
        if (direction.equalsIgnoreCase("pull")) {
          endPointConnection.getConnection().subscribeRemote(remote, local);
        } else if (direction.equalsIgnoreCase("push")) {
          endPointConnection.getConnection().subscribeLocal(local, remote);
        }
      }
      setState(new Established(endPointConnection));
    }
    catch (Exception exception){
      exception.printStackTrace();
      endPointConnection.close();
      setState(new Disconnected(endPointConnection));
    }
  }

}
