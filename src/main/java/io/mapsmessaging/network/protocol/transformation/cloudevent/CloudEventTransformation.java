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

package io.mapsmessaging.network.protocol.transformation.cloudevent;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.engine.schema.SchemaManager;
import io.mapsmessaging.network.protocol.ProtocolMessageTransformation;
import io.mapsmessaging.schemas.config.SchemaConfig;
import lombok.NonNull;

import org.jetbrains.annotations.NotNull;

public class CloudEventTransformation implements ProtocolMessageTransformation {

  public CloudEventTransformation() {
    // Used by the java services
  }

  @Override
  public String getName() {
    return "CloudEvent";
  }

  @Override
  public int getId() {
    return 7;
  }

  @Override
  public String getDescription() {
    return "CloudEvent V1.0 support as specified in https://cloudevents.io/";
  }

  @Override
  public void incoming(@NonNull @NotNull MessageBuilder messageBuilder) {
    // No Op at present
  }

  @Override
  public @NonNull byte[] outgoing(@NonNull @NotNull Message message, String destinationName) {
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    return CloudEventHelper.toCloudEvent(message, "", "", gson);
  }
}