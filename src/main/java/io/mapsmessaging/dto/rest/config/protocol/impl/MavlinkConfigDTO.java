package io.mapsmessaging.dto.rest.config.protocol.impl;

import io.mapsmessaging.dto.rest.config.protocol.ProtocolConfigDTO;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(
    description = "MAVLink protocol configuration. Controls session handling, topic mapping, JSON conversion, source filtering, message filtering, raw frame forwarding, and rejected frame DLQ publishing."
)
public class MavlinkConfigDTO extends ProtocolConfigDTO {

  public MavlinkConfigDTO() {
    super("mavlink");
  }

  @Schema(
      description =
          "Fully qualified path to the MAVLink dialect XML. "
              + "If not provided, the common dialect is used.",
      example = "C:/path/to/dialects/common.xml",
      defaultValue = "",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  protected String fullyQualifiedPathToDialectXml = "";

  @Schema(
      description = "Idle session timeout in seconds. Session is closed if no MAVLink traffic is received within this period.",
      example = "600",
      minimum = "1",
      maximum = "86400",
      defaultValue = "600",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  protected long idleSessionTimeout = 600;

  @Schema(
      description = "Maximum allowed session lifetime in seconds, regardless of activity.",
      example = "86400",
      minimum = "60",
      maximum = "604800",
      defaultValue = "86400",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  protected int maximumSessionExpiry = 86400;

  @Schema(
      description = "Interval in seconds at which MAVLink heartbeat or advertise messages are emitted.",
      example = "30",
      minimum = "1",
      maximum = "3600",
      defaultValue = "30",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  protected int advertiseInterval = 30;

  @Schema(
      description = "Maximum number of in-flight MAVLink events per session. Limits back-pressure and memory usage.",
      example = "1",
      minimum = "1",
      maximum = "1024",
      defaultValue = "1",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  protected int maxInFlightEvents = 1;

  @Schema(
      description =
          "Topic name template used when publishing decoded MAVLink messages. "
              + "Supported placeholders: {remoteSocket}, {systemId}, {systemName}, {componentId}, {messageName}.",
      example = "/{remoteSocket}/{systemId}/{componentId}/{messageName}",
      defaultValue = "/{remoteSocket}/{systemId}/{componentId}/{messageName}",
      requiredMode = Schema.RequiredMode.REQUIRED
  )
  protected String topicNameTemplate = "/{remoteSocket}/{systemId}/{componentId}/{messageName}";


  @Schema(
      description =
          "Topic name template used when publishing MAVLink session state changes induced when sequence number monitor detects issues. "
              + "Supported placeholders: {remoteSocket}, {systemId}, {systemName}, {componentId}, {messageName}.",
      example = "/{remoteSocket}/{systemId}/{componentId}/{messageName}",
      defaultValue = "/{remoteSocket}/{systemId}/{componentId}/{messageName}/status",
      requiredMode = Schema.RequiredMode.REQUIRED
  )
  protected String statusTopicNameTemplate = "/{remoteSocket}/{systemId}/{componentId}/{messageName}/status";

  @Schema(
      description =
          "Convert incoming MAVLink frames into JSON using the registered MAVLink message definitions. "
              + "If false, raw binary frames are published.",
      example = "true",
      defaultValue = "true",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  protected boolean parseToJson = true;

  @Schema(
      description =
          "Comma-separated list of MAVLink-compatible UDP endpoints to forward received frames to. "
              + "Each entry must be a valid udp://host:port/ URI. Blank disables forwarding.",
      example = "udp://192.168.1.50:14550/,udp://192.168.1.51:14550/",
      defaultValue = "",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED
  )
  protected String forwardUrls = "";

  @Schema(
      description =
          "When forwarding is enabled, forward raw MAVLink frames instead of decoded messages.",
      example = "true",
      defaultValue = "true",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  protected boolean forwardRawFrames = true;

  @Schema(
      description =
          "If true, frames rejected by source or message filtering are forwarded "
              + "as raw MAVLink frames to the configured forwardUrls. "
              + "This allows other MAVLink systems to receive frames even when "
              + "this server chooses not to parse or publish them locally.",
      example = "false",
      defaultValue = "false",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  protected boolean forwardRejectedRawFrames = false;

  @Schema(
      description =
          "Prevent forwarding a MAVLink packet back to its source address and port "
              + "if that address is present in forwardUrls.",
      example = "true",
      defaultValue = "true",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  protected boolean dropIfTargetEqualsSource = true;

  @Schema(
      description =
          "Duplicate suppression window in milliseconds. "
              + "Packets received with identical content within this window are dropped. "
              + "Set to 0 to disable duplicate detection.",
      example = "0",
      minimum = "0",
      maximum = "60000",
      defaultValue = "0",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  protected int dedupWindowMillis = 0;

  @ArraySchema(
      schema = @Schema(
          description =
              "Global allow-list of MAVLink message IDs. "
                  + "If empty, all MAVLink message IDs are accepted unless explicitly rejected "
                  + "by rejectedMessageIds. If populated, only message IDs in this list are accepted."
      )
  )
  protected List<Integer> acceptedMessageIds = new ArrayList<>();

  @ArraySchema(
      schema = @Schema(
          description =
              "Global reject-list of MAVLink message IDs. "
                  + "Applied after acceptedMessageIds. If empty, no message IDs are explicitly rejected."
      )
  )
  protected List<Integer> rejectedMessageIds = new ArrayList<>();

  @ArraySchema(
      schema = @Schema(
          implementation = MavlinkKnownSourceDTO.class,
          description =
              "Registry of known MAVLink sources identified by systemId and componentId. "
                  + "Entries may provide metadata such as a friendly name and optional "
                  + "per-source message filtering overrides."
      )
  )
  protected List<MavlinkKnownSourceDTO> knownSources = new ArrayList<>();

  @Schema(
      description =
          "If true, only MAVLink sources listed in knownSources are accepted. "
              + "Frames from unknown systemId/componentId pairs are rejected. "
              + "If false, unknown sources are accepted and knownSources entries "
              + "are used only for metadata and filtering overrides.",
      example = "false",
      defaultValue = "false",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  protected boolean rejectUnknownSources = false;

  @Schema(
      description =
          "Namespace used when publishing rejected MAVLink frames. "
              + "The rejection reason may be appended as a child topic "
              + "(for example: /protocol/mavlink/dlq/message-id-not-accepted).",
      example = "/protocol/mavlink/dlq",
      defaultValue = "/protocol/mavlink/dlq",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED
  )
  protected String rejectedFrameNamespace = "/protocol/mavlink/dlq";

  @Schema(
      description =
          "If true, rejected frame events include metadata such as source address, "
              + "systemId, componentId, messageId, and rejection reason in addition "
              + "to the raw MAVLink frame payload.",
      example = "true",
      defaultValue = "true",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  protected boolean includeRejectedFrameMetadata = true;
}