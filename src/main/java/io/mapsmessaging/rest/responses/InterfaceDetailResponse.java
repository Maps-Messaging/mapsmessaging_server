package io.mapsmessaging.rest.responses;

import io.mapsmessaging.rest.data.InterfaceInfo;
import io.mapsmessaging.utilities.configuration.ConfigurationProperties;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.Request;
import lombok.Getter;

public class InterfaceDetailResponse extends BaseResponse {

  @Getter
  @ApiModelProperty(value="List of interfaces")
  private final List<InterfaceInfo> data;

  @Getter
  @ApiModelProperty(value="Default values for all interfaces used unless overridden by the specific config")
  private final Map<String, Object> globalConfig;


  public InterfaceDetailResponse(Request request, List<InterfaceInfo> list, ConfigurationProperties map){
    super(request);
    data = list;
    if(map != null) {
      globalConfig = map.getMap();
    }
    else{
      globalConfig = null;
    }
  }

}
