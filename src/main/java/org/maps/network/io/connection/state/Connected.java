package org.maps.network.io.connection.state;

import java.util.LinkedHashMap;
import java.util.Map;
import org.maps.network.io.connection.EndPointConnection;
import org.maps.utilities.configuration.ConfigurationProperties;

public class Connected extends State {

  public Connected(EndPointConnection connection) {
    super(connection);
  }

  @Override
  public void execute() {
    try {
      Map<String, String> destinationMap = new LinkedHashMap<>();
      ConfigurationProperties properties = endPointConnection.getProperties();
      if (properties.getProperty("direction").equalsIgnoreCase("pull")) {
        destinationMap.put(properties.getProperty("remote_namespace"), properties.getProperty("local_namespace"));
        endPointConnection.getConnection().subscribeRemote(properties.getProperty("remote_namespace"), destinationMap);
      } else if (properties.getProperty("direction").equalsIgnoreCase("push")) {
        destinationMap.put(properties.getProperty("local_namespace"), properties.getProperty("remote_namespace"));
        endPointConnection.getConnection().subscribeLocal(properties.getProperty("local_namespace"), destinationMap);
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
