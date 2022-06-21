package io.mapsmessaging.api.features;

import lombok.Getter;

public enum DestinationMode {
  NORMAL(0,"Normal", "Normal Event publish/subscription", true),
  SCHEMA(1,"Schema", "Access to the destinations schema data", true),
  METRICS(2,"Metrics", "Access to the destinations metrics", false);


  @Getter private final int id;
  @Getter private final String name;
  @Getter private final String description;
  @Getter private final boolean publishable;

  private DestinationMode(int id, String name, String description, boolean publishable){
    this.id = id;
    this.name = name;
    this.description = description;
    this.publishable = publishable;
  }

  public static DestinationMode getInstance(int id) {
    switch (id) {
      case 0:
        return NORMAL;

      case 1:
        return SCHEMA;

      case 2:
        return METRICS;

      default:
        throw new IllegalArgumentException("Invalid handestination  mode value supplied:" + id);
    }
  }
}

