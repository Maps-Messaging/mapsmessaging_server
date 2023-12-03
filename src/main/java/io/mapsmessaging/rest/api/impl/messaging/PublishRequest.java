package io.mapsmessaging.rest.api.impl.messaging;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PublishRequest {
  @NotNull
  @Schema(description = "Topic to which the message will be published")
  private String destinationName;

  @NotNull
  @Schema(description = "Message object containing the data to be published")
  private Message message;

  @Schema(description = "Should the message be retained on the destination", defaultValue = "false")
  private boolean retain = false;
}
