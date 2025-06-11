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

package io.mapsmessaging.dto.helpers;

import io.mapsmessaging.BuildInfo;
import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.dto.rest.ServerInfoDTO;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.utilities.SystemUtils;

import java.util.LinkedHashMap;
import java.util.Map;

public class StatusMessageHelper {

  public static ServerInfoDTO fromMessageDaemon(MessageDaemon messageDaemon) {
    long nano = System.nanoTime();

    ServerInfoDTO dto = new ServerInfoDTO();
    dto.setVersion(BuildInfo.getBuildVersion());
    dto.setBuildDate(BuildInfo.getBuildDate());
    dto.setServerName(messageDaemon.getId());
    dto.setUptime(System.currentTimeMillis() - messageDaemon.getStartTime());
    dto.setDestinations(messageDaemon.getDestinationManager().size());
    dto.setStorageSize(messageDaemon.getDestinationManager().getStorageSize());
    dto.setConnections(EndPoint.currentConnections.sum());

    // Memory stats
    dto.setMaxMemory(Runtime.getRuntime().maxMemory());
    dto.setFreeMemory(Runtime.getRuntime().freeMemory());
    dto.setTotalMemory(Runtime.getRuntime().totalMemory());
    dto.setTimeToCreateNano(System.nanoTime() - nano);

    // Thread states
    dto.setNumberOfThreads(Thread.activeCount());
    Map<String, Integer> threadState = new LinkedHashMap<>();
    for (Thread thread : Thread.getAllStackTraces().keySet()) {
      Thread.State state = thread.getState();
      threadState.put(state.name(), threadState.getOrDefault(state.name(), 0) + 1);
    }
    dto.setThreadState(threadState);

    // CPU stats
    dto.setCpuTime(SystemUtils.getInstance().getCpuTime());
    dto.setCpuPercent(SystemUtils.getInstance().getCpuPercentage());

    return dto;
  }

  private StatusMessageHelper() {
  }
}
