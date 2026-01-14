package io.mapsmessaging.dto.rest.config.destination;


import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes({@JsonSubTypes.Type(
    value = MemoryStorageConfigDTO.class,
    name = "memory"
), @JsonSubTypes.Type(
    value = PartitionStorageConfigDTO.class,
    name = "partition"
), @JsonSubTypes.Type(
    value = MemoryTierConfigDTO.class,
    name = "tieredMemory"
)})
@Schema(
    description = "Base class for all storage configurations",
    discriminatorProperty = "type",
    oneOf = {MemoryStorageConfigDTO.class, PartitionStorageConfigDTO.class, MemoryTierConfigDTO.class},
    discriminatorMapping = {@DiscriminatorMapping(
        value = "memory",
        schema = MemoryStorageConfigDTO.class
    ), @DiscriminatorMapping(
        value = "partition",
        schema = PartitionStorageConfigDTO.class
    ), @DiscriminatorMapping(
        value = "tieredMemory",
        schema = MemoryTierConfigDTO.class
    )}
)

@Data
public abstract class StorageConfigDTO extends BaseConfigDTO {

  @Schema(
      description = "Type of storage configuration",
      requiredMode = Schema.RequiredMode.REQUIRED,
      example = "memory",
      required = true,
      allowableValues = {"memory", "partition", "tieredMemory"}
  )
  protected String type;

  @Schema(
      description = "Debug mode enabled for the storage configuration",
      defaultValue = "false",
      example = "false",
      nullable = false,
      requiredMode = Schema.RequiredMode.NOT_REQUIRED
  )
  private boolean debug;

  protected StorageConfigDTO(String type){
    this.type = type;
  }

}
