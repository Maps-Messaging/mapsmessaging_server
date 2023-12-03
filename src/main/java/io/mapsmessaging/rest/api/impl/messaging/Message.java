package io.mapsmessaging.rest.api.impl.messaging;

import io.mapsmessaging.api.features.Priority;
import io.mapsmessaging.api.features.QualityOfService;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class Message {
  @NotNull
  @Schema(description = "Payload of the message")
  private byte[] payload;
  @Schema(description = "Content type of the message")
  private String contentType;
  @Schema(description = "Correlation data of the message")
  private byte[] correlationData;
  @Schema(description = "Message parameters")
  private final Map<String, Object> dataMap;
  @Schema(description = "Message expiry time in milliseconds", defaultValue = "-1")
  private long expiry = -1;   // time in milliseconds when this message will expire
  @Schema(description = "Message priority 4 = Normal value, Highest = 10", defaultValue = "4")
  private int priority = Priority.NORMAL.getValue();
  @Schema(description = "Quality of Service level for the message, 0 : At most once, 1: At least once and 2: exactly once ", defaultValue = "0")
  private int qualityOfService = QualityOfService.AT_MOST_ONCE.getLevel();
}
