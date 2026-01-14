package io.mapsmessaging.dto.rest.config;

import io.mapsmessaging.dto.rest.auth.SecurityManagerDTO;
import io.mapsmessaging.dto.rest.config.ml.MLModelManagerDTO;
import io.mapsmessaging.dto.rest.schema.SchemaManagerConfigDTO;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    description = "Base configuration DTO",
    discriminatorProperty = "type",
    oneOf = {
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
        DeviceManagerConfigDTO.class
    }
)
public interface ConfigurationManagerDTO {
}
