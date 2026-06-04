package io.mapsmessaging.state.capability;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TaskCapabilities {

  @SerializedName("task_capabilities")
  private List<TaskCapability> tasks = new ArrayList<>();

  @SerializedName("task_conditions_mode")
  private TaskConditionMode taskConditionsMode = TaskConditionMode.SIMPLE_TASK_STATE;

  @SerializedName("task_conditions_template")
  private TaskTemplateMode taskConditionsTemplate = TaskTemplateMode.NOT_SUPPORTED;

}