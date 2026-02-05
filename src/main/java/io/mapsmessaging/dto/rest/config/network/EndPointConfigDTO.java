/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
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

package io.mapsmessaging.dto.rest.config.network;


import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.network.impl.*;
import io.mapsmessaging.network.protocol.impl.proxy.ProxyProtocolMode;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = DtlsConfigDTO.class, name = "dtls"),
    @JsonSubTypes.Type(value = LoRaSerialConfigDTO.class, name = "loraSerial"),
    @JsonSubTypes.Type(value = LoRaChipConfigDTO.class, name = "loraDevice"),
    @JsonSubTypes.Type(value = SerialConfigDTO.class, name = "serial"),
    @JsonSubTypes.Type(value = TcpConfigDTO.class, name = "tcp"),
    @JsonSubTypes.Type(value = TlsConfigDTO.class, name = "ssl"),
    @JsonSubTypes.Type(value = UdpConfigDTO.class, name = "udp"),
    @JsonSubTypes.Type(value = SatelliteEndPointDTO.class, name = "satellite"),
    @JsonSubTypes.Type(value = CanbusConfigDTO.class, name = "canbus"),
})
@Schema(
    description = "Abstract base class for all schema configurations",
    discriminatorProperty = "type",
    discriminatorMapping = {
        @DiscriminatorMapping(value = "dtls", schema = DtlsConfigDTO.class),
        @DiscriminatorMapping(value = "loraSerial", schema = LoRaSerialConfigDTO.class),
        @DiscriminatorMapping(value = "loraDevice", schema = LoRaChipConfigDTO.class),
        @DiscriminatorMapping(value = "serial", schema = SerialConfigDTO.class),
        @DiscriminatorMapping(value = "tcp", schema = TcpConfigDTO.class),
        @DiscriminatorMapping(value = "ssl", schema = TlsConfigDTO.class),
        @DiscriminatorMapping(value = "udp", schema = UdpConfigDTO.class),
        @DiscriminatorMapping(value = "satellite", schema = SatelliteEndPointDTO.class),
        @DiscriminatorMapping(value = "canbus", schema = CanbusConfigDTO.class),
    })

@Data
@EqualsAndHashCode(callSuper = false)
@SuppressWarnings("java:S1313") // the IP address is used in an example and it exposes no private info
public class EndPointConfigDTO extends BaseConfigDTO {

  public EndPointConfigDTO(String type){
    this.type = type;
  }

  @Schema(description = "Type of the endpoint",
      example = "tcp",
      allowableValues = {"tcp", "ssl", "udp", "dtls", "loraDevice", "loraSerial", "serial", "satellite", "canbus"},
      requiredMode = Schema.RequiredMode.REQUIRED
  )
  protected String type;

  @Schema(
      description = "Whether the endpoint is discoverable",
      example = "false",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      defaultValue = "false",
      nullable = false
  )
  protected boolean discoverable = false;

  @Schema(
      description = "Number of selector threads",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      example = "2",
      minimum = "1",
      maximum = "10000",
      defaultValue = "2",
      nullable = true
  )
  protected int selectorThreadCount = 2;

  @Schema(
      description = "Server read buffer size in bytes",
      example = "10240",
      minimum = "1024",
      maximum = "104857600",
      defaultValue = "10240"
  )
  protected long serverReadBufferSize = 10240;

  @Schema(
      description = "Server write buffer size in bytes",
      example = "10240",
      minimum = "1024",
      maximum = "104857600"
  )
  protected long serverWriteBufferSize = 10240;

  @Schema(
      description = "Proxy Protocol support mode. 'ENABLED' allows but doesn't require it, 'REQUIRED' enforces it, 'DISABLED' will NOT check for incoming PROXY requests.",
      defaultValue = "DISABLED",
      example = "REQUIRED",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  protected ProxyProtocolMode proxyProtocolMode = ProxyProtocolMode.DISABLED;

  @Schema(
      description = "Comma-separated list of allowed proxy source addresses. Supports hostnames, IPv4/IPv6 addresses, and CIDR blocks (e.g., 192.168.0.0/24, ::1, example.com).",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      pattern = "^\\s*[^,\\s][^,]*\\s*(?:,\\s*[^,\\s][^,]*\\s*)*$",
      example = "example.com, localhost, 192.168.1.10, [2001:db8::1]",
      defaultValue = "",
      nullable = true
  )
  protected String allowedProxyHosts;

  @Schema(
      description = "Time to wait for a client to establish the connection, in milliseconds",
      example = "5000",
      minimum = "1000",
      maximum = "120000"
  )
  protected long connectionTimeout = 5000;
}
