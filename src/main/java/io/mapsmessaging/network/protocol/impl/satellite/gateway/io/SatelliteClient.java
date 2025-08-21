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

package io.mapsmessaging.network.protocol.impl.satellite.gateway.io;

import io.mapsmessaging.network.protocol.impl.satellite.gateway.model.MessageData;
import io.mapsmessaging.network.protocol.impl.satellite.gateway.model.RemoteDeviceInfo;

import java.io.IOException;
import java.util.List;
import java.util.Queue;

public interface SatelliteClient {

  // Authenticate and validate the session
  boolean authenticate() throws IOException, InterruptedException;

  void close();

  // Get information on all known modems/devices
  List<RemoteDeviceInfo> getTerminals(String deviceId) throws IOException, InterruptedException;

  // Receive
  Queue<MessageData> scanForIncoming();

  // Process any pending messages
  void processPendingMessages(List<MessageData> queue);

  void unmute(String deviceId);

  void mute(String deviceId);

}
