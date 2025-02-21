package io.mapsmessaging.network.protocol.impl.extension;

import io.mapsmessaging.network.EndPointURL;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.EndPointConnectedCallback;
import io.mapsmessaging.network.io.EndPointConnectionFactory;
import io.mapsmessaging.network.io.EndPointServerStatus;
import io.mapsmessaging.network.io.impl.SelectorLoadManager;

import java.io.IOException;
import java.util.List;

public class ExtensionEndPointConnectionFactory implements EndPointConnectionFactory {

  @java.lang.SuppressWarnings({"squid:S4818", "squid:S2095"})
  @Override
  public EndPoint connect(EndPointURL url, SelectorLoadManager selector, EndPointConnectedCallback connectedCallback, EndPointServerStatus endPointServerStatus,
                          List<String> jmxPath) throws IOException {
    EndPoint endPoint = new ExtensionEndPoint(generateID(), endPointServerStatus);
    connectedCallback.connected(endPoint);
    endPoint.getServer().handleNewEndPoint(endPoint);
    return endPoint;
  }

  @Override
  public String getName() {
    return "extension";
  }

  @Override
  public String getDescription() {
    return "Dummy extension end point factory";
  }

}
