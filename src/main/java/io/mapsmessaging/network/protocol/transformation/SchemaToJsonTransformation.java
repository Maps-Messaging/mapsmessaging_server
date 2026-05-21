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

package io.mapsmessaging.network.protocol.transformation;

import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.engine.schema.MessageSchemaToJsonBuilder;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;

import static io.mapsmessaging.logging.ServerLogMessages.MESSAGE_TRANSFORMATION_EXCEPTION;

public class SchemaToJsonTransformation implements ProtocolMessageTransformation {

  private final Logger logger = LoggerFactory.getLogger(getClass());
  private final MessageSchemaToJsonBuilder messageSchemaToJsonBuilder;

  public SchemaToJsonTransformation() {
    messageSchemaToJsonBuilder = new MessageSchemaToJsonBuilder();
  }

  @Override
  public String getName() {
    return "Schema-To-Json";
  }

  @Override
  public String getDescription() {
    return "Transforms outgoing messages into a JSON object if there is a corresponding schema";
  }

  @Override
  public int getId() {
    return 4;
  }

  @Override
  public Message outgoing(Message message, String destinationName) {
    try {
      return messageSchemaToJsonBuilder.parse(message, destinationName);
    } catch (Exception e) {
      logger.log(MESSAGE_TRANSFORMATION_EXCEPTION, e);
    }
    return null;
  }
}
