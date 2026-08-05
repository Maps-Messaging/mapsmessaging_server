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

package io.mapsmessaging.state;


import io.mapsmessaging.configuration.EnvironmentConfig;
import io.mapsmessaging.dto.rest.config.protocol.impl.TakProtocolDTO;
import io.mapsmessaging.dto.rest.system.Status;
import io.mapsmessaging.dto.rest.system.SubSystemStatusDTO;
import io.mapsmessaging.geospatial.GeoSpatialAreaRegistry;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.state.adapter.StateMessageAdapterContext;
import io.mapsmessaging.state.adapter.StateMessageAdapterFactory;
import io.mapsmessaging.state.auditor.AuditorFactory;
import io.mapsmessaging.state.auditor.StateAuditContext;
import io.mapsmessaging.state.config.DroneInfoRegistry;
import io.mapsmessaging.state.config.TwinManagerConfig;
import io.mapsmessaging.state.config.TwinManagerConfigDTO;
import io.mapsmessaging.state.config.geospatial.GeoSpatialConfigLoader;
import io.mapsmessaging.state.config.n2k.N2KTwinConfig;
import io.mapsmessaging.state.drone.core.TwinLifecycleStatus;
import io.mapsmessaging.state.drone.core.TwinManager;
import io.mapsmessaging.state.n2k.N2kSession;
import io.mapsmessaging.utilities.Agent;
import io.mapsmessaging.utilities.Lifecycle;
import io.mapsmessaging.utilities.configuration.ConfigurationManager;
import lombok.Getter;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

import static io.mapsmessaging.state.logging.StateLogMessages.*;

public class StateManagerAgent implements Agent {


  private final Logger logger = LoggerFactory.getLogger(StateManagerAgent.class);

  private final List<Lifecycle> lifecycleList = new ArrayList<>();

  @Getter
  private final AISN2KManager aisManager;

  @Getter
  private TwinManager twinManager;

  @Getter
  private StateAuditContext auditContext;
  private AuditorFactory.AuditorInstance auditorInstance;

  public StateManagerAgent() {
    TwinManagerConfigDTO config = ConfigurationManager.getInstance().getConfiguration(TwinManagerConfig.class);
    DroneInfoRegistry registry;
    TakProtocolDTO takConfig;
    if(config != null){
      try {
        AuditorFactory factory = new AuditorFactory();
        auditorInstance = factory.build(Path.of( EnvironmentConfig.getInstance().getPathLookups().get("MAPS_DATA")));
        auditContext = auditorInstance.getAuditContext();
      } catch (IOException e) {
        logger.log(STATE_MANAGER_AUDIT_INIT_FAILED, e);
      }

      GeoSpatialAreaRegistry geoSpatialAreaRegistry = null;
      if (config.getGeospatial() != null
          && config.getGeospatial().getAreas() != null
          && !config.getGeospatial().getAreas().isEmpty()) {
        try {
          geoSpatialAreaRegistry =
              GeoSpatialConfigLoader.load(config.getGeospatial(), config.getDroneInfo());
        } catch (IOException e) {
          throw new IllegalStateException("Unable to load TwinManager geospatial configuration", e);
        }
      }

      twinManager =
          new TwinManager(
              config.isRemoveExpiredTwins(),
              config.getStaleTimeoutMillis(),
              config.getHeartbeatTimeoutMillis(),
              config.getRetentionTimeoutMillis(),
              auditContext,
              geoSpatialAreaRegistry);
      registry = new DroneInfoRegistry(config.getDroneInfo());
      takConfig = config.getTak();
      N2KTwinConfig n2KTwinConfig = config.getN2KTwinConfig();
      lifecycleList.add(new SchedulerManager(twinManager));
      lifecycleList.add(new TakManager(twinManager, takConfig));
      lifecycleList.add(new MavlinkTwinManager(twinManager, registry, config));
      loadStateMessageAdapters(config);
      if(n2KTwinConfig != null && n2KTwinConfig.isEnable()){
        lifecycleList.add(new N2kSession(twinManager, n2KTwinConfig, registry));
      }
      lifecycleList.add(new TwinPublisherManager(twinManager, config.getPublish()));
      aisManager = new AISN2KManager(twinManager, config.getN2KTwinConfig());
      lifecycleList.add(aisManager);
    }
    else{
      aisManager = null;
    }
  }

  @Override
  public String getName() {
    return "State Manager";
  }

  @Override
  public String getDescription() {
    return "Manages state of known objects within memory and maintains a digital twin";
  }

  public GeoSpatialAreaRegistry getGeoSpatialAreaRegistry() {
    return twinManager == null ? null : twinManager.getGeoSpatialAreaRegistry();
  }

  private void loadStateMessageAdapters(TwinManagerConfigDTO config) {
    StateMessageAdapterContext context = new StateMessageAdapterContext(twinManager, config);
    ServiceLoader<StateMessageAdapterFactory> adapterFactories = ServiceLoader.load(StateMessageAdapterFactory.class);
    for (StateMessageAdapterFactory adapterFactory : adapterFactories) {
      adapterFactory.create(context).ifPresent(lifecycleList::add);
    }
  }

  @Override
  public synchronized void start() {
    try {
      logger.log(STATE_MANAGER_START);
      for(Lifecycle lifecycle: lifecycleList){
        lifecycle.start();
      }
    } finally {
      logger.log(STATE_MANAGER_STARTED);
    }
  }

  @Override
  public synchronized void stop() {
    logger.log(STATE_MANAGER_STOP);
    try {
      for(Lifecycle lifecycle: lifecycleList){
        lifecycle.start();
      }
    } finally {
      logger.log(STATE_MANAGER_STOPPED);
    }
  }

  @Override
  public SubSystemStatusDTO getStatus() {
    SubSystemStatusDTO status = new SubSystemStatusDTO();
    status.setName(getName());
    status.setComment(
        "Current objects: " + twinManager.getTwinCount()
            + ", active: " + twinManager.getTwinCountByStatus(TwinLifecycleStatus.ACTIVE)
            + ", disconnected: " + twinManager.getTwinCountByStatus(TwinLifecycleStatus.DISCONNECTED)
            + ", stale: " + twinManager.getTwinCountByStatus(TwinLifecycleStatus.STALE)
    );
    status.setStatus(Status.OK);
    return status;
  }
}
