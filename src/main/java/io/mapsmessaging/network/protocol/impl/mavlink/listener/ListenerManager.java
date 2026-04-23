/*
 *

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

package io.mapsmessaging.network.protocol.impl.mavlink.listener;

import io.mapsmessaging.network.protocol.impl.mavlink.packet.AutopilotVersionPacket;
import io.mapsmessaging.network.protocol.impl.mavlink.packet.MavlinkPacket;

import io.mapsmessaging.state.drone.core.TwinManager;
import io.mapsmessaging.state.drone.core.TwinUpdateContext;

import java.util.LinkedHashMap;
import java.util.Map;

public class ListenerManager {

  private final Map<Integer, Listener> listeners;

  public ListenerManager(TwinManager twinManager) {
    listeners = new LinkedHashMap<>();
    listeners.put(SysStatusListener.LISTENER_ID, new SysStatusListener(twinManager));
    listeners.put(GpsRawIntListener.LISTENER_ID, new GpsRawIntListener(twinManager));
    listeners.put(GlobalPositionListener.LISTENER_ID, new GlobalPositionListener(twinManager));
    listeners.put(AttitudeListener.LISTENER_ID, new AttitudeListener(twinManager));
    listeners.put(AutopilotVersionListener.LISTENER_ID, new AutopilotVersionListener(twinManager));
    listeners.put(HeartbeatListener.LISTENER_ID, new HeartbeatListener(twinManager));
    listeners.put(SystemTimeListener.LISTENER_ID, new SystemTimeListener(twinManager));
    listeners.put(AltitudeListener.LISTENER_ID, new AltitudeListener(twinManager));
    listeners.put(ExtendedSysStateListener.LISTENER_ID, new ExtendedSysStateListener(twinManager));
    listeners.put(MissionCurrentListener.LISTENER_ID, new MissionCurrentListener(twinManager));
    listeners.put(HomePositionListener.LISTENER_ID, new HomePositionListener(twinManager));
  }

  public boolean handle(int messageId, String twinId, MavlinkPacket pkt, TwinUpdateContext context) {
    Listener listener = listeners.get(messageId);
    if (listener != null) {
      listener.handle(twinId, pkt, context);
      return true;
    }
    return false;
  }
}
