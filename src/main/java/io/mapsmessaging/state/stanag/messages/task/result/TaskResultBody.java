package io.mapsmessaging.state.stanag.messages.task.result;

import com.google.gson.annotations.SerializedName;
import io.mapsmessaging.state.stanag.messages.FlexibleEnumeration;
import io.mapsmessaging.state.stanag.messages.TaskState;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class TaskResultBody {

  private final UUID identifier;

  private final UUID node;

  private final TaskState state;

  private final Instant timestamp;

  @SerializedName("result_reason")
  private final FlexibleEnumeration resultReason;
}