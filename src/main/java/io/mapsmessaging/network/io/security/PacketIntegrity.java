package io.mapsmessaging.network.io.security;

import io.mapsmessaging.network.io.Packet;

public interface PacketIntegrity {

  boolean isSecure(Packet packet);
  boolean isSecure(Packet packet, int offset, int length);

  boolean secure(Packet packet);
  boolean secure(Packet packet, int offset, int length);

}
