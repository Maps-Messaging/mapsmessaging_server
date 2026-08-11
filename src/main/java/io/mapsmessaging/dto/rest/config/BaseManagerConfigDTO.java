package io.mapsmessaging.dto.rest.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.mapsmessaging.dto.rest.auth.SecurityManagerDTO;
import io.mapsmessaging.dto.rest.config.ml.MLModelManagerDTO;
import io.mapsmessaging.dto.rest.schema.SchemaManagerConfigDTO;
import io.mapsmessaging.state.config.TwinManagerConfigDTO;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
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
    ,
    discriminatorMapping = {
        @DiscriminatorMapping(value = "AggregatorManagerConfigDTO", schema = AggregatorManagerConfigDTO.class),
        @DiscriminatorMapping(value = "NetworkConnectionManagerConfigDTO", schema = NetworkConnectionManagerConfigDTO.class),
        @DiscriminatorMapping(value = "RestApiManagerConfigDTO", schema = RestApiManagerConfigDTO.class),
        @DiscriminatorMapping(value = "RoutingManagerConfigDTO", schema = RoutingManagerConfigDTO.class),
        @DiscriminatorMapping(value = "DiscoveryManagerConfigDTO", schema = DiscoveryManagerConfigDTO.class),
        @DiscriminatorMapping(value = "MLModelManagerDTO", schema = MLModelManagerDTO.class),
        @DiscriminatorMapping(value = "SchemaManagerConfigDTO", schema = SchemaManagerConfigDTO.class),
        @DiscriminatorMapping(value = "AuthManagerConfigDTO", schema = AuthManagerConfigDTO.class),
        @DiscriminatorMapping(value = "LoRaDeviceManagerConfigDTO", schema = LoRaDeviceManagerConfigDTO.class),
        @DiscriminatorMapping(value = "JolokiaConfigDTO", schema = JolokiaConfigDTO.class),
        @DiscriminatorMapping(value = "TenantManagementConfigDTO", schema = TenantManagementConfigDTO.class),
        @DiscriminatorMapping(value = "MessageDaemonConfigDTO", schema = MessageDaemonConfigDTO.class),
        @DiscriminatorMapping(value = "DestinationManagerConfigDTO", schema = DestinationManagerConfigDTO.class),
        @DiscriminatorMapping(value = "LicenseManagerConfigDTO", schema = LicenseManagerConfigDTO.class),
        @DiscriminatorMapping(value = "SecurityManagerDTO", schema = SecurityManagerDTO.class),
        @DiscriminatorMapping(value = "NetworkManagerConfigDTO", schema = NetworkManagerConfigDTO.class),
        @DiscriminatorMapping(value = "DeviceManagerConfigDTO", schema = DeviceManagerConfigDTO.class),
        @DiscriminatorMapping(value = "TwinManagerConfigDTO", schema = TwinManagerConfigDTO.class)
    }
)
public abstract class BaseManagerConfigDTO extends BaseConfigDTO {

  @Schema(
      description = "Discriminator for the concrete configuration manager DTO.",
      requiredMode = Schema.RequiredMode.REQUIRED,
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

  @JsonIgnore
  public abstract String getSimpleName();

}
