package io.mapsmessaging.network.io.security;

import io.mapsmessaging.network.io.security.impl.signature.AppenderSignatureManager;
import io.mapsmessaging.network.io.security.impl.signature.PrependerSignatureManager;
import io.mapsmessaging.utilities.configuration.ConfigurationProperties;
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

  public PacketIntegrity createPacketIntegrity(ConfigurationProperties properties){
    String hmacAlgorithm = properties.getProperty("HmacAlgorithm");
    if(hmacAlgorithm != null) {
      String managerName = properties.getProperty("HmacManager", "Appender");
      SignatureManager manager;
      if (managerName.equalsIgnoreCase("appender")) {
        manager = new AppenderSignatureManager();
      } else {
        manager = new PrependerSignatureManager();
      }
      String keyStr = properties.getProperty("HmacSharedKey");
      byte[] key = SharedKeyHelper.convertKey(keyStr);
      try {
        return PacketIntegrityFactory.getInstance().getPacketIntegrity(hmacAlgorithm, manager, key);
      } catch (NoSuchAlgorithmException | InvalidKeyException e) {
      }
    }
    return null;
  }

  private PacketIntegrityFactory(){
    implementations = new LinkedHashMap<>();
    ServiceLoader<PacketIntegrity> instanceList = ServiceLoader.load(PacketIntegrity.class);
    for(PacketIntegrity instance:instanceList){
      implementations.put(instance.getName(), instance);
    }
  }
}
