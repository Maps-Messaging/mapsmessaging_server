package io.mapsmessaging.engine.schema;

import lombok.Getter;

public class LinkFormat {

  @Getter
  private final String path;

  @Getter
  private final String interfaceDescription;

  @Getter
  private final String resourceType;

  public LinkFormat(String path, String interfaceDescription, String resourceType) {
    this.path = path;
    this.interfaceDescription = interfaceDescription;
    this.resourceType = resourceType;
  }

  public String pack() {
    return "<" + path + ">"
        + "if=\"" + interfaceDescription + "\";"
        + "rt=\"" + resourceType + "\"";
  }
}
