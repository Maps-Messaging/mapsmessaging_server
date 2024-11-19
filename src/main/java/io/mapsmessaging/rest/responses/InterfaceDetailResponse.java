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

package io.mapsmessaging.rest.responses;

import io.mapsmessaging.dto.rest.interfaces.InterfaceInfoDTO;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.Getter;

public class InterfaceDetailResponse extends BaseResponse {

  @Getter
//  @ApiModelProperty(value="List of interfaces")
  private final List<InterfaceInfoDTO> data;

  public InterfaceDetailResponse(HttpServletRequest request, List<InterfaceInfoDTO> list) {
    super(request);
    data = list;
  }

}
