package io.mapsmessaging.rest.impl;

import static spark.Spark.get;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.network.EndPointManager;
import io.mapsmessaging.rest.RestApi;
import io.mapsmessaging.rest.data.ProtocolInfo;
import io.mapsmessaging.rest.transformation.JsonTransformation;
import java.util.ArrayList;
import java.util.List;
import spark.Response;

public class ServerQueryApi implements RestApi {

  public void initialise(){
    get("/server/name", (req, res) -> getName(res));
    get("/server/destinations", (req, res) -> getDestinations(res), new JsonTransformation());
    get("/server/protocols", (req, res) -> getProtocols(res), new JsonTransformation());
  }

  private String getName(Response res) {
    res.type("application/json");
    return MessageDaemon.getInstance().getId();
  }

  private List<String> getDestinations(Response res) {
    res.type("application/json");
    return MessageDaemon.getInstance().getDestinationManager().getAll();
  }

  private List<ProtocolInfo> getProtocols(Response res) {
    res.type("application/json");
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
    return protocols;
  }

}