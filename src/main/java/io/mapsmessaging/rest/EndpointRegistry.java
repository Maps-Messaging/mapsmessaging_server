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

package io.mapsmessaging.rest;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class EndpointRegistry {

  private static final EndpointRegistry INSTANCE = new EndpointRegistry();

  private final List<EndpointInfo> endpoints;

  private EndpointRegistry() {
    this.endpoints = new CopyOnWriteArrayList<>();
  }

  public static EndpointRegistry getInstance() {
    return INSTANCE;
  }

  public void setEndpoints(List<EndpointInfo> endpointInfos) {
    endpoints.clear();
    endpoints.addAll(endpointInfos);
  }

  public List<EndpointInfo> getEndpoints() {
    return Collections.unmodifiableList(endpoints);
  }
}
