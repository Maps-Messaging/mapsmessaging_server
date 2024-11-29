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

package io.mapsmessaging.dto.rest.protocol;

import io.mapsmessaging.dto.rest.session.SessionInformationDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(
    title = "Protocol Information",
    description = "Provides detailed information about the protocol and session"
)
public class ProtocolInformationDTO {

  @Schema(description = "Type of the protocol", allowableValues = {
      "amqp", "coap", "lora", "mqtt", "mqtt-sn", "mqttV5", "nmea", "semtech", "stomp", "websocket"
  })
  protected String type;

  private String sessionId;
  private long timeout;
  private long keepAlive;
  private String messageTransformationName;
  private Map<String, String> selectorMapping;
  private Map<String, String> destinationTransformationMapping;
  private List<SessionInformationDTO> sessionList;


}
