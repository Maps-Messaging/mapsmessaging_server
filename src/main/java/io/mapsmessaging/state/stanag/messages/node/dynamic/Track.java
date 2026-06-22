package io.mapsmessaging.state.stanag.messages.node.dynamic;

import com.google.gson.annotations.SerializedName;
import io.mapsmessaging.state.stanag.messages.node.common.EntityDescription;
import io.mapsmessaging.state.stanag.messages.node.common.Pose;
import io.mapsmessaging.state.stanag.messages.node.common.Velocity;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class Track {

  private final UUID identifier;

  private final EntityDescription description;

  private final Instant timestamp;

  private final Pose pose;

  @SerializedName("track_phase")
  private final String trackPhase;

  @SerializedName("source_of_information")
  private final UUID sourceOfInformation;

  @SerializedName("time_of_initiation")
  private final Instant timeOfInitiation;

  @SerializedName("time_of_validity")
  private final Instant timeOfValidity;

  private final Velocity velocity;
}