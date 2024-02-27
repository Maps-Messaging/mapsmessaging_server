/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.mapsmessaging.engine.system.impl.server;

import io.mapsmessaging.BuildInfo;
import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.EndPointServerStatus;
import io.mapsmessaging.rest.data.ServerStatistics;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StatusMessage {

  private String serverName;
  private String version;
  private String buildDate;
  private long totalMemory;
  private long maxMemory;
  private long freeMemory;
  private int numberOfThreads;
  private ServerStatistics serverStatistics;
  private long timeToCreateNano;
  private long uptime;
  private long connections;
  private long destinations;
  private long storageSize;
  private Map<String, Integer> threadState = new LinkedHashMap<>();

  public StatusMessage(MessageDaemon messageDaemon){
    long nano = System.nanoTime();
    version = BuildInfo.getBuildVersion();
    buildDate = BuildInfo.getBuildDate();
    serverName = messageDaemon.getId();
    uptime = System.currentTimeMillis() - messageDaemon.getStartTime();

    destinations = messageDaemon.getDestinationManager().size();
    storageSize = messageDaemon.getDestinationManager().getStorageSize();
    connections = EndPoint.currentConnections.sum();

    //----------------------------------------------------
    // Overall server statistics
    serverStatistics = new ServerStatistics();
    //----------------------------------------------------

    //----------------------------------------------------
    // Memory stats
    maxMemory = Runtime.getRuntime().maxMemory();
    freeMemory = Runtime.getRuntime().freeMemory();
    totalMemory = Runtime.getRuntime().totalMemory();
    timeToCreateNano = System.nanoTime() - nano;
    //----------------------------------------------------

    //----------------------------------------------------
    // Thread states
    numberOfThreads = Thread.activeCount();
    for(Thread thread: Thread.getAllStackTraces().keySet()){
      Thread.State state = thread.getState();
      threadState.put(state.name(), threadState.getOrDefault(state.name(), 0) + 1);
    }
    //----------------------------------------------------

  }

}
