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

package io.mapsmessaging.api.transformers;

import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.network.protocol.Protocol;

import java.nio.charset.StandardCharsets;

import static org.mockito.Mockito.*;

public final class TransformationTestSupport {

  private TransformationTestSupport() {
  }

  public static Protocol.ParsedMessage parsedMessage(String destinationName, Message message) {
    Protocol.ParsedMessage parsedMessage = new Protocol.ParsedMessage();
    parsedMessage.setDestinationName(destinationName);
    parsedMessage.setMessage(message);
    return parsedMessage;
  }

  public static Message mockMessage(byte[] opaqueData) {
    Message message = mock(Message.class, withSettings().lenient());
    when(message.getOpaqueData()).thenReturn(opaqueData);
    return message;
  }

  public static Message mockMessage(byte[] opaqueData, String schemaId) {
    Message message = mockMessage(opaqueData);
    when(message.getSchemaId()).thenReturn(schemaId);
    return message;
  }

  public static byte[] utf8Bytes(String value) {
    if (value == null) {
      return null;
    }
    return value.getBytes(StandardCharsets.UTF_8);
  }

  public static String utf8String(byte[] value) {
    if (value == null) {
      return null;
    }
    return new String(value, StandardCharsets.UTF_8);
  }

  public static ConfigurationProperties emptyProperties() {
    return new ConfigurationProperties();
  }
}
