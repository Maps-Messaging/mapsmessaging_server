/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
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

package io.mapsmessaging.api.message;

import io.mapsmessaging.storage.StorableFactory;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;

public class MessageFactory implements StorableFactory<Message> {

  private static MessageFactory instance;

  static {
    instance = new MessageFactory();
  }

  public static MessageFactory getInstance() {
    return instance;
  }

  @Override
  public @NotNull Message unpack(@NotNull ByteBuffer[] byteBuffers) throws IOException {
    return new Message(byteBuffers);
  }

  @Override
  public @NotNull ByteBuffer[] pack(@NotNull Message message) throws IOException {
    return message.pack();
  }

  public @NotNull ByteBuffer[] pack(@NotNull Message message, Map<String, String> updatedMeta) throws IOException {
    return message.pack(updatedMeta);
  }
}
