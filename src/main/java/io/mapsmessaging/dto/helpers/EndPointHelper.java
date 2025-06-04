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

import io.mapsmessaging.dto.rest.endpoint.EndPointDetailsDTO;
import io.mapsmessaging.dto.rest.endpoint.EndPointSummaryDTO;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.EndPointStatus;
import io.mapsmessaging.network.protocol.impl.proxy.ProxyProtocolInfo;
import io.mapsmessaging.security.SubjectHelper;

public class EndPointHelper {

  public static EndPointSummaryDTO buildSummaryDTO(String adapterName, EndPoint endPoint) {
    EndPointSummaryDTO summaryDTO = new EndPointSummaryDTO();
    ProxyProtocolInfo info = endPoint.getProxyProtocolInfo();
    summaryDTO.setProxyAddress(info != null ? info.getDestination().getHostString() : "");
    summaryDTO.setId(endPoint.getId());
    summaryDTO.setAdapter(adapterName);
    summaryDTO.setName(endPoint.getName());
    summaryDTO.setConnectedTimeMs(System.currentTimeMillis() - endPoint.getConnected());
    if(endPoint.getBoundProtocol() != null) {
      summaryDTO.setProtocolName(endPoint.getBoundProtocol().getName());
      summaryDTO.setProtocolVersion(endPoint.getBoundProtocol().getVersion());
    }
    else{
      summaryDTO.setProtocolName("Not Detected");
      summaryDTO.setProtocolVersion("N/A");

    }
    if (endPoint.getEndPointSubject() != null) {
      summaryDTO.setUser(SubjectHelper.getUsername(endPoint.getEndPointSubject()));
    } else {
      summaryDTO.setUser("anonymous");
    }

    summaryDTO.setLastRead(endPoint.getLastRead());
    summaryDTO.setLastWrite(endPoint.getLastWrite());

    EndPointStatus status = endPoint.getEndPointStatus();
    summaryDTO.setTotalBytesRead(status.getReadBytesTotal());
    summaryDTO.setTotalBytesWritten(status.getWriteBytesTotal());

    if (status.supportsMovingAverages()) {
      summaryDTO.setBytesRead(status.getReadByteAverages().getCurrent());
      summaryDTO.setBytesWritten(status.getWriteByteAverages().getCurrent());

      summaryDTO.setOverFlow(status.getBufferOverFlow().getCurrent());
      summaryDTO.setTotalOverflow(status.getBufferOverFlow().getTotal());

      summaryDTO.setUnderFlow(status.getBufferUnderFlow().getCurrent());
      summaryDTO.setTotalUnderflow(status.getBufferUnderFlow().getTotal());
    }
    return summaryDTO;
  }

  public static EndPointDetailsDTO buildDetailsDTO(String adapterName, EndPoint endPoint) {
    EndPointDetailsDTO detailsDTO = new EndPointDetailsDTO();
    detailsDTO.setEndPointSummary(buildSummaryDTO(adapterName, endPoint));
    detailsDTO.setProtocolInformation(endPoint.getBoundProtocol().getInformation());
    return detailsDTO;
  }

  private EndPointHelper() {}
}
