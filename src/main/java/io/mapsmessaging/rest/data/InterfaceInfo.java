package io.mapsmessaging.rest.data;

import io.mapsmessaging.network.EndPointManager;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
public class InterfaceInfo {

  @Getter
  @Setter
  private String name;

  @Getter
  @Setter
  private String protocol;

  @Getter
  @Setter
  private int port;

  @Getter
  @Setter
  private String host;

  @Getter
  @Setter
  private String state;

  @Getter
  @Setter
  private Map<String, Object> config;

  public InterfaceInfo(){}

  public InterfaceInfo(EndPointManager endPointManager){
    setName(endPointManager.getEndPointServer().getConfig().getProperties().getProperty("name"));
    setProtocol(endPointManager.getProtocols());
    setPort(endPointManager.getEndPointServer().getUrl().getPort());
    setHost(endPointManager.getEndPointServer().getUrl().getHost());
    setConfig(endPointManager.getEndPointServer().getConfig().getProperties());
    switch (endPointManager.getState()) {
      case START:
        setState("Started");
        break;
      case STOPPED:
        setState("Stopped");
        break;
      case PAUSED:
        setState("Paused");
        break;

      default:
        setState("Unknown");
    }

  }

}
