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

import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.state.config.CotConfigDTO;
import io.mapsmessaging.state.config.TwinManagerConfig;
import io.mapsmessaging.state.config.TwinManagerConfigDTO;
import io.mapsmessaging.state.drone.core.EntityTwin;
import io.mapsmessaging.state.drone.core.TwinLifecycleStatus;
import io.mapsmessaging.state.drone.core.TwinManager;
import io.mapsmessaging.state.drone.core.TwinObserver;
import io.mapsmessaging.state.drone.core.TwinRelationship;
import io.mapsmessaging.state.drone.core.TwinUpdateContext;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.drone.model.BatteryState;
import io.mapsmessaging.state.drone.model.DetectionEvent;
import io.mapsmessaging.state.drone.tak.model.TakEvent;
import io.mapsmessaging.state.logging.StateLogMessages;
import io.mapsmessaging.utilities.configuration.ConfigurationManager;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Twin observer that maps twin state into TAK CoT XML and publishes it to a topic.
 */
public class TakTwinObserver implements TwinObserver {

  private static final long PUBLISH_INTERVAL_MS = 1000L;
  private static final long STATS_PUBLISH_INTERVAL_MS = 30_000L;

  private final Logger logger = LoggerFactory.getLogger(TakTwinObserver.class);

  private final Map<String, TakTwinContext> takContexts;
  private final Map<String, Long> lastStatsPublishTimes;
  private final TwinManager twinManager;
  private final TakEventMapper takEventMapper;
  private final TakXmlSerialiser takXmlSerialiser;
  private final EventPublisher eventPublisher;
  private final CotConfigResolver cotConfigResolver;
  private final CotEventPolicy cotEventPolicy;
  private final CotIntegrationJMX cotIntegrationJMX;
  private final TakOutputJMX takOutputJMX;
  private final PictureRecoveryTracker pictureRecoveryTracker;
  private final List<PictureRecoveryJMX> pictureRecoveryJMXs;
  private final boolean namespaceFilteringEnabled;

  public TakTwinObserver(TwinManager twinManager) {
    this.twinManager = Objects.requireNonNull(twinManager, "twinManager cannot be null");
    this.takContexts = new ConcurrentHashMap<>();
    this.lastStatsPublishTimes = new ConcurrentHashMap<>();
    this.takEventMapper = new TakEventMapper();
    this.takXmlSerialiser = new TakXmlSerialiser();
    this.cotEventPolicy = new CotEventPolicy();
    this.cotIntegrationJMX = new CotIntegrationJMX(cotEventPolicy);

    TwinManagerConfigDTO config = ConfigurationManager.getInstance().getConfiguration(TwinManagerConfig.class);
    List<CotConfigDTO> cotConfigs = config == null ? List.of() : config.getCot();
    this.cotConfigResolver = new CotConfigResolver(cotConfigs);
    this.namespaceFilteringEnabled = !cotConfigs.isEmpty();

    if (config != null && config.getTak() != null) {
      if (config.getTak().getTopic() != null && !config.getTak().getTopic().isBlank()) {
        EventPublisher publisher;
        try {
          publisher = new EventPublisher(config.getTak().getTopic());
        } catch (InterruptedException exception) {
          Thread.currentThread().interrupt();
          publisher = null;
          logger.log(StateLogMessages.STATE_MANAGER_TAK_PUBLISHER_CREATE_FAILED, config.getTak().getTopic(), exception);
        } catch (Throwable exception) {
          publisher = null;
          logger.log(StateLogMessages.STATE_MANAGER_TAK_PUBLISHER_CREATE_FAILED, config.getTak().getTopic(), exception);
        }
        eventPublisher = publisher;
      } else {
        eventPublisher = null;
      }
      twinManager.addObserver(this);
    } else {
      this.eventPublisher = null;
    }
    this.takOutputJMX = new TakOutputJMX(eventPublisher);
    this.pictureRecoveryTracker = new PictureRecoveryTracker(Clock.systemUTC());
    this.pictureRecoveryJMXs = new ArrayList<>();
    for (PictureRecoveryTracker.FailureType failureType : PictureRecoveryTracker.FailureType.values()) {
      for (PictureRecoveryTracker.Stage stage : PictureRecoveryTracker.Stage.values()) {
        pictureRecoveryJMXs.add(new PictureRecoveryJMX(pictureRecoveryTracker, failureType, stage));
      }
    }
    MtiStatusRegistry.addStatusListener(pictureRecoveryTracker);
  }

