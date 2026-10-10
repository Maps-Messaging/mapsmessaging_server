/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.protection;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

/** Mandatory ServiceLoader discovery. Duplicate scheme IDs fail initialization. */
public final class MqttSn2CryptoProviders {
  private final Map<Integer, MqttSn2CryptoProvider> schemes;

  public MqttSn2CryptoProviders() {
    this(ServiceLoader.load(MqttSn2CryptoProvider.class));
  }

  public MqttSn2CryptoProviders(Iterable<MqttSn2CryptoProvider> providers) {
    Map<Integer, MqttSn2CryptoProvider> loaded = new HashMap<>();
    for (MqttSn2CryptoProvider provider : providers) {
      for (int scheme : provider.supportedSchemes()) {
        if (scheme < 0 || scheme > 255 || loaded.putIfAbsent(scheme, provider) != null) {
          throw new IllegalStateException("Duplicate or invalid MQTT-SN protection scheme: " + scheme);
        }
      }
    }
    schemes = Map.copyOf(loaded);
  }

  public MqttSn2CryptoProvider require(int scheme) {
    MqttSn2CryptoProvider result = schemes.get(scheme);
    if (result == null) throw new IllegalArgumentException("Unconfigured protection scheme: " + scheme);
    return result;
  }
}
