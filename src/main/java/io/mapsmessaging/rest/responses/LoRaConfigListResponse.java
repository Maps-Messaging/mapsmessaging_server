/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 * Copyright [ 2024 - 2025 ] [Maps Messaging B.V.]
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

package io.mapsmessaging.rest.responses;

import io.mapsmessaging.dto.rest.lora.LoRaDeviceConfigInfoDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(
    name = "LoRaConfigListResponse",
    description = "A response containing a list of LoRa device configuration details."
)
public class LoRaConfigListResponse extends BaseResponse {

  @Schema(
      description = "A list of LoRa device configuration information objects.",
      nullable = false
  )
  private final List<LoRaDeviceConfigInfoDTO> data;

  public LoRaConfigListResponse(List<LoRaDeviceConfigInfoDTO> data) {
    this.data = data;
  }
}
