/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 * Copyright [ 2024 - 2024 ] [Maps Messaging B.V.]
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

package io.mapsmessaging.dto.helpers;

import io.mapsmessaging.dto.rest.integration.IntegrationInfoDTO;
import io.mapsmessaging.network.io.connection.EndPointConnection;

public class IntegrationInfoHelper {

  public static IntegrationInfoDTO fromEndPointConnection(EndPointConnection endPointConnection) {
    String runState = "";
    if(endPointConnection.isStarted()){
      runState = "Started";
      if(endPointConnection.isPaused()){
        runState = "Paused";
      }
    }
    else{
      runState = "Stopped";
    }
    return new IntegrationInfoDTO( endPointConnection.getProperties(),  runState);
  }

  private IntegrationInfoHelper() {
  }
}
