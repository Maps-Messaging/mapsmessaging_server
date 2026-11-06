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

package io.mapsmessaging.dto.rest.config.network.impl;

import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.network.KeyStoreConfigDTO;
import io.mapsmessaging.memory.security.DataProtection;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = false)
@ToString(exclude = {"keyStore", "trustStore", "keyPassword"})
@Schema(description = "Optional mutually authenticated Ed25519 memory transport security; HMAC provides integrity only")
public class MemorySecurityConfigDTO extends BaseConfigDTO {
  protected boolean enabled;
  protected String identity;
  protected KeyStoreConfigDTO keyStore;
  protected KeyStoreConfigDTO trustStore;
  protected String keyPassword;
  protected Map<String, List<String>> trustedPeers = new LinkedHashMap<>();
  protected DataProtection protection = DataProtection.AES_256_GCM;
}
