/*
 *
 *  Copyright [ 2020 - 2024 ] [Matthew Buckton]
 *  Copyright [ 2024 - 2025 ] [Maps Messaging B.V.]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package io.mapsmessaging.dto.rest.config.network;


import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.network.impl.*;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = DtlsConfigDTO.class, name = "dtls"),
    @JsonSubTypes.Type(value = LoRaConfigDTO.class, name = "lora"),
    @JsonSubTypes.Type(value = SerialConfigDTO.class, name = "serial"),
    @JsonSubTypes.Type(value = TcpConfigDTO.class, name = "tcp"),
    @JsonSubTypes.Type(value = TlsConfigDTO.class, name = "ssl"),
    @JsonSubTypes.Type(value = UdpConfigDTO.class, name = "udp"),
})
@Schema(
    description = "Abstract base class for all schema configurations",
    discriminatorProperty = "type",
    discriminatorMapping = {
        @DiscriminatorMapping(value = "dtls", schema = DtlsConfigDTO.class),
        @DiscriminatorMapping(value = "lora", schema = LoRaConfigDTO.class),
        @DiscriminatorMapping(value = "serial", schema = SerialConfigDTO.class),
        @DiscriminatorMapping(value = "tcp", schema = TcpConfigDTO.class),
        @DiscriminatorMapping(value = "ssl", schema = TlsConfigDTO.class),
        @DiscriminatorMapping(value = "udp", schema = UdpConfigDTO.class),
    })

@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
public class EndPointConfigDTO extends BaseConfigDTO {

  @Schema(description = "Type of the endpoint",
      example = "tcp, ssl, udp, dtls, lora, serial",
      allowableValues = {"tcp", "ssl", "udp", "dtls", "lora", "serial"}
  )
  protected String type;

  @Schema(description = "Whether the endpoint is discoverable", example = "false")
  protected boolean discoverable;

  @Schema(description = "Number of selector threads", example = "2")
  protected int selectorThreadCount;

  @Schema(description = "Server read buffer size in bytes", example = "10240")
  protected long serverReadBufferSize;

  @Schema(description = "Server write buffer size in bytes", example = "10240")
  protected long serverWriteBufferSize;
}
