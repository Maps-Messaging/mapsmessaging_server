/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

/** Optional profile check for the released protection-bc provider artifact. */
class MqttSn2ProtectionArtifactTest {

  @Test
  void optionalBouncyCastleProviderLoadsAndAdvertisesCsd01Schemes() throws Exception {
    // CSD01 MQTT-SN-4.13 and MQTT-SN-2.1.3-1..3.
    Class<?> providerType;
    Class<?> resolverType;
    try {
      providerType = Class.forName(
          "io.mapsmessaging.mqttsn.protection.bc.BouncyCastleProtectionProvider");
      resolverType = Class.forName(
          "io.mapsmessaging.mqttsn.protection.bc.ProtectionKeyResolver");
    } catch (ClassNotFoundException absent) {
      Assumptions.assumeTrue(false,
          "Enable -DmqttSn2ProtectionBc=true to test the optional BC artifact");
      return;
    }

    Object resolver = Proxy.newProxyInstance(resolverType.getClassLoader(),
        new Class<?>[] {resolverType}, (proxy, method, arguments) -> {
          if (method.getReturnType() == byte[].class) {
            return new byte[32];
          }
          if (method.getReturnType() == boolean.class) {
            return false;
          }
          return null;
        });
    Object provider = providerType.getConstructor(resolverType).newInstance(resolver);
    assertTrue((boolean) providerType.getMethod("supports", int.class).invoke(provider, 0x00));
    assertTrue((boolean) providerType.getMethod("supports", int.class).invoke(provider, 0x40));
  }
}
