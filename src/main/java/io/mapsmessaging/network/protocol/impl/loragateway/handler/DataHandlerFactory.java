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

package io.mapsmessaging.network.protocol.impl.loragateway.handler;

import io.mapsmessaging.network.protocol.impl.loragateway.Constants;

import java.util.TreeMap;

public class DataHandlerFactory {

  private final TreeMap<Byte, PacketHandler> dataHandlers;
  private final PacketHandler unknownHandler;

  public DataHandlerFactory() {
    dataHandlers = new TreeMap<>();
    dataHandlers.put(Constants.DATA, new DataHandler());
    dataHandlers.put(Constants.LOG, new LogHandler());
    dataHandlers.put(Constants.FAILURE, new FailureHandler());
    dataHandlers.put(Constants.SUCCESSFUL, new SuccessHandler());
    dataHandlers.put(Constants.PING, new PingHandler());
    dataHandlers.put(Constants.VERSION, new VersionHandler());
    unknownHandler = new UnknownHandler();
  }

  public PacketHandler getHandler(byte command) {
    PacketHandler handler = dataHandlers.get(command);
    if (handler == null) {
      handler = unknownHandler;
    }
    return handler;
  }
}
