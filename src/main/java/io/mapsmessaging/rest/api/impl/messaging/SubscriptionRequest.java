package io.mapsmessaging.rest.api.impl.messaging;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SubscriptionRequest {
  @NotNull
  @Schema(description = "Name of the subscription")
  private String name;

  @NotNull
  @Schema(description = "Name of the destination (topic or queue) for subscription")
  private String destinationName;

  @Schema(description = "Name of the named subscription, if any")
  private String namedSubscription;

  @Schema(description = "Filter expression using JMS selector syntax")
  private String filter; // JMS selector syntax filter

  @Schema(description = "Maximum depth of messages queued for the subscription", defaultValue = "1")
  private int maxDepth = 1; // Default value set here

  @Schema(description = "Should the message be retained on the destination", defaultValue = "false")
  private boolean retainMessage = false;

}
