/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      https://commonsclause.com/
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.mapsmessaging.engine.session;

import io.mapsmessaging.config.AuthManagerConfig;
import io.mapsmessaging.config.SecurityManagerConfig;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.AuthManagerConfigDTO;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityManagerAnonymousTest {

  @Test
  void anonymousAccessIsDisabledByDefault() {
    assertFalse(new AuthManagerConfigDTO().isAllowAnonymous());
  }

  @Test
  void missingUsernameIsNotRewrittenWhenAnonymousAccessIsDisabled() {
    assertNull(SecurityManager.resolveUsername(null, false));
    assertEquals("", SecurityManager.resolveUsername("", false));
    assertEquals("user", SecurityManager.resolveUsername("user", false));
  }

  @Test
  void missingUsernameIsRewrittenWhenAnonymousAccessIsEnabled() {
    assertEquals("anonymous", SecurityManager.resolveUsername(null, true));
    assertEquals("anonymous", SecurityManager.resolveUsername("", true));
    assertEquals("user", SecurityManager.resolveUsername("user", true));
  }

  @Test
  void authManagerConfigDefaultsAnonymousAccessToFalse() throws Exception {
    ConfigurationProperties properties = authManagerProperties();

    AuthManagerConfig config = createAuthManagerConfig(properties);

    assertFalse(config.isAllowAnonymous());
    assertFalse(config.toConfigurationProperties().getBooleanProperty("allowAnonymous", false));
  }

  @Test
  void authManagerConfigRoundTripsAnonymousAccess() throws Exception {
    ConfigurationProperties properties = authManagerProperties();
    properties.put("allowAnonymous", true);

    AuthManagerConfig config = createAuthManagerConfig(properties);
    ConfigurationProperties roundTrip = config.toConfigurationProperties();

    assertTrue(config.isAllowAnonymous());
    assertTrue(roundTrip.getBooleanProperty("allowAnonymous", false));
  }

  @Test
  void securityManagerConfigContainsOnlyAuthenticationMappings() throws Exception {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("default", "PublicAuthConfig");
    properties.put("usernamePassword", "UsernamePasswordLoginModule");

    SecurityManagerConfig config = createSecurityManagerConfig(properties);
    ConfigurationProperties roundTrip = config.toConfigurationProperties();

    assertEquals("PublicAuthConfig", config.getAuthName("default"));
    assertEquals("UsernamePasswordLoginModule", config.getAuthName("usernamePassword"));
    assertFalse(roundTrip.containsKey("allowAnonymous"));
  }

  private ConfigurationProperties authManagerProperties() {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("authenticationEnabled", true);
    properties.put("authorisationEnabled", true);
    properties.put("config", new ConfigurationProperties());
    return properties;
  }

  private AuthManagerConfig createAuthManagerConfig(ConfigurationProperties properties) throws Exception {
    Constructor<AuthManagerConfig> constructor = AuthManagerConfig.class.getDeclaredConstructor(ConfigurationProperties.class);
    constructor.setAccessible(true);
    return constructor.newInstance(properties);
  }

  private SecurityManagerConfig createSecurityManagerConfig(ConfigurationProperties properties) throws Exception {
    Constructor<SecurityManagerConfig> constructor = SecurityManagerConfig.class.getDeclaredConstructor(ConfigurationProperties.class);
    constructor.setAccessible(true);
    return constructor.newInstance(properties);
  }
}
