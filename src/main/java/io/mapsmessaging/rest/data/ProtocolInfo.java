package io.mapsmessaging.rest.data;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;


@ToString
public class ProtocolInfo {

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

}
