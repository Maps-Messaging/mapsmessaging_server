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

import io.mapsmessaging.canbus.j1939.n2k.codec.N2kMessageParser;
import io.mapsmessaging.canbus.j1939.n2k.N2kParserFactory;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import static io.mapsmessaging.state.logging.StateLogMessages.N2K_AIS_OUTBOUND_NOT_CONFIGURED;
import io.mapsmessaging.state.config.n2k.N2KTwinConfig;
import io.mapsmessaging.state.drone.core.TwinManager;
import io.mapsmessaging.state.n2k.DroneMonitor;
import io.mapsmessaging.state.n2k.N2kSession;
import io.mapsmessaging.utilities.Lifecycle;


public class AISN2KManager implements Lifecycle {

  private static final Logger logger = LoggerFactory.getLogger(AISN2KManager.class);

  private final N2KTwinConfig aisConfig;
  private final TwinManager twinManager;
  private final N2kSession session;
  private DroneMonitor droneMonitor;



  public AISN2KManager(TwinManager twinManager, N2KTwinConfig aisConfig, N2kSession session) {
    this.aisConfig = aisConfig;
    this.twinManager = twinManager;
    this.session = session;
  }

  @Override
  public void start() {
    if (aisConfig == null || !aisConfig.isPublishMavlinkDrones()) {
      return;
    }
    if (session == null || aisConfig.getOutboundTopicName() == null || aisConfig.getOutboundTopicName().isBlank()) {
      logger.log(N2K_AIS_OUTBOUND_NOT_CONFIGURED);
      return;
    }
    N2kMessageParser parser = new N2kMessageParser(N2kParserFactory.getN2kParser());
    droneMonitor = new DroneMonitor(twinManager, aisConfig.getAis(), parser, session);
    twinManager.addObserver(droneMonitor);
  }

  @Override
  public void stop() {
    if (droneMonitor != null) {
      droneMonitor.close();
      droneMonitor = null;
    }
  }
}
