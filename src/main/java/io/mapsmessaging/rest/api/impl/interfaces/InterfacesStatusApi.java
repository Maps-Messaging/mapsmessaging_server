/*
 * Copyright [ 2020 - 2023 ] [Matthew Buckton]
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

package io.mapsmessaging.rest.api.impl.interfaces;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.network.EndPointManager;
import io.mapsmessaging.rest.data.interfaces.InterfaceStatus;
import io.mapsmessaging.utilities.stats.LinkedMovingAverageRecord;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

@Tag(name = "Server Interface Management")
@Path(URI_PATH)
public class InterfacesStatusApi extends BaseInterfaceApi {


  @GET
  @Path("/server/interface/{endpoint}/status")
  @Produces({MediaType.APPLICATION_JSON})
  public InterfaceStatus getInterfaceStatus(@PathParam("endpoint") String endpointName) {
    List<EndPointManager> endPointManagers = MessageDaemon.getInstance().getNetworkManager().getAll();
    for (EndPointManager endPointManager : endPointManagers) {
      if (isMatch(endpointName, endPointManager)) {
        return createStatus(endPointManager);
      }
    }
    return null;
  }

  @GET
  @Path("/server/interface/status")
  @Produces({MediaType.APPLICATION_JSON})
  public List<InterfaceStatus> getAllInterfaceStatus() {
    List<InterfaceStatus> results = new ArrayList<>();
    List<EndPointManager> endPointManagers = MessageDaemon.getInstance().getNetworkManager().getAll();
    for (EndPointManager endPointManager : endPointManagers) {
      results.add(createStatus(endPointManager));
    }
    return results;
  }

  private InterfaceStatus createStatus(EndPointManager endPointManager) {
    Map<String, LinkedMovingAverageRecord> statistics = new LinkedHashMap<>();
    addToMap(statistics, endPointManager.getEndPointServer().getAverageBytesRead());
    addToMap(statistics, endPointManager.getEndPointServer().getAverageBytesSent());
    addToMap(statistics, endPointManager.getEndPointServer().getAveragePacketsRead());
    addToMap(statistics, endPointManager.getEndPointServer().getAveragePacketsSent());

    return new InterfaceStatus(
        endPointManager.getName(),
        endPointManager.getEndPointServer().getTotalBytesSent(),
        endPointManager.getEndPointServer().getTotalBytesRead(),
        endPointManager.getEndPointServer().getTotalPacketsSent(),
        endPointManager.getEndPointServer().getTotalPacketsRead(),
        statistics
    );
  }

  private void addToMap(Map<String, LinkedMovingAverageRecord> map, LinkedMovingAverageRecord record) {
    map.put(record.getName(), record);
  }
}