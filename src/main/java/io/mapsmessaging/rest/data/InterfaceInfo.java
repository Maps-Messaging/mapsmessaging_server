package io.mapsmessaging.rest.data;

import io.mapsmessaging.network.EndPointManager;
import io.swagger.annotations.ApiModelProperty;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
public class InterfaceInfo {

  @Getter
  @Setter
  @ApiModelProperty(value="Unique name of the interface")
  private String name;

  @Getter
  @Setter
  @ApiModelProperty(value="Port that the interface is bound to")
  private int port;

  @Getter
  @Setter
  @ApiModelProperty(value="Host that the interface is bound to")
  private String host;

  @Getter
  @Setter
  @ApiModelProperty(value="Current state of the interface")
  private String state;

  @Getter
  @Setter
  @ApiModelProperty(value="Configuration for the interface")
  private Map<String, Object> config;

  public InterfaceInfo(){}

  public InterfaceInfo(EndPointManager endPointManager){
    setName(endPointManager.getEndPointServer().getConfig().getProperties().getProperty("name"));
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
