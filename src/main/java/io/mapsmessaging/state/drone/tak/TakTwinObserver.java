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
 *
 */

package io.mapsmessaging.state.drone.tak;

import io.mapsmessaging.config.TwinManagerConfig;
import io.mapsmessaging.dto.rest.config.TwinManagerConfigDTO;
import io.mapsmessaging.state.drone.core.EntityTwin;
import io.mapsmessaging.state.drone.core.TwinLifecycleStatus;
import io.mapsmessaging.state.drone.core.TwinManager;
import io.mapsmessaging.state.drone.core.TwinObserver;
import io.mapsmessaging.state.drone.core.TwinRelationship;
import io.mapsmessaging.state.drone.core.TwinUpdateContext;
import io.mapsmessaging.state.drone.tak.model.TakEvent;
import io.mapsmessaging.utilities.configuration.ConfigurationManager;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Twin observer that maps twin state into TAK CoT XML and publishes it to a topic.
 */
public class TakTwinObserver implements TwinObserver {

  private static final long PUBLISH_INTERVAL_MS = 1000L;

  private final Map<String, TakTwinContext> takContexts;
  private final String takHost;
  private final int takPort;
  private final TwinManager twinManager;
  private final TakEventMapper takEventMapper;
  private final TakXmlSerialiser takXmlSerialiser;
  private final TakSocketConnection globalSocketConnection;
  private final EventPublisher eventPublisher;

  public TakTwinObserver(TwinManager twinManager) {
    this.twinManager = Objects.requireNonNull(twinManager, "twinManager cannot be null");
    this.takContexts = new ConcurrentHashMap<>();
    this.takEventMapper = new TakEventMapper();
    this.takXmlSerialiser = new TakXmlSerialiser();
    TwinManagerConfigDTO config = ConfigurationManager.getInstance().getConfiguration(TwinManagerConfig.class);
    if (config != null && config.getTak() != null) {
      this.takHost = config.getTak().getHostname();
      this.takPort = config.getTak().getPort();
      if(config.getTak().isSharedConnection() && takHost != null && !takHost.isBlank() && takPort > 0){
        globalSocketConnection = new TakSocketConnection(takHost, takPort);
      }
      else{
        globalSocketConnection = null;
      }
      if(config.getTak().getTopic() != null && !config.getTak().getTopic().isBlank()){
        EventPublisher e = null;
        try {
          e = new EventPublisher(config.getTak().getTopic());
        } catch (Throwable ex) {
          e = null;
          ex.printStackTrace();
        }
        eventPublisher = e;
      }
      else{
        eventPublisher = null;
      }
      twinManager.addObserver(this);
    }
    else {
      this.takHost = null;
      this.takPort = 0;
      globalSocketConnection = null;
      eventPublisher = null;
    }
  }

  public void shutdown() {
    twinManager.removeObserver(this);
    for (TakTwinContext context : takContexts.values()) {
      if(globalSocketConnection == null) {
        if (context.getSocketConnection() != null) {
          context.getSocketConnection().close();
        }
      }
      else{
        globalSocketConnection.close();
      }
    }
    takContexts.clear();
    if(eventPublisher != null){
      try {
        eventPublisher.close();
      } catch (IOException e) {

      }
    }
  }

  @Override
  public void onTwinAdded(EntityTwin twin, TwinUpdateContext context) {
    TakTwinContext twinContext = takContexts.computeIfAbsent(twin.getTwinId(), key -> new TakTwinContext());
    publishTwin(twin, context, twinContext);
  }

  @Override
  public void onTwinUpdated(String twinId, EntityTwin current, TwinUpdateContext context) {
    if (current == null) {
      return;
    }

    String resolvedTwinId = twinId;
    if (resolvedTwinId == null || resolvedTwinId.isBlank()) {
      resolvedTwinId = current.getTwinId();
    }

    if (resolvedTwinId == null || resolvedTwinId.isBlank()) {
      return;
    }

    long now = System.currentTimeMillis();
    TakTwinContext twinContext = takContexts.computeIfAbsent(resolvedTwinId, key -> new TakTwinContext());
    if (twinContext.getLastUpdate() + PUBLISH_INTERVAL_MS > now) {
      return;
    }

    twinContext.setLastUpdate(now);
    publishTwin(current, context, twinContext);
  }

  @Override
  public void onTwinRemoved(EntityTwin removed, TwinUpdateContext context) {
    TakTwinContext twinContext = takContexts.get(removed.getTwinId());
    if (twinContext != null) {
      publishRemoval(removed, context, twinContext);
    }

    takContexts.remove(removed.getTwinId());
    if (twinContext != null && twinContext.getSocketConnection() != null && globalSocketConnection == null) {
      twinContext.getSocketConnection().close();
    }
  }

  @Override
  public void onRelationshipUpdated(String twinId, TwinRelationship relationship, TwinUpdateContext context) {
    // ignored for now
  }

  @Override
  public void onTwinStatusChanged(String twinId,
                                  TwinLifecycleStatus previousStatus,
                                  TwinLifecycleStatus currentStatus,
                                  EntityTwin twin,
                                  TwinUpdateContext context) {
    TakTwinContext twinContext = takContexts.computeIfAbsent(twinId, key -> new TakTwinContext());
    publishTwin(twin, context, twinContext);
  }

  private void publishTwin(EntityTwin twin, TwinUpdateContext context, TakTwinContext twinContext) {
    if (twin == null || twin.getGeoPosition() == null) {
      return;
    }

    TakEvent takEvent = takEventMapper.map(twin, context);
    if (takEvent == null) {
      return;
    }
    String xml = takXmlSerialiser.toXml(takEvent);

    if(takHost != null) {
      if (twinContext.getSocketConnection() == null) {
        twinContext.setSocketConnection(Objects.requireNonNullElseGet(globalSocketConnection, () -> new TakSocketConnection(takHost, takPort)));
      }
      twinContext.getSocketConnection().accept(xml);
    }
    if(eventPublisher != null) {
      try {
        eventPublisher.publish(xml);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

  }

  private void publishRemoval(EntityTwin twin, TwinUpdateContext context, TakTwinContext twinContext) {
    if (twin == null || twinContext.getSocketConnection() == null) {
      return;
    }

    TakEvent takEvent = takEventMapper.mapRemoval(twin, context);
    if (takEvent == null) {
      return;
    }

    twinContext.getSocketConnection().accept(takXmlSerialiser.toXml(takEvent));
  }
}