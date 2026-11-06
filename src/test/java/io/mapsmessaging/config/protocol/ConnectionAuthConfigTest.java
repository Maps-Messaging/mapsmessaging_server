package io.mapsmessaging.config.protocol;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.destination.FormatConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.ConnectionAuthConfigDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConnectionAuthConfigTest {

  @Test
  void configurationRoundTripsAuthenticationFields() {
    ConfigurationProperties props = new ConfigurationProperties();
    props.put("username", "user");
    props.put("password", "secret");
    props.put("clientId", "client-1");
    props.put("tokenGenerator", "auth0");

    ConnectionAuthConfig source = new ConnectionAuthConfig(props);
    ConnectionAuthConfig restored =
        new ConnectionAuthConfig(source.toConfigurationProperties());

    assertEquals(source.getUsername(), restored.getUsername());
    assertEquals(source.getPassword(), restored.getPassword());
    assertEquals(source.getClientId(), restored.getClientId());
    assertEquals(source.getTokenGenerator(), restored.getTokenGenerator());
  }

  @Test
  void updateAppliesChangesAndRejectsWrongDto() {
    ConnectionAuthConfig config =
        new ConnectionAuthConfig(new ConfigurationProperties());

    ConnectionAuthConfigDTO update = new ConnectionAuthConfigDTO();
    update.setUsername("new-user");
    update.setPassword("new-pass");
    update.setClientId("new-client");
    update.setTokenGenerator("auth0");

    assertTrue(config.update(update));
    assertEquals("new-user", config.getUsername());
    assertEquals("new-pass", config.getPassword());
    assertEquals("new-client", config.getClientId());
    assertEquals("auth0", config.getTokenGenerator());
    assertFalse(config.update(update));
    assertFalse(config.update(new FormatConfigDTO()));
  }
}
