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

package io.mapsmessaging.rest.responses;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.integration.IntegrationInfoDTO;
import lombok.Getter;

import java.util.List;
import java.util.Map;

public class IntegrationDetailResponse extends BaseResponse {

  @Getter
//  @ApiModelProperty(value="List of interfaces")
  private final List<IntegrationInfoDTO> data;

  @Getter
  //@ApiModelProperty(value="Default values for all interfaces used unless overridden by the specific config")
  private final Map<String, Object> globalConfig;


  public IntegrationDetailResponse( List<IntegrationInfoDTO> list, ConfigurationProperties map) {
    data = list;
    if(map != null) {
      globalConfig = map.getMap();
    }
    else{
      globalConfig = null;
    }
  }

}
