package io.mapsmessaging.state.stanag.messages.task.feedback;

import com.google.gson.annotations.SerializedName;
import io.mapsmessaging.state.stanag.messages.TaskState;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
public class TaskFeedbackBody {

  private final UUID identifier;

  private final UUID node;

  private final TaskState state;

  @SerializedName("percent_complete")
  private Double percentComplete;

  @SerializedName("time_remaining")
  private final String timeRemaining;

  @SerializedName("waypoints_remaining")
  private final List<String> waypointsRemaining;
}