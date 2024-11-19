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

package io.mapsmessaging.rest.api.impl.integration;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.dto.rest.integration.IntegrationStatusDTO;
import io.mapsmessaging.dto.rest.stats.LinkedMovingAverageRecordDTO;
import io.mapsmessaging.network.io.connection.EndPointConnection;
import io.mapsmessaging.rest.api.impl.interfaces.BaseInterfaceApi;
import io.mapsmessaging.selector.ParseException;
import io.mapsmessaging.selector.SelectorParser;
import io.mapsmessaging.selector.operators.ParserExecutor;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Tag(name = "Server Integration Management")
@Path(URI_PATH)
public class IntergrationStatusApi extends BaseInterfaceApi {


  @GET
  @Path("/server/integration/{endpoint}/status")
  @Produces({MediaType.APPLICATION_JSON})
  public IntegrationStatusDTO getIntegrationStatus(@PathParam("endpoint") String endpointName) {
    checkAuthentication();
    if (!hasAccess("integrations")) {
      response.setStatus(403);
      return null;
    }

    List<EndPointConnection> endPointManagers = MessageDaemon.getInstance().getNetworkConnectionManager().getEndPointConnectionList();
    for (EndPointConnection endPointConnection : endPointManagers) {
      if (endpointName.equals(endPointConnection.getConfigName())) {
        return fromConnection(endPointConnection);
      }
    }
    return null;
  }

  @GET
  @Path("/server/integration/status")
  @Produces({MediaType.APPLICATION_JSON})
  public List<IntegrationStatusDTO> getAllIntegrationStatus(@QueryParam("filter") String filter) throws ParseException {
    checkAuthentication();
    if (!hasAccess("integrations")) {
      response.setStatus(403);
      return new ArrayList<>();
    }
    ParserExecutor parser = (filter != null && !filter.isEmpty())  ? SelectorParser.compile(filter) : null;
    List<EndPointConnection> endPointManagers = MessageDaemon.getInstance().getNetworkConnectionManager().getEndPointConnectionList();
    return endPointManagers.stream()
        .map(IntergrationStatusApi::fromConnection)
        .filter(status -> parser == null || parser.evaluate(status))
        .collect(Collectors.toList());

  }

  public static IntegrationStatusDTO fromConnection(EndPointConnection connection) {
    IntegrationStatusDTO dto = new IntegrationStatusDTO();
    dto.setState(connection.getState().getName());
    dto.setInterfaceName(connection.getConfigName());
    dto.setBytesSent(connection.getTotalBytesSent());
    dto.setBytesReceived(connection.getTotalBytesRead());
    dto.setMessagesSent(connection.getTotalPacketsSent());
    dto.setMessagesReceived(connection.getTotalPacketsRead());
    dto.setErrors(connection.getTotalErrors());

    // Initialize statistics and timestamps if connection details are available
    Map<String, LinkedMovingAverageRecordDTO> stats = new LinkedHashMap<>();
    if (connection.getConnection() != null && connection.getConnection().getEndPoint() != null) {
      dto.setLastReadTime(connection.getConnection().getEndPoint().getLastRead());
      dto.setLastWriteTime(connection.getConnection().getEndPoint().getLastWrite());
    }

    // Populate statistics
    addToMap(stats, connection.getAverageBytesRead());
    addToMap(stats, connection.getAverageBytesSent());
    addToMap(stats, connection.getAveragePacketsRead());
    addToMap(stats, connection.getAveragePacketsSent());
    dto.setStatistics(stats);

    return dto;
  }

  private static void addToMap(Map<String, LinkedMovingAverageRecordDTO> stats, LinkedMovingAverageRecordDTO average) {
    if (average != null) {
      stats.put(average.getName(), average);
    }
  }
}
