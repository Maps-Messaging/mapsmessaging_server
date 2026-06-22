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

package io.mapsmessaging.state.stanag;

import com.google.gson.Gson;
import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.api.Destination;
import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.engine.schema.SchemaManager;
import io.mapsmessaging.state.GsonStanagHelper;
import io.mapsmessaging.state.config.StanagConfig;
import io.mapsmessaging.state.drone.core.EntityTwin;
import io.mapsmessaging.state.drone.core.TwinObserver;
import io.mapsmessaging.state.drone.core.TwinUpdateContext;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.drone.model.Contact;
import io.mapsmessaging.state.stanag.messages.core.MessageHeaderBuilder;
import io.mapsmessaging.state.stanag.messages.node.common.NodeMessageSupport;
import io.mapsmessaging.state.stanag.messages.node.description.NodeDescriptionBuilder;
import io.mapsmessaging.state.stanag.messages.node.dynamic.DynamicUpdate;
import io.mapsmessaging.state.stanag.messages.node.dynamic.DynamicUpdateBuilder;
import io.mapsmessaging.state.stanag.messages.node.status.NodeStatusBuilder;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class NodeUpdate implements TwinObserver {

  private final Session session;
  private final Map<String, TwinStatus> statusMap;
  private final long descriptionInterval;
  private final long statusInterval;

  private final String topicTemplate;
  private final NodeDescriptionBuilder nodeDescriptionBuilder;
  private final NodeStatusBuilder nodeStatusBuilder;
  private final DynamicUpdateBuilder dynamicUpdateBuilder;
  private final Gson gson;

  public NodeUpdate(Session session, StanagConfig stanagConfig) {
    this.session = session;
    statusMap = new ConcurrentHashMap<>();
    this.descriptionInterval = stanagConfig.getDescriptionIntervalSec() * 1000L;
    this.statusInterval = stanagConfig.getStatusIntervalSec() * 1000L;
    this.topicTemplate = stanagConfig.getTaskTopicTemplate();
    MessageHeaderBuilder messageHeaderBuilder = new MessageHeaderBuilder(Clock.systemUTC());
    NodeMessageSupport nodeMessageSupport = new NodeMessageSupport();
    nodeStatusBuilder = new NodeStatusBuilder(messageHeaderBuilder, nodeMessageSupport);
    nodeDescriptionBuilder = new NodeDescriptionBuilder(messageHeaderBuilder, nodeMessageSupport);
    dynamicUpdateBuilder = new DynamicUpdateBuilder(messageHeaderBuilder, nodeMessageSupport, MessageDaemon.getInstance().getUuid());
    gson = GsonStanagHelper.createGson();
  }

  @Override
  public void onTwinAdded(EntityTwin twin, TwinUpdateContext context) {
    TwinStatus status = statusMap.get(twin.getTwinId());
    if(status == null) {
      statusMap.put(twin.getTwinId(), new TwinStatus());
    }
    else{
      status.setLastStatusUpdate(System.currentTimeMillis());
      status.setLastDescriptionUpdate(0);
    }
  }

  @Override
  public void onTwinUpdated(String twinId, EntityTwin current, TwinUpdateContext context) {
    TwinStatus status = statusMap.get(twinId);
    long currentTime = System.currentTimeMillis();
    if(status != null) {
      if(currentTime - status.getLastDescriptionUpdate() > descriptionInterval) {
        if(current.getGeoPosition() != null && current.getGeoPosition().getLatitude() != null && current.getGeoPosition().getLongitude() != null) {
          sendDescription(status, current);
          status.setLastDescriptionUpdate(currentTime);
          status.setSentDescription(true);
        }
      }
      else if(currentTime - status.getLastStatusUpdate() > statusInterval && status.isSentDescription()) {
        sendStatus(status, current);
        status.setLastStatusUpdate(currentTime);
        List<Contact> contactList = ((DroneTwin)current).getContactList();
        if(contactList != null && !contactList.isEmpty()) {
          sendContacts((DroneTwin)current, status, contactList);
        }
      }
    }
    else{
      onTwinAdded(current, context);
    }
  }

  private void sendContacts(DroneTwin droneTwin, TwinStatus status, List<Contact> contactList) {
    for (Contact contact : contactList) {
      Optional<DynamicUpdate> dynamicUpdate = dynamicUpdateBuilder.build(droneTwin, contact);
      if (dynamicUpdate.isPresent()) {
        String json = gson.toJson(dynamicUpdate.get());
        String topicName = computeTopic(droneTwin.getUuid().toString(), "MessageTypeEnum_DYNAMIC_UPDATE");
        publish(status, topicName, json);
      }
    }
  }

  private void sendStatus(TwinStatus status, EntityTwin current) {
    String jsonStatus = gson.toJsonTree(nodeStatusBuilder.build((DroneTwin) current)).getAsJsonObject().toString();
    String topic = computeTopic(current.getUuid().toString(), "MessageTypeEnum_NODE_STATUS");
    publish(status, topic, jsonStatus);
  }

  private void sendDescription(TwinStatus status, EntityTwin current) {
    String jsonStatus = gson.toJsonTree(nodeDescriptionBuilder.build((DroneTwin) current)).getAsJsonObject().toString();
    String topic = computeTopic(current.getUuid().toString(), "MessageTypeEnum_NODE_DESCRIPTION");
    publish(status, topic, jsonStatus);
  }

  private void publish(TwinStatus status, String topic, String json) {
    try {
      Destination destination = status.destinationMap.get(topic);
      if(destination == null){
        destination = findDestination(topic);
        status.destinationMap.put(topic, destination);
      }
      MessageBuilder messageBuilder = new MessageBuilder();
      messageBuilder.setOpaqueData(json.getBytes(StandardCharsets.UTF_8))
          .setQoS(QualityOfService.AT_MOST_ONCE)
          .setSchemaId(SchemaManager.DEFAULT_JSON_SCHEMA.toString());
      destination.storeMessage(messageBuilder.build());
    } catch (IOException e) {
      // log this
    }
  }


  private Destination findDestination(String topicName)  throws IOException{
    try {
      return session.findDestination(topicName, DestinationType.TOPIC).get(30, TimeUnit.SECONDS);
    } catch (InterruptedException | ExecutionException | TimeoutException e) {
      throw new IOException(e);
    }
  }

  @Override
  public void onTwinRemoved(EntityTwin removed, TwinUpdateContext context) {
    TwinStatus status = statusMap.remove(removed.getTwinId());
    status.close();
  }


  private String computeTopic(String twinId, String messageEnum) {
    String topic = topicTemplate.replace("{twinId}", twinId);
    topic = topic.replace("{messageEnumName}", messageEnum);
    return topic;
  }

  @Getter
  @Setter
  private static class TwinStatus {
    private long lastStatusUpdate = System.currentTimeMillis();
    private long lastDescriptionUpdate = 0;
    private String topicPath = "topic";
    private boolean sentDescription = false;
    private Map<String, Destination> destinationMap = new ConcurrentHashMap<>();

    public void close(){
      destinationMap.clear();
    }

  }
}
