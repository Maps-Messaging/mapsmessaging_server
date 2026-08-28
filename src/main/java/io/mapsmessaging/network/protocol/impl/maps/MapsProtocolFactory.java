/*
 * Copyright [ 2020 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 */
package io.mapsmessaging.network.protocol.impl.maps;

import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.network.protocol.ProtocolImplFactory;
import io.mapsmessaging.network.protocol.detection.MultiByteArrayDetection;
import java.io.IOException;
import java.util.Map;

public class MapsProtocolFactory extends ProtocolImplFactory {

  private static final byte[][] SIGNATURE = {{'M', 'A', 'P', 'S'}};

  public MapsProtocolFactory() {
    super("MAPS", "MapsMessaging native protocol version 1", new MultiByteArrayDetection(SIGNATURE, 0));
  }

  @Override
  public Protocol connect(EndPoint endPoint, String sessionId, String username, String password, Map<String, String> topicMap) throws IOException {
    MapsProtocol protocol = new MapsProtocol(endPoint);
    protocol.getTopicNameMapping().putAll(topicMap);
    protocol.connect(sessionId, username, password);
    return protocol;
  }

  @Override
  public void create(EndPoint endPoint, Packet packet) throws IOException {
    new MapsProtocol(endPoint, packet);
  }

  @Override
  public String getTransportType() {
    return "stream";
  }
}
