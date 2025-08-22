package io.mapsmessaging.dto.rest.config.protocol.impl;

import io.mapsmessaging.dto.rest.config.protocol.ProtocolConfigDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Schema(description = "Common Satellite Configuration DTO")
public class BaseSatelliteConfigDTO extends ProtocolConfigDTO {

  @Schema(description = "Time in seconds to poll the modem for incoming messages", example = "15", defaultValue = "10")
  protected long incomingMessagePollInterval;

  @Schema(description = "Time in seconds to poll for outgoing messages", example = "60", defaultValue = "60")
  protected long outgoingMessagePollInterval;

  @Schema(description = "maximum buffer size allowed by the satellite communications", example = "4000", defaultValue = "4000")
  protected int maxBufferSize;

  @Schema(description = "minimum sized buffer that will be compressed", example = "512", defaultValue = "256")
  protected int compressionCutoffSize;

  @Schema(description = "life time of message in minutes", example = "5", defaultValue = "10")
  protected int messageLifeTimeInMinutes;

  @Schema(description = "Shared secret for encryption", example="this is a shared secret", defaultValue = "")
  protected String sharedSecret;

  @Schema(description = "If set, then high priority messages will NOT be queued, will incur additional charges", defaultValue = "false", example = "false")
  protected boolean sendHighPriorityMessages;
}
