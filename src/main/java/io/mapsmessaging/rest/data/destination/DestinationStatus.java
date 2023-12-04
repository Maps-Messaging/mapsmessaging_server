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

package io.mapsmessaging.rest.data.destination;

import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.utilities.stats.LinkedMovingAverageRecord;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

@Data
public class DestinationStatus implements Serializable {

  @Schema(description = "Name of the destination")
  private final String name;
  @Schema(description = "Map of moving averages")
  private final Map<String, LinkedMovingAverageRecord> statistics;
  @Schema(description = "Map of storage statistics")
  private final Map<String, Map<String, LinkedMovingAverageRecord>> storeageStatistics;

  public DestinationStatus(DestinationImpl destinationImpl) {
    this.name = destinationImpl.getFullyQualifiedNamespace();
    statistics = destinationImpl.getStats().getStatistics();
    if(destinationImpl.getResourceStatistics() != null) {
      storeageStatistics = destinationImpl.getResourceStatistics().getStatistics();
    }
    else{
      storeageStatistics = null;
    }
  }
}