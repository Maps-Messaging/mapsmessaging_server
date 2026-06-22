package io.mapsmessaging.state.stanag.messages.node.description;

import com.google.gson.annotations.SerializedName;
import io.mapsmessaging.state.config.capability.TaskCapabilities;
import io.mapsmessaging.state.stanag.messages.node.common.Pose;
import io.mapsmessaging.state.stanag.messages.node.common.Velocity;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Getter
@Builder
public class NodeDescriptionBody {

  private final UUID identifier;

  private final Map<String, Object> description;

  private final TaskCapabilities capabilities;

  private final Instant timestamp;

  @SerializedName("time_of_validity")
  private final Instant timeOfValidity;

  private final Pose pose;

  private final Velocity velocity;

  @SerializedName("time_of_initiation")
  private final Instant timeOfInitiation;

  @SerializedName("source_of_information")
  private final UUID sourceOfInformation;
}