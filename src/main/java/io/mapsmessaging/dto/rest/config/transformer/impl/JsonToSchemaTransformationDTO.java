package io.mapsmessaging.dto.rest.config.transformer.impl;

import io.mapsmessaging.dto.rest.config.transformer.TransformationConfigDTO;
import io.mapsmessaging.dto.rest.config.transformer.TransformationType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;


@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Transformation DTO allows a schema lookup to use to convert from json to the native schema format")
public class JsonToSchemaTransformationDTO extends TransformationConfigDTO {
  public JsonToSchemaTransformationDTO() {
    super(TransformationType.JSON_TO_SCHEMA);
  }

  @Schema(
      description = "Exact schema name used to resolve the schema from the SchemaManager. " +
          "This is the preferred and unambiguous lookup key. " +
          "If provided, it takes precedence over format and messageName.",
      example = "base.location.Location",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true,
      minLength = 1,
      maxLength = 512
  )
  protected String schemaName;

  @Schema(
      description = "Schema format type used together with messageName to locate a schema when schemaName is not known. " +
          "Examples include protobuf, avro, json-schema, xml, csv, or other registered schema formats. " +
          "This field is ignored when schemaName is supplied.",
      example = "protobuf",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true,
      minLength = 1,
      maxLength = 64
  )
  protected String format;

  @Schema(
      description = "Logical message name used together with format to locate a schema when schemaName is not supplied. " +
          "This may be a simple message name such as Location or a fully qualified message name if required to avoid ambiguity. " +
          "If multiple schemas match, schemaName must be provided.",
      example = "Location",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true,
      minLength = 1,
      maxLength = 512
  )
  protected String messageName;

}