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

package io.mapsmessaging.auth;


import io.mapsmessaging.api.features.DestinationType;
import lombok.Getter;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("java:S6548") // yes it is a singleton
public class ResourceTypes {


  private static class Holder {
    static final ResourceTypes INSTANCE = new ResourceTypes();
  }
  public static ResourceTypes getInstance() {
    return Holder.INSTANCE;
  }

  @Getter
  private final Set<String> resources;


  private ResourceTypes() {
    Set<String> tmp = new HashSet<>();
    tmp.add("Server");
    for(DestinationType type : DestinationType.values()){
      tmp.add(type.getName());
    }
    resources = Collections.unmodifiableSet(tmp);
  }

}
