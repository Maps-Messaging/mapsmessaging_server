/*
 *
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      https://commonsclause.com/
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import org.junit.jupiter.api.Test;

/** Verifies the protection-bc test dependency is available on every test run. */
class MqttSn2ProtectionArtifactTest {

  @Test
  void bouncyCastleProviderLoadsAndAdvertisesCsd01Schemes() throws Exception {
    // CSD01 MQTT-SN-4.13 and MQTT-SN-2.1.3-1..3.
    Class<?> providerType = Class.forName(
        "io.mapsmessaging.mqttsn.protection.bc.BouncyCastleProtectionProvider");
    Class<?> resolverType = Class.forName(
        "io.mapsmessaging.mqttsn.protection.bc.ProtectionKeyResolver");

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
