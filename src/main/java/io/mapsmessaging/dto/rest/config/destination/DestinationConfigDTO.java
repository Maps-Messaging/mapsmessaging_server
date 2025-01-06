/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 * Copyright [ 2024 - 2025 ] [Maps Messaging B.V.]
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

package io.mapsmessaging.dto.rest.config.destination;

import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper=false)
@NoArgsConstructor
@Schema(description = "Destination Configuration DTO")
public class DestinationConfigDTO extends BaseConfigDTO {

  @Schema(description = "Enable or disable synchronization", example = "true")
  protected boolean sync;

  @Schema(description = "Enable or disable debug mode", example = "true")
  protected boolean debug;

  @Schema(description = "Enable or disable remapping", example = "false")
  protected boolean remap;

  @Schema(description = "Item count for destination", example = "100")
  protected int itemCount;

  @Schema(description = "Polling time for expired events", example = "20")
  protected int expiredEventPoll;

  @Schema(description = "Auto-pause timeout in seconds", example = "300")
  protected int autoPauseTimeout;

  @Schema(description = "Maximum partition size in bytes", example = "4096")
  protected long maxPartitionSize;

  @Schema(description = "Trailing path", example = "path/to/trail")
  protected String trailingPath;

  @Schema(description = "Name of the destination", example = "destinationName")
  protected String name;

  @Schema(description = "Directory path for destination", example = "/var/data")
  protected String directory;

  @Schema(description = "Namespace for destination", example = "namespace")
  protected String namespace;

  @Schema(description = "Type of destination", example = "type")
  protected String type;

  @Schema(description = "Format configuration")
  protected FormatConfigDTO format;

  @Schema(description = "Cache configuration")
  protected CacheConfigDTO cache;

  @Schema(description = "Archive configuration")
  protected ArchiveConfigDTO archive;

  @Schema(description = "Namespace mapping", example = "mappedNamespace")
  protected String namespaceMapping;
}
