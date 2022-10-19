package io.mapsmessaging.rest;

import static spark.Spark.get;
import static spark.Spark.ipAddress;
import static spark.Spark.port;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.network.EndPointManager;
import io.mapsmessaging.rest.data.ProtocolInfo;
import io.mapsmessaging.rest.data.Transformation;
import io.mapsmessaging.utilities.configuration.ConfigurationProperties;
import java.util.ArrayList;
import java.util.List;

public class ServerQueryApi {

  public void initRestApi(ConfigurationProperties map){

    if(map.getBooleanProperty("enabled", false)) {
      port(map.getIntProperty("port", 4567));
      ipAddress(map.getProperty("host", "0.0.0.0"));
      get("/server/name", (req, res) -> MessageDaemon.getInstance().getId());
      get("/server/destinations", (req, res) -> MessageDaemon.getInstance().getDestinationManager().getAll(), new Transformation());
      get("/server/protocols", (req, res) -> {
        List<EndPointManager> endPointManagers = MessageDaemon.getInstance().getNetworkManager().getAll();
        List<ProtocolInfo> protocols = new ArrayList<>();
        for (EndPointManager endPointManager : endPointManagers) {
          ProtocolInfo protocol = new ProtocolInfo();
          protocol.setName(endPointManager.getName());
          protocol.setProtocol(endPointManager.getProtocols());
          protocol.setPort(endPointManager.getEndPointServer().getUrl().getPort());
          protocol.setHost(endPointManager.getEndPointServer().getUrl().getHost());
          protocols.add(protocol);
        }
        return protocols.toArray(new ProtocolInfo[0]);
      }, new Transformation());
    }
  }

}