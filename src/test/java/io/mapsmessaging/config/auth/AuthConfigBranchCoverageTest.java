package io.mapsmessaging.config.auth;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.auth.AuthConfigDTO;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AuthConfigBranchCoverageTest {

  @Test
  void absentRemoteBlockProducesEmptyTokenConfiguration() {
    AuthConfig config = new AuthConfig(new ConfigurationProperties());

    assertNull(config.getUsername());
    assertNull(config.getPassword());
    assertNotNull(config.getTokenConfig());
    assertTrue(config.getTokenConfig().isEmpty());
  }

  @Test
  void tokenGeneratorLoadsNestedTokenConfigurationAndRoundTripsOptionalFields() {
    ConfigurationProperties token = new ConfigurationProperties();
    token.put("audience", "maps");
    ConfigurationProperties remote = new ConfigurationProperties();
    remote.put("username", "user");
    remote.put("password", "pass");
    remote.put("tokenGenerator", "jwt");
    remote.put("sessionId", "session");
    remote.put("tokenConfig", token);
    ConfigurationProperties root = new ConfigurationProperties();
    root.put("remote", remote);

    AuthConfig config = new AuthConfig(root);
    ConfigurationProperties packed = config.toConfigurationProperties();
    ConfigurationProperties packedRemote = (ConfigurationProperties) packed.get("remote");

    assertEquals("maps", config.getTokenConfig().get("audience"));
    assertEquals("user", packedRemote.getProperty("username"));
    assertEquals("jwt", packedRemote.getProperty("tokenGenerator"));
  }

  @Test
  void updateHandlesNullCurrentValuesAndTokenMapChanges() {
    AuthConfig config = new AuthConfig(new ConfigurationProperties());
    AuthConfigDTO update = new AuthConfigDTO();
    update.setUsername("u");
    update.setPassword("p");
    update.setTokenGenerator("generator");
    update.setSessionId("s");
    update.setTokenConfig(new LinkedHashMap<>(Map.of("k", "v")));

    assertTrue(config.update(update));
    assertEquals("u", config.getUsername());
    assertEquals("v", config.getTokenConfig().get("k"));
    assertFalse(config.update(update));
    assertFalse(config.update(new BaseConfigDTO()));
  }
}