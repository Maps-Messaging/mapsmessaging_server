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
    description = "MAVLink protocol configuration. Controls session handling, topic mapping, JSON conversion, source filtering, message filtering, frame forwarding, duplicate suppression, and rejected frame publishing."
)
public class MavlinkConfigDTO extends ProtocolConfigDTO {

  public MavlinkConfigDTO() {
    super("mavlink");
  }

  @Schema(
      description =
          "MAVLink dialect name or fully qualified path to a MAVLink dialect XML file. "
              + "If blank, the default MAVLink dialect is used. If the value resolves to an existing file, "
              + "it is loaded as a dialect XML path; otherwise it is treated as a dialect name.",
      example = "common",
      defaultValue = "",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  protected String dialectName = "";

  @Schema(
      description = "Idle session timeout in seconds. The session is closed if no MAVLink traffic is received within this period.",
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
      description = "Maximum number of in-flight MAVLink events per session. This limits back-pressure and memory usage.",
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
          "Topic name template used when publishing accepted MAVLink messages. "
              + "Supported placeholders: {remoteSocket}, {systemId}, {systemName}, {componentId}, {messageId}, {messageName}. "
              + "{systemName} is populated from acceptedSources.name when a matching source entry exists.",
      example = "/{remoteSocket}/{systemId}/{componentId}/{messageName}",
      defaultValue = "/{remoteSocket}/{systemId}/{componentId}/{messageName}",
      requiredMode = Schema.RequiredMode.REQUIRED
  )
  protected String topicNameTemplate = "/{remoteSocket}/{systemId}/{componentId}/{messageName}";

  @Schema(
      description =
          "Topic name template used when publishing MAVLink sequence monitor status changes. "
              + "Supported placeholders: {remoteSocket}, {systemId}, {systemName}, {componentId}, {messageId}, {messageName}. "
              + "{systemName} is populated from acceptedSources.name when a matching source entry exists.",
      example = "/{remoteSocket}/{systemId}/{componentId}/{messageName}/status",
      defaultValue = "/{remoteSocket}/{systemId}/{componentId}/{messageName}/status",
      requiredMode = Schema.RequiredMode.REQUIRED
  )
  protected String statusTopicNameTemplate = "/{remoteSocket}/{systemId}/{componentId}/{messageName}/status";

  @Schema(
      description =
          "Convert accepted MAVLink frames into a JSON MAVLink envelope before publishing. "
              + "If false, the raw MAVLink frame bytes are published.",
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
      description = "When forwarding is enabled, forward raw MAVLink frames rather than transformed or decoded messages.",
      example = "true",
      defaultValue = "true",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  protected boolean forwardRawFrames = true;

  @Schema(
      description =
          "If true, frames rejected by source or message filtering are still forwarded as raw MAVLink frames "
              + "to the configured forwardUrls. This only affects forwarding; rejected frames are still not "
              + "published to the normal accepted message topic.",
      example = "false",
      defaultValue = "false",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  protected boolean forwardRejectedRawFrames = false;

  @Schema(
      description =
          "Prevent forwarding a MAVLink packet back to its source address and port if that address is present in forwardUrls.",
      example = "true",
      defaultValue = "true",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  protected boolean dropIfTargetEqualsSource = true;

  @Schema(
      description =
          "Duplicate suppression window in milliseconds. Packets received with identical content within this window are dropped. "
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
              "Global allow-list of MAVLink message IDs. If empty, all MAVLink message IDs are accepted unless rejected by rejectedMessageIds. "
                  + "Per-source acceptedMessageIds in acceptedSources override this list for that source."
      )
  )
  protected List<Integer> acceptedMessageIds = new ArrayList<>();

  @ArraySchema(
      schema = @Schema(
          description =
              "Global reject-list of MAVLink message IDs. Applied after acceptedMessageIds and per-source filtering. "
                  + "If empty, no message IDs are explicitly rejected."
      )
  )
  protected List<Integer> rejectedMessageIds = new ArrayList<>();

  @ArraySchema(
      schema = @Schema(
          implementation = MavlinkAcceptedSourceDTO.class,
          description =
              "Protocol-level MAVLink source filter entries. Each entry identifies a source by systemId and componentId, "
                  + "optionally provides a name for topic template expansion, and may define per-source accepted message IDs. "
                  + "This is not twin enrichment metadata; vehicle class and descriptions belong in twin configuration."
      )
  )
  protected List<MavlinkAcceptedSourceDTO> acceptedSources = new ArrayList<>();

  @Schema(
      description =
          "If true, only MAVLink sources listed in acceptedSources are accepted. "
              + "Frames from unknown systemId/componentId pairs are rejected. "
              + "If false, unknown sources are accepted and acceptedSources entries are used only for naming and per-source message filtering.",
      example = "false",
      defaultValue = "false",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  protected boolean rejectUnknownSources = false;

  @Schema(
      description =
          "Namespace used when publishing rejected MAVLink frames. Rejected frames are frames that fail source or message filtering. "
              + "The rejection reason may be appended as a child topic, for example /protocol/mavlink/dlq/message-id-not-accepted.",
      example = "/protocol/mavlink/dlq",
      defaultValue = "/protocol/mavlink/dlq",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED
  )
  protected String rejectedFrameNamespace = "/protocol/mavlink/dlq";

  @Schema(
      description =
          "If true, rejected frame events include metadata such as systemId, componentId, messageId, sequence, signed flag, "
              + "and payload. If false, the rejected frame event contains the original raw frame bytes.",
      example = "true",
      defaultValue = "true",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  protected boolean includeRejectedFrameMetadata = true;
}