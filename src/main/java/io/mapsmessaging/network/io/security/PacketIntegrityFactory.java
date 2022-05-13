package io.mapsmessaging.network.io.security;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

public class PacketIntegrityFactory {

  private static final PacketIntegrityFactory instance = new PacketIntegrityFactory();
  public static PacketIntegrityFactory getInstance(){
    return instance;
  }

  public List<String> getAlgorithms(){
    return new ArrayList<>(implementations.keySet());
  }

  private final Map<String, PacketIntegrity> implementations;

  public PacketIntegrity getPacketIntegrity(String algoritm, SignatureManager stamper, byte[] key) throws NoSuchAlgorithmException, InvalidKeyException {
    return implementations.get(algoritm).initialise(stamper, key);
  }

  private PacketIntegrityFactory(){
    implementations = new LinkedHashMap<>();
    ServiceLoader<PacketIntegrity> instanceList = ServiceLoader.load(PacketIntegrity.class);
    for(PacketIntegrity instance:instanceList){
      implementations.put(instance.getName(), instance);
    }
  }
}
