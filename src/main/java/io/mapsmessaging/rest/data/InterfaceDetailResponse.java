package io.mapsmessaging.rest.data;

import java.util.List;
import javax.xml.bind.annotation.XmlRootElement;
import lombok.Getter;
import lombok.Setter;

@XmlRootElement
public class InterfaceDetailResponse {

  @Getter
  @Setter
  private List<InterfaceInfo> data;


}
