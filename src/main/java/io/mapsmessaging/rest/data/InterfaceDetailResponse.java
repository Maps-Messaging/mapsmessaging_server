package io.mapsmessaging.rest.data;

import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import java.util.Map;
import javax.xml.bind.annotation.XmlRootElement;
import lombok.Getter;
import lombok.Setter;

@XmlRootElement
public class InterfaceDetailResponse {

  @Getter
  @Setter
  @ApiModelProperty(value="List of interfaces")
  private List<InterfaceInfo> data;

  @Getter
  @Setter
  @ApiModelProperty(value="Default values for all interfaces used unless overridden by the specific config")
  private Map<String, Object> globalConfig;


}
