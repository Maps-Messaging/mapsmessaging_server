/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
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

package io.mapsmessaging.rest.data.lora;

import io.mapsmessaging.rest.responses.BaseResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.Getter;

public class LoRaListResponse extends BaseResponse {

  @Getter
  private final List<LoRaDeviceInfo> data;


  public LoRaListResponse(HttpServletRequest request, List<LoRaDeviceInfo> data) {
    super(request);
    this.data = data;
  }
}