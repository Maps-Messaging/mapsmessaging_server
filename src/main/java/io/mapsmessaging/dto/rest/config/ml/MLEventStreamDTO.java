package io.mapsmessaging.dto.rest.config.ml;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MLEventStreamDTO {

  @Schema(description = "Unique ID for the model stream")
  private String id;

  @Schema(description = "Topic filter to match incoming events")
  private String topicFilter;

  @Schema(description = "Schema ID that the event must match")
  private String schemaId;

  @Schema(description = "Selector used to evaluate events")
  private String selector;

  @Schema(description = "Where to publish outliers")
  private String outlierTopic;

  @Schema(description = "Max number of events to train the model")
  private int maxTrainEvents;

  @Schema(description = "Max time in seconds to train the model, 0 disables")
  private int maxTrainTimeSeconds;

  @Schema(description = "Outlier rate threshold to trigger retraining")
  private double retrainThreshold;
}