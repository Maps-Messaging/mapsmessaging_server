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

package io.mapsmessaging.dto.rest.protocol.impl;

import io.mapsmessaging.dto.rest.protocol.ProtocolInformationDTO;
import io.mapsmessaging.dto.rest.session.SessionInformationDTO;
import io.mapsmessaging.network.protocol.impl.satellite.gateway.model.RemoteDeviceInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class SatelliteProtocolInformation extends ProtocolInformationDTO {

  @Schema(
      description = "Information about the session established with the satellite endpoint"
  )
  private SessionInformationDTO sessionInfo;

  @Schema(
      description = "Information about the remote satellite device"
  )
  private RemoteDeviceInfo remoteDeviceInfo;

  @Schema(
      description = "Total number of bytes transmitted through the satellite link",
      example = "1048576"
  )
  private long bytesTransmitted;

  @Schema(
      description = "Total number of bytes received through the satellite link",
      example = "524288"
  )
  private long bytesReceived;

  @Schema(
      description = "Total number of packets sent through the satellite link",
      example = "250"
  )
  private long packetsSent;

  @Schema(
      description = "Total number of packets received through the satellite link",
      example = "245"
  )
  private long packetsReceived;


  public SatelliteProtocolInformation() {
    type = "satellite";
  }
}
