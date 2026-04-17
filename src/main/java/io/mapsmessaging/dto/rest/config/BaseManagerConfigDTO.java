package io.mapsmessaging.dto.rest.config;

import io.mapsmessaging.dto.rest.auth.SecurityManagerDTO;
import io.mapsmessaging.dto.rest.config.ml.MLModelManagerDTO;
import io.mapsmessaging.dto.rest.schema.SchemaManagerConfigDTO;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    description = "Base configuration DTO for configuration managers.",
    discriminatorProperty = "type",
    requiredProperties = {"type"},
    oneOf = {
        AggregatorManagerConfigDTO.class,
        NetworkConnectionManagerConfigDTO.class,
        RestApiManagerConfigDTO.class,
        RoutingManagerConfigDTO.class,
        DiscoveryManagerConfigDTO.class,
        MLModelManagerDTO.class,
        SchemaManagerConfigDTO.class,
        AuthManagerConfigDTO.class,
        LoRaDeviceManagerConfigDTO.class,
        JolokiaConfigDTO.class,
        TenantManagementConfigDTO.class,
        MessageDaemonConfigDTO.class,
        DestinationManagerConfigDTO.class,
        LicenseManagerConfigDTO.class,
        SecurityManagerDTO.class,
        NetworkManagerConfigDTO.class,
        DeviceManagerConfigDTO.class,
        TwinManagerConfigDTO.class
    }
)
public abstract class BaseManagerConfigDTO extends BaseConfigDTO {

  @Schema(
      description = "Discriminator for the concrete configuration manager DTO.",
      requiredMode = Schema.RequiredMode.REQUIRED,
      accessMode = Schema.AccessMode.READ_ONLY,
      example = "AuthManagerConfig"
  )
  private String type;

  protected BaseManagerConfigDTO() {
  }

  protected BaseManagerConfigDTO(String type) {
    this.type = type;
  }

  public String getType() {
    return type;
  }

  protected void setType(String type) {
    this.type = type;
  }

  public abstract String getName();

}
