package io.mapsmessaging.dto.rest.config.destination;

import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.storage.StorageConfig;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@Schema(description = "Destination Configuration DTO")
public class DestinationConfigDTO extends BaseConfigDTO {

  @Schema(
      description = "Enable or disable remapping",
      example = "false",
      defaultValue = "false",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED
  )
  protected boolean remap = false;

  @Schema(
      description = "Trailing path",
      example = "path/to/trail",
      defaultValue = " ",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED
  )
  protected String trailingPath = "";

  @Schema(
      description = "Directory path for destination",
      example = "/var/data",
      requiredMode = Schema.RequiredMode.REQUIRED,
      nullable = false
  )
  protected String directory;

  @Schema(
      description = "Namespace for destination",
      example = "namespace",
      requiredMode = Schema.RequiredMode.REQUIRED,
      nullable = false
  )
  protected String namespace;

  @Schema(
      description = "Type of destination",
      example = "Partition",
      allowableValues = {"Partition", "Memory", "MemoryTier"},
      defaultValue = "Partition",
      requiredMode = Schema.RequiredMode.REQUIRED
  )
  protected String type = "Partition";

  @Schema(
      description = "Format configuration",
      type = "object",
      implementation = Object.class,
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  protected FormatConfigDTO format;

  @Schema(
      description = "Message override parameters",
      type = "object",
      implementation = Object.class,
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  protected MessageOverrideDTO messageOverride;

  @Schema(
      description = "Namespace mapping",
      example = "mappedNamespace",
      defaultValue = "",
      requiredMode = Schema.RequiredMode.REQUIRED,
      nullable = false
  )
  protected String namespaceMapping;

  @Schema(
      description = "Auto-pause timeout in seconds",
      example = "300",
      minimum = "0",
      maximum = "86400",
      defaultValue = "0",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED
  )
  protected int autoPauseTimeout = 300;

  @Schema(
      description = "Storage configuration",
      type = "object",
      implementation = Object.class,
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  protected StorageConfig storageConfig;

  @Schema(
      description = "Cache configuration",
      type = "object",
      implementation = Object.class,
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  protected CacheConfigDTO cache;
}
