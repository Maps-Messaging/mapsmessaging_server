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

package io.mapsmessaging.api.features;

import io.mapsmessaging.engine.destination.subscription.modes.NormalSubscriptionModeManager;
import io.mapsmessaging.engine.destination.subscription.modes.SchemaSubscriptionModeManager;
import io.mapsmessaging.engine.destination.subscription.modes.SubscriptionModeManager;
import lombok.Getter;

public enum DestinationMode {
  NORMAL(0, "Normal", "$normal", "Normal Event publish/subscription", true),
  SCHEMA(1, "Schema", "$schema", "Access to the destinations schema data", true);


  @Getter
  private final int id;
  @Getter
  private final String name;
  @Getter
  private final String description;
  @Getter
  private final boolean publishable;
  @Getter
  private final String namespace;

  private DestinationMode(int id, String name, String namespace, String description, boolean publishable) {
    this.id = id;
    this.name = name;
    this.namespace = namespace;
    this.description = description;
    this.publishable = publishable;
  }

  public SubscriptionModeManager getSubscriptionModeManager(){
    if(id == 0){
      return new NormalSubscriptionModeManager();
    }
    else if(id == 1){
      return new SchemaSubscriptionModeManager();
    }
    return new NormalSubscriptionModeManager();
  }

  public static DestinationMode getInstance(int id) {
    switch (id) {
      case 0:
        return NORMAL;

      case 1:
        return SCHEMA;

      default:
        throw new IllegalArgumentException("Invalid handestination  mode value supplied:" + id);
    }
  }
}

