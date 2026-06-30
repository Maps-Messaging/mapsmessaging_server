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
import io.mapsmessaging.network.protocol.impl.n2k.N2kProtocol;
import io.mapsmessaging.schemas.formatters.impl.CanbusFormatter;
import io.mapsmessaging.state.config.n2k.N2KTwinConfig;
import io.mapsmessaging.state.drone.core.TwinManager;
import io.mapsmessaging.state.n2k.DroneMonitor;
import io.mapsmessaging.utilities.Lifecycle;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class AISN2KManager implements Lifecycle {

  private final N2KTwinConfig aisConfig;
  private final TwinManager twinManager;
  private final List<DroneMonitor> droneMonitors;



  public AISN2KManager(TwinManager twinManager, N2KTwinConfig aisConfig) {
    this.aisConfig = aisConfig;
    this.twinManager = twinManager;
    droneMonitors = new CopyOnWriteArrayList<>();
  }

  @Override
  public void start() {

  }

  @Override
  public void stop() {
    for(DroneMonitor droneMonitor: droneMonitors){
      droneMonitor.close();
    }
  }

  public void registerProtocol(N2kProtocol n2kprotocol){
    if(aisConfig.isPublishMavlinkDrones()) {
      N2kMessageParser parser = ((CanbusFormatter) n2kprotocol.getFormatter()).getParser();
      droneMonitors.add(new DroneMonitor(twinManager, aisConfig.getAis(), parser, n2kprotocol));
    }
  }

  public void unregisterProtocol(N2kProtocol n2kprotocol){
    for(DroneMonitor droneMonitor: droneMonitors){
      if(droneMonitor.getN2kProtocol() == n2kprotocol){
        droneMonitors.remove(droneMonitor);
        droneMonitor.close();
        break;
      }
    }
  }

}
