package io.mapsmessaging.license.features;

import lombok.Data;

@Data
public class Features {
  private String name;
  private boolean ml;

  private Network network;
  private Protocols protocols;
  private Management management;
  private InterConnections interConnections;
  private Storage storage;
  private Hardware hardware;
  private Engine engine;
}
