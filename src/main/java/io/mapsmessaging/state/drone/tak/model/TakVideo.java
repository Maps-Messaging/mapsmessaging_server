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


package io.mapsmessaging.state.drone.tak.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A video feed offered by the entity, rendered as the {@code __video} detail element. The client
 * plays the {@code url}; the connection entry beside it is the same stream taken apart, which is
 * what older clients read. Credentials stay in the URL: the connection entry has nowhere to put
 * them.
 */
@Data
@NoArgsConstructor
@Schema(description = "A video feed the client can play from the marker.")
public class TakVideo {

  @Schema(description = "Stable identifier of the feed, so a client does not collect duplicates.")
  private String uid;

  @Schema(description = "Name shown beside the feed.", example = "optical")
  private String alias;

  @Schema(description = "The stream, as the client should open it.", example = "rtsp://host:8554/optical_view")
  private String url;

  @Schema(description = "Host of the stream, without credentials.", example = "host")
  private String address;

  @Schema(description = "Port of the stream; the scheme's default when the URL omits it.", example = "8554")
  private int port;

  @Schema(description = "Path of the stream.", example = "/optical_view")
  private String path;

  @Schema(description = "Scheme the client plays it with.", example = "rtsp")
  private String protocol;
}
