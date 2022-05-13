package io.mapsmessaging.network.io.security;

import io.mapsmessaging.network.io.Packet;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public interface PacketIntegrity {

  PacketIntegrity initialise(SignatureManager stamper, byte[] key) throws NoSuchAlgorithmException, InvalidKeyException;

  String getName();

  boolean isSecure(Packet packet);

  boolean isSecure(Packet packet, int offset, int length);

  Packet secure(Packet packet);
  Packet secure(Packet packet, int offset, int length);

  int size();

  void reset();

}
