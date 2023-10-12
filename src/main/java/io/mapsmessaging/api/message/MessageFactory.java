/*
 * Copyright [ 2020 - 2023 ] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.mapsmessaging.api.message;

import io.mapsmessaging.storage.StorableFactory;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.ByteBuffer;

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
}
