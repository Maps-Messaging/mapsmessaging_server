/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
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

package io.mapsmessaging.network.protocol.impl.proxy;

import io.mapsmessaging.network.io.Packet;

import java.lang.reflect.Constructor;
import java.nio.ByteBuffer;

final class TestPacketFactory {

  private TestPacketFactory() {
  }

  static Packet packetOf(byte[] bytes) {
    try {
      return tryConstruct(bytes);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("Unable to construct Packet for tests. Add a Packet ctor for ByteBuffer or byte[] (or update TestPacketFactory).", e);
    }
  }

  private static Packet tryConstruct(byte[] bytes) throws ReflectiveOperationException {
    Class<Packet> packetClass = Packet.class;

    // 1) Packet(ByteBuffer)
    for (Constructor<?> ctor : packetClass.getDeclaredConstructors()) {
      Class<?>[] params = ctor.getParameterTypes();
      if (params.length == 1 && ByteBuffer.class.isAssignableFrom(params[0])) {
        ctor.setAccessible(true);
        return (Packet) ctor.newInstance(ByteBuffer.wrap(bytes));
      }
    }

    // 2) Packet(byte[])
    for (Constructor<?> ctor : packetClass.getDeclaredConstructors()) {
      Class<?>[] params = ctor.getParameterTypes();
      if (params.length == 1 && params[0].isArray() && params[0].getComponentType() == byte.class) {
        ctor.setAccessible(true);
        return (Packet) ctor.newInstance((Object) bytes);
      }
    }

    // 3) Packet(byte[], int, int) or Packet(byte[], int, int, ...)
    for (Constructor<?> ctor : packetClass.getDeclaredConstructors()) {
      Class<?>[] params = ctor.getParameterTypes();
      if (params.length >= 3
          && params[0].isArray()
          && params[0].getComponentType() == byte.class
          && params[1] == int.class
          && params[2] == int.class) {
        ctor.setAccessible(true);

        Object[] args = new Object[params.length];
        args[0] = bytes;
        args[1] = 0;
        args[2] = bytes.length;
        for (int i = 3; i < params.length; i++) {
          if (params[i] == int.class) {
            args[i] = 0;
          } else if (params[i] == boolean.class) {
            args[i] = false;
          } else {
            args[i] = null;
          }
        }
        return (Packet) ctor.newInstance(args);
      }
    }

    throw new NoSuchMethodException("No suitable Packet constructor found for ByteBuffer/byte[] input.");
  }
}
