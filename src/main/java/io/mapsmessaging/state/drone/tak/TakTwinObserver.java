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

package io.mapsmessaging.state.drone.tak;

import io.mapsmessaging.state.config.TwinManagerConfig;
import io.mapsmessaging.state.config.TwinManagerConfigDTO;
import io.mapsmessaging.state.drone.core.EntityTwin;
import io.mapsmessaging.state.drone.core.TwinLifecycleStatus;
import io.mapsmessaging.state.drone.core.TwinManager;
import io.mapsmessaging.state.drone.core.TwinObserver;
import io.mapsmessaging.state.drone.core.TwinRelationship;
import io.mapsmessaging.state.drone.core.TwinUpdateContext;
import io.mapsmessaging.state.drone.model.BatteryState;
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
  private static final long STATS_PUBLISH_INTERVAL_MS = 30_000L;

  private final Map<String, TakTwinContext> takContexts;
  private final Map<String, Long> lastStatsPublishTimes;
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
    this.lastStatsPublishTimes = new ConcurrentHashMap<>();
    this.takEventMapper = new TakEventMapper();
    this.takXmlSerialiser = new TakXmlSerialiser();

    TwinManagerConfigDTO config =
        ConfigurationManager.getInstance().getConfiguration(TwinManagerConfig.class);

    if (config != null && config.getTak() != null) {
      this.takHost = config.getTak().getHostname();
      this.takPort = config.getTak().getPort();

      if (config.getTak().isSharedConnection()
          && takHost != null
          && !takHost.isBlank()
          && takPort > 0) {
        globalSocketConnection = new TakSocketConnection(takHost, takPort);
      } else {
        globalSocketConnection = null;
      }

      if (config.getTak().getTopic() != null && !config.getTak().getTopic().isBlank()) {
        EventPublisher publisher;
        try {
          publisher = new EventPublisher(config.getTak().getTopic());
        } catch (Throwable exception) {
          publisher = null;
          exception.printStackTrace();
        }
        eventPublisher = publisher;
      } else {
        eventPublisher = null;
      }

      twinManager.addObserver(this);
    } else {
      this.takHost = null;
      this.takPort = 0;
      this.globalSocketConnection = null;
      this.eventPublisher = null;
    }
  }

  public void shutdown() {
    twinManager.removeObserver(this);

    if (globalSocketConnection != null) {
      globalSocketConnection.close();
    } else {
      for (TakTwinContext context : takContexts.values()) {
        if (context.getSocketConnection() != null) {
          context.getSocketConnection().close();
        }
      }
    }

    takContexts.clear();
    lastStatsPublishTimes.clear();

    if (eventPublisher != null) {
      try {
        eventPublisher.close();
      } catch (IOException ignored) {
      }
    }
  }

  @Override
  public void onTwinAdded(EntityTwin twin, TwinUpdateContext context) {
    TakTwinContext twinContext =
        takContexts.computeIfAbsent(twin.getTwinId(), key -> new TakTwinContext());

    twinContext.setLastUpdate(System.currentTimeMillis());
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
    TakTwinContext twinContext =
        takContexts.computeIfAbsent(resolvedTwinId, key -> new TakTwinContext());

    if (twinContext.getLastUpdate() + PUBLISH_INTERVAL_MS > now) {
      return;
    }

    twinContext.setLastUpdate(now);
    publishTwin(current, context, twinContext);
  }

  @Override
  public void onTwinRemoved(EntityTwin removed, TwinUpdateContext context) {
    if (removed == null || removed.getTwinId() == null) {
      return;
    }

    TakTwinContext twinContext = takContexts.get(removed.getTwinId());
    if (twinContext != null) {
      publishRemoval(removed, context, twinContext);
    }

    takContexts.remove(removed.getTwinId());
    lastStatsPublishTimes.remove(removed.getTwinId());

    if (twinContext != null
        && twinContext.getSocketConnection() != null
        && globalSocketConnection == null) {
      twinContext.getSocketConnection().close();
    }
  }

  @Override
  public void onRelationshipUpdated(
      String twinId, TwinRelationship relationship, TwinUpdateContext context) {
    // ignored for now
  }

  @Override
  public void onTwinStatusChanged(
      String twinId,
      TwinLifecycleStatus previousStatus,
      TwinLifecycleStatus currentStatus,
      EntityTwin twin,
      TwinUpdateContext context) {

    if (twin == null) {
      return;
    }

    String resolvedTwinId =
        twinId == null || twinId.isBlank() ? twin.getTwinId() : twinId;

    if (resolvedTwinId == null || resolvedTwinId.isBlank()) {
      return;
    }

    TakTwinContext twinContext =
        takContexts.computeIfAbsent(resolvedTwinId, key -> new TakTwinContext());

    publishTwin(twin, context, twinContext);
  }

  private void publishTwin(
      EntityTwin twin, TwinUpdateContext context, TakTwinContext twinContext) {

    if (twin == null || twin.getGeoPosition() == null) {
      return;
    }

    TakEvent takEvent = takEventMapper.map(twin, context);
    if (takEvent == null) {
      return;
    }

    String xml = takXmlSerialiser.toXml(takEvent);
    xml = appendStatsIfDue(twin, xml);

    if (takHost != null && !takHost.isBlank() && takPort > 0) {
      if (twinContext.getSocketConnection() == null) {
        twinContext.setSocketConnection(
            Objects.requireNonNullElseGet(
                globalSocketConnection, () -> new TakSocketConnection(takHost, takPort)));
      }

      twinContext.getSocketConnection().accept(xml);
    }

    if (eventPublisher != null) {
      try {
        eventPublisher.publish(xml);
      } catch (IOException exception) {
        exception.printStackTrace();
      }
    }
  }

  private void publishRemoval(
      EntityTwin twin, TwinUpdateContext context, TakTwinContext twinContext) {

    if (twin == null || twinContext.getSocketConnection() == null) {
      return;
    }

    TakEvent takEvent = takEventMapper.mapRemoval(twin, context);
    if (takEvent == null) {
      return;
    }

    twinContext.getSocketConnection().accept(takXmlSerialiser.toXml(takEvent));
  }

  private String appendStatsIfDue(EntityTwin twin, String xml) {
    String twinId = twin.getTwinId();
    if (twinId == null || twinId.isBlank() || xml == null || xml.isBlank()) {
      return xml;
    }

    BatteryState batteryState = twin.getBatteryState();
    String stats = buildStats(batteryState);
    if (stats == null) {
      return xml;
    }

    long now = System.currentTimeMillis();
    long lastPublish = lastStatsPublishTimes.getOrDefault(twinId, 0L);
    if (lastPublish + STATS_PUBLISH_INTERVAL_MS > now) {
      return xml;
    }

    int detailEnd = xml.lastIndexOf("</detail>");
    if (detailEnd < 0) {
      return xml;
    }

    lastStatsPublishTimes.put(twinId, now);

    return xml.substring(0, detailEnd)
        + stats
        + xml.substring(detailEnd);
  }

  private String buildStats(BatteryState batteryState) {
    if (batteryState == null) {
      return null;
    }

    StringBuilder stringBuilder = new StringBuilder(96);
    stringBuilder.append("<stats");

    int initialLength = stringBuilder.length();

    Double percentage = batteryState.getPercentage();
    if (percentage != null && Double.isFinite(percentage)) {
      int batteryPercentage =
          (int) Math.round(Math.max(0.0, Math.min(100.0, percentage)));

      appendAttribute(stringBuilder, "battery", batteryPercentage);
    }

    Double temperatureCelsius = batteryState.getTemperatureCelsius();
    if (temperatureCelsius != null && Double.isFinite(temperatureCelsius)) {
      appendAttribute(
          stringBuilder,
          "battery_temp",
          (int) Math.round(temperatureCelsius));
    }

    Boolean charging = batteryState.getCharging();
    if (charging != null) {
      appendAttribute(
          stringBuilder,
          "battery_status",
          charging ? "Charging" : "Discharging");
    }

    if (stringBuilder.length() == initialLength) {
      return null;
    }

    stringBuilder.append("/>");
    return stringBuilder.toString();
  }

  private void appendAttribute(
      StringBuilder stringBuilder, String name, Object value) {

    stringBuilder
        .append(' ')
        .append(name)
        .append("=\"")
        .append(value)
        .append('"');
  }
}