package io.mapsmessaging.dto.rest.config.transformer.impl;

import io.mapsmessaging.dto.rest.config.transformer.TransformationConfigDTO;
import io.mapsmessaging.dto.rest.config.transformer.TransformationType;
import io.swagger.v3.oas.annotations.media.Schema;

public class JsonToSchemaTransformationDTO extends TransformationConfigDTO {

  public JsonToSchemaTransformationDTO() {
    super(TransformationType.JSON_TO_SCHEMA);
  }

  @Schema(
      description = "Schema name used to resolve the schema from the SchemaManager.",
      example = "base.location.Location",
      requiredMode = Schema.RequiredMode.REQUIRED,
      nullable = false,
      minLength = 1,
      maxLength = 512
  )
  protected String schemaName;

}
