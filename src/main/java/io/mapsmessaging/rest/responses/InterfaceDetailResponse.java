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

package io.mapsmessaging.rest.responses;

import io.mapsmessaging.rest.data.InterfaceInfo;
import io.mapsmessaging.utilities.configuration.ConfigurationProperties;
import io.swagger.v3.oas.annotations.*;
import lombok.Getter;

import javax.ws.rs.core.Request;
import java.util.List;
import java.util.Map;

public class InterfaceDetailResponse extends BaseResponse {

  @Getter
//  @ApiModelProperty(value="List of interfaces")
  private final List<InterfaceInfo> data;

  @Getter
  //@ApiModelProperty(value="Default values for all interfaces used unless overridden by the specific config")
  private final Map<String, Object> globalConfig;


  public InterfaceDetailResponse(Request request, List<InterfaceInfo> list, ConfigurationProperties map){
    super(request);
    data = list;
    if(map != null) {
      globalConfig = map.getMap();
    }
    else{
      globalConfig = null;
    }
  }

}
