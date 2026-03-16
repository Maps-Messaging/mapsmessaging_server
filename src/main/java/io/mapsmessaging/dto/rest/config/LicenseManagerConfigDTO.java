package io.mapsmessaging.dto.rest.config;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "License Management Configuration DTO")
public class LicenseManagerConfigDTO extends BaseManagerConfigDTO {
  @Schema(
      description = "MAPS registered client name",
      example = "Company B.V.",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  protected String clientName;

  @Schema(
      description = "MAPS license secret retrived from Maps support",
      example = "license string",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  protected String clientSecret;

  public LicenseManagerConfigDTO() {
    super("LicenseManagerConfigDTO");
  }
}
