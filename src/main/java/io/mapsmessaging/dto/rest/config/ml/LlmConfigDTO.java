package io.mapsmessaging.dto.rest.config.ml;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class LlmConfigDTO {

  @Schema(
      description = "API token used to authenticate with the LLM provider",
      example = "sk-proj-abc123..."
  )
  private String apiToken;

  @Schema(
      description = "Model name to use (e.g., gpt-4.1, gpt-4o, gpt-3.5-turbo)",
      example = "gpt-4.1"
  )
  private String model;
}
