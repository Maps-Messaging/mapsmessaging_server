/*
 *    Copyright [ 2020 - 2022 ] [Matthew Buckton]
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *
 */

package io.mapsmessaging.engine.system;

import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.destination.subscription.Subscription;
import io.mapsmessaging.engine.schema.SchemaManager;
import io.mapsmessaging.utilities.service.Service;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

public abstract class SystemTopic extends DestinationImpl implements Service {

  private final List<SystemTopic> empty;

  protected SystemTopic(String name) throws IOException {
    super(name, DestinationType.TOPIC);
    empty = new ArrayList<>();
  }

  @Override
  public String getName() {
    return getFullyQualifiedNamespace();
  }

  @Override
  public String getDescription() {
    return "";
  }

  @Override
  public synchronized void addSubscription(@NonNull @NotNull Subscription subscription) {
    super.addSubscription(subscription);
    subscription.sendMessage(generateMessage());
  }

  @Override
  public synchronized int storeMessage(@NonNull @NotNull Message message) throws IOException {
    throw new IOException("Write to a system topic is prohibited");
  }

  public void start() {
    String schemaId = getSchemaUUID();
    if (schemaId != null) {
      try {
        super.updateSchema(SchemaManager.getInstance().getSchema(schemaId), null);
      } catch (IOException e) {
        // No need to worry about these since system topics are in memory only
      }
    }
  }

  public abstract String getSchemaUUID();

  public void sendUpdate() throws IOException {
    super.storeMessage(generateMessage());
  }

  protected Message generateMessage() {
    return getMessage("0".getBytes());
  }

  public boolean hasUpdates() {
    return false;
  }

  public String[] aliases() {
    return new String[0];
  }

  public List<SystemTopic> getChildren() {
    return empty;
  }

  protected Message getMessage(byte[] payload) {
    MessageBuilder builder = new MessageBuilder();
    builder.setOpaqueData(payload);
    return new Message(builder);
  }
}
