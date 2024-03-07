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

package io.mapsmessaging.auth.registry.principal;

import lombok.Getter;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

public class AccessIdPrincipal implements Principal {

  @Getter
  private final List<UUID> accessIds;

  public AccessIdPrincipal(List<UUID> accessIds) {
    this.accessIds = accessIds;
  }

  public String getName() {
    return "AccessIdPrinicpal";
  }
}