  public void shutdown() {
    twinManager.removeObserver(this);
    cotIntegrationJMX.close();
    takOutputJMX.close();
    MtiStatusRegistry.removeStatusListener(pictureRecoveryTracker);
    for (PictureRecoveryJMX pictureRecoveryJMX : pictureRecoveryJMXs) {
      pictureRecoveryJMX.close();
    }
    pictureRecoveryJMXs.clear();
    takContexts.clear();
    lastStatsPublishTimes.clear();

    if (eventPublisher != null) {
      try {
        eventPublisher.close();
      } catch (IOException exception) {
        logger.log(StateLogMessages.STATE_MANAGER_COMPONENT_STOP_FAILED, EventPublisher.class.getName(), exception);
      }
    }
  }

  @Override
  public void onTwinAdded(EntityTwin twin, TwinUpdateContext context) {
    TakTwinContext twinContext = takContexts.computeIfAbsent(twin.getTwinId(), key -> new TakTwinContext());
    twinContext.setLastUpdate(System.currentTimeMillis());
    pictureRecoveryTracker.onTwinAdded(twin.getTwinId(), receivedTime(context));
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
    if (removed == null || removed.getTwinId() == null) {
      return;
    }

    TakTwinContext twinContext = takContexts.get(removed.getTwinId());
    if (twinContext != null) {
      publishRemoval(removed, context, twinContext);
    }

    takContexts.remove(removed.getTwinId());
    lastStatsPublishTimes.remove(removed.getTwinId());
    pictureRecoveryTracker.onTwinRemoved(removed.getTwinId());
  }

  @Override
  public void onDetectionEvent(DroneTwin source, DetectionEvent event, TwinUpdateContext context) {
    if (source == null || source.getTwinId() == null || source.getTwinId().isBlank()) {
      return;
    }

    TakTwinContext twinContext =
        takContexts.computeIfAbsent(source.getTwinId(), key -> new TakTwinContext());

    CotConfigDTO cotConfig = resolveCotConfig(context, twinContext);
    if (namespaceFilteringEnabled && cotConfig == null) {
      return;
    }

    TakEvent takEvent = takEventMapper.mapDetection(source, event, context);
    if (takEvent == null) {
      return;
    }
    cotEventPolicy.applyDetection(takEvent, source, cotConfig);

    publishXml(takXmlSerialiser.toXml(takEvent));
  }

  @Override
  public void onRelationshipUpdated(String twinId, TwinRelationship relationship, TwinUpdateContext context) {
    // ignored for now
  }

  @Override
  public void onTwinStatusChanged(String twinId, TwinLifecycleStatus previousStatus, TwinLifecycleStatus currentStatus, EntityTwin twin, TwinUpdateContext context) {

    if (twin == null) {
      return;
    }
    String resolvedTwinId = twinId == null || twinId.isBlank() ? twin.getTwinId() : twinId;

    if (resolvedTwinId == null || resolvedTwinId.isBlank()) {
      return;
    }

    TakTwinContext twinContext = takContexts.computeIfAbsent(resolvedTwinId, key -> new TakTwinContext());

    pictureRecoveryTracker.onTwinStatusChanged(resolvedTwinId, previousStatus, currentStatus, receivedTime(context));
    publishTwin(twin, context, twinContext);
  }

