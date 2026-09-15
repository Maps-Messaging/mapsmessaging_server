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

import io.mapsmessaging.config.SecurityManagerConfig;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.auth.SecurityManagerDTO;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityManagerAnonymousTest {

  @Test
  void anonymousAccessIsDisabledByDefault() {
    assertFalse(new SecurityManagerDTO().isAllowAnonymous());
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
  void securityManagerConfigDefaultsAnonymousAccessToFalse() throws Exception {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("default", "PublicAuthConfig");
    properties.put("usernamePassword", "UsernamePasswordLoginModule");

    SecurityManagerConfig config = createConfig(properties);

    assertFalse(config.isAllowAnonymous());
    assertEquals("PublicAuthConfig", config.getAuthName("default"));
    assertEquals("UsernamePasswordLoginModule", config.getAuthName("usernamePassword"));
    assertFalse(config.getMap().containsKey("allowAnonymous"));
  }

  @Test
  void securityManagerConfigRoundTripsAnonymousAccess() throws Exception {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("allowAnonymous", true);
    properties.put("default", "PublicAuthConfig");
    properties.put("usernamePassword", "UsernamePasswordLoginModule");

    SecurityManagerConfig config = createConfig(properties);
    ConfigurationProperties roundTrip = config.toConfigurationProperties();

    assertTrue(config.isAllowAnonymous());
    assertFalse(config.getMap().containsKey("allowAnonymous"));
    assertTrue(roundTrip.getBooleanProperty("allowAnonymous", false));
    assertEquals("PublicAuthConfig", roundTrip.getProperty("default"));
    assertEquals("UsernamePasswordLoginModule", roundTrip.getProperty("usernamePassword"));
  }

  private SecurityManagerConfig createConfig(ConfigurationProperties properties) throws Exception {
    Constructor<SecurityManagerConfig> constructor = SecurityManagerConfig.class.getDeclaredConstructor(ConfigurationProperties.class);
    constructor.setAccessible(true);
    return constructor.newInstance(properties);
  }
}
