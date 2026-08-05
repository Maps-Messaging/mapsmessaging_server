/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
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

package io.mapsmessaging.state.config.geospatial;

import io.mapsmessaging.geospatial.GeoSpatialBoundaryType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "GeoJSON boundary configuration")
public class GeoSpatialBoundaryConfigDTO {

  @Schema(description = "Name used to identify the boundary within its geospatial area")
  private String name;

  @Schema(description = "Filesystem path to the GeoJSON boundary file")
  private String path;

  @Schema(description = "Boundary rule applied during route validation")
  private GeoSpatialBoundaryType type = GeoSpatialBoundaryType.INSIDE;
}
