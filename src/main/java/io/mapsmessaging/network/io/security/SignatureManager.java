package io.mapsmessaging.network.io.security;

import io.mapsmessaging.network.io.Packet;

public interface SignatureManager {

  byte[] getSignature(Packet packet, byte[] signature);

  Packet setSignature(Packet packet, byte[] signature);

  Packet getData(Packet packet, int size);

}
