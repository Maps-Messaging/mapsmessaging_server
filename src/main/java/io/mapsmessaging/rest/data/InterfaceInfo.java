package io.mapsmessaging.rest.data;

import io.mapsmessaging.network.EndPointManager;
import io.swagger.annotations.ApiModelProperty;
import java.util.Map;
import lombok.Getter;
import lombok.ToString;

@ToString
public class InterfaceInfo {

  @Getter
  @ApiModelProperty(value="Unique name of the interface")
  private final String name;

  @Getter
  @ApiModelProperty(value="Port that the interface is bound to")
  private final int port;

  @Getter
  @ApiModelProperty(value="Host that the interface is bound to")
  private final String host;

  @Getter
  @ApiModelProperty(value="Current state of the interface")
  private final String state;

  @Getter
  @ApiModelProperty(value="Configuration for the interface")
  private final Map<String, Object> config;

  public InterfaceInfo(EndPointManager endPointManager){
    name = (endPointManager.getEndPointServer().getConfig().getProperties().getProperty("name"));
    port = (endPointManager.getEndPointServer().getUrl().getPort());
    host = (endPointManager.getEndPointServer().getUrl().getHost());
    config = (endPointManager.getEndPointServer().getConfig().getProperties().getMap());
    switch (endPointManager.getState()) {
      case START:
        state = ("Started");
        break;
      case STOPPED:
        state = ("Stopped");
        break;
      case PAUSED:
        state = ("Paused");
        break;

      default:
        state = ("Unknown");
    }

  }

}
