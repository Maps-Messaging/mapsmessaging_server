package io.mapsmessaging.dto.rest.config;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "License Management Configuration DTO")
public class LicenseManagerConfigDTO extends BaseConfigDTO implements ConfigurationManagerDTO {
  @Schema(
      description = "MAPS registered client name",
      example = "Company B.V."
  )
  protected String clientName;
  @Schema(
      description = "MAPS license secret retrived from Maps support",
      example = "license string"
  )
  protected String clientSecret;
}
