package io.mapsmessaging.config;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.RestApiManagerConfigDTO;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

import static org.junit.jupiter.api.Assertions.*;

class RestApiManagerConfigBranchCoverageTest {

  @Test
  void constructorReadsCoreServerAndCacheSettings() throws Exception {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("enabled", true);
    properties.put("enableAuthentication", true);
    properties.put("hostnames", "localhost,example.test");
    properties.put("port", 9443);
    properties.put("enableSwagger", false);
    properties.put("enableCache", true);
    properties.put("cacheLifetime", 1234L);
    properties.put("cacheCleanup", 5678L);
    properties.put("inactiveTimeout", 99);

    RestApiManagerConfig config = create(properties);

    assertTrue(config.isEnabled());
    assertTrue(config.isEnableAuthentication());
    assertEquals(9443, config.getPort());
    assertFalse(config.isEnableSwagger());
    assertTrue(config.isEnableCache());
    assertEquals(1234L, config.getCacheLifetime());
    assertEquals(99, config.getInactiveTimeout());
  }

  @Test
  void updateRejectsUnrelatedDtoAndAppliesCoreChanges() throws Exception {
    RestApiManagerConfig config = create(new ConfigurationProperties());
    assertFalse(config.update(new BaseConfigDTO()));

    RestApiManagerConfigDTO update = new RestApiManagerConfigDTO();
    update.setEnabled(!config.isEnabled());
    update.setEnableAuthentication(!config.isEnableAuthentication());
    update.setHostnames("new-host");
    update.setPort(config.getPort() + 1);
    update.setInactiveTimeout(config.getInactiveTimeout() + 1);
    update.setEnableSwagger(!config.isEnableSwagger());
    update.setEnableSwaggerUI(!config.isEnableSwaggerUI());
    update.setEnableWadlEndPoint(!config.isEnableWadlEndPoint());
    update.setEnableUserManagement(!config.isEnableUserManagement());
    update.setEnableSchemaManagement(!config.isEnableSchemaManagement());
    update.setEnableInterfaceManagement(!config.isEnableInterfaceManagement());
    update.setEnableDestinationManagement(!config.isEnableDestinationManagement());
    update.setTlsConfig(config.getTlsConfig());
    update.setStaticConfig(config.getStaticConfig());
    update.setCorsHeaders(config.getCorsHeaders());

    assertTrue(config.update(update));
    assertEquals("new-host", config.getHostnames());
  }

  private static RestApiManagerConfig create(ConfigurationProperties properties) throws Exception {
    Constructor<RestApiManagerConfig> constructor =
        RestApiManagerConfig.class.getDeclaredConstructor(ConfigurationProperties.class);
    constructor.setAccessible(true);
    return constructor.newInstance(properties);
  }
}