  private void publishTwin(EntityTwin twin, TwinUpdateContext context, TakTwinContext twinContext) {

    if (twin == null || twin.getGeoPosition() == null) {
      return;
    }

    CotConfigDTO cotConfig = resolveCotConfig(context, twinContext);
    if (namespaceFilteringEnabled && cotConfig == null) {
      return;
    }

    TakEvent takEvent = takEventMapper.map(twin, context);
    if (takEvent == null) {
      return;
    }
    TakEvent cyberIconEvent = cotEventPolicy.apply(takEvent, twin, context, cotConfig);

    String xml = takXmlSerialiser.toXml(takEvent);
    xml = appendStatsIfDue(twin, xml);

    if (context != null && context.getReceivedTime() != null) {
      TakOutputStats.recordLatencyMillis(System.currentTimeMillis() - context.getReceivedTime().toEpochMilli());
    }

    boolean handedToTak = publishXml(xml);

    if (cyberIconEvent != null) {
      publishXml(takXmlSerialiser.toXml(cyberIconEvent));
      twinContext.setCyberIconPublished(true);
    } else if (twinContext.isCyberIconPublished()) {
      TakEvent cyberIconRemoval = cotEventPolicy.buildCyberIconRemoval(takEvent);
      if (cyberIconRemoval != null) {
        publishXml(takXmlSerialiser.toXml(cyberIconRemoval));
      }
      twinContext.setCyberIconPublished(false);
    }

    if (handedToTak) {
      pictureRecoveryTracker.onCotHandedToTak(
          twin.getTwinId(), MtiStatusRegistry.snapshot(twin.getTwinId()) != null);
    }
  }

  /** @return true if the XML was queued for publishing to the CoT topic. */
  private boolean publishXml(String xml) {
    boolean handedToTak = false;
    if (eventPublisher != null) {
      try {
        eventPublisher.publish(xml);
        handedToTak = true;
      } catch (IOException exception) {
        logger.log(StateLogMessages.STATE_MANAGER_TAK_OBSERVER_PUBLISH_FAILED, exception);
      }
    }
    return handedToTak;
  }

  private static Instant receivedTime(TwinUpdateContext context) {
    return context == null ? null : context.getReceivedTime();
  }

  private void publishRemoval(EntityTwin twin, TwinUpdateContext context, TakTwinContext twinContext) {

    CotConfigDTO cotConfig = resolveCotConfig(context, twinContext);
    if (namespaceFilteringEnabled && cotConfig == null) {
      return;
    }

    TakEvent takEvent = takEventMapper.mapRemoval(twin, context);
    if (takEvent == null) {
      return;
    }
    cotEventPolicy.applyRemoval(takEvent, twin, context, cotConfig);

    publishXml(takXmlSerialiser.toXml(takEvent));

    if (twinContext.isCyberIconPublished()) {
      TakEvent cyberIconRemoval = cotEventPolicy.buildCyberIconRemoval(takEvent);
      if (cyberIconRemoval != null) {
        publishXml(takXmlSerialiser.toXml(cyberIconRemoval));
      }
      twinContext.setCyberIconPublished(false);
    }
  }

  private CotConfigDTO resolveCotConfig(TwinUpdateContext context, TakTwinContext twinContext) {
    if (context != null && context.getSourceNamespace() != null && !context.getSourceNamespace().isBlank()) {
      CotConfigDTO match = cotConfigResolver.resolve(context.getSourceNamespace());
      if (match != null) {
        twinContext.setCotConfig(match);
      }
      return match;
    }

    if (twinContext.getCotConfig() != null) {
      return twinContext.getCotConfig();
    }

    if (context != null && context.getUpdateSource() != null && !context.getUpdateSource().isBlank()) {
      CotConfigDTO match = cotConfigResolver.resolve(context.getUpdateSource());
      if (match != null) {
        twinContext.setCotConfig(match);
        return match;
      }
    }

    return null;
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
      int batteryPercentage = (int) Math.round(Math.clamp(percentage, 0.0, 100.0));
      appendAttribute(stringBuilder, "battery", batteryPercentage);
    }

    Double temperatureCelsius = batteryState.getTemperatureCelsius();
    if (temperatureCelsius != null && Double.isFinite(temperatureCelsius)) {
      appendAttribute(stringBuilder, "battery_temp", (int) Math.round(temperatureCelsius));
    }

    Boolean charging = batteryState.getCharging();
    if (charging != null) {
      appendAttribute(stringBuilder, "battery_status", charging ? "Charging" : "Discharging");
    }

    if (stringBuilder.length() == initialLength) {
      return null;
    }

    stringBuilder.append("/>");
    return stringBuilder.toString();
  }

  private void appendAttribute(StringBuilder stringBuilder, String name, Object value) {
    stringBuilder.append(' ').append(name).append("=\"").append(value).append('"');
  }
}
