/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
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

package io.mapsmessaging.config.protocol.impl;

import io.mapsmessaging.config.Config;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.impl.InmarsatDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class InmarsatConfig extends InmarsatDTO implements Config {

  public InmarsatConfig(ConfigurationProperties config) {
    setType("inmarsat");
    ProtocolConfigFactory.unpack(config, this);

    baseUrl = config.getProperty("baseUrl", "https://apis.inmarsat.com/v1/");
    clientId = config.getProperty("clientId");
    clientSecret = config.getProperty("clientSecret");
    pollInterval = config.getIntProperty("pollInterval", 10);
    httpRequestTimeout = config.getIntProperty("httpRequestTimeoutSec", 30);

    // Load mailboxes[i].*
    List<MailboxConfig> boxes = new ArrayList<>();
    for (int i = 0; ; i++) {
      String id = config.getProperty("mailboxes[" + i + "].id", null);
      if (id == null || id.isBlank()) break;
      String pw = config.getProperty("mailboxes[" + i + "].password", "");
      MailboxConfig mb = new MailboxConfig();
      mb.setId(id);
      mb.setPassword(pw);
      boxes.add(mb);
    }
    this.mailboxes = boxes;
  }

  @Override
  public boolean update(BaseConfigDTO cfg) {
    boolean changed = false;
    if (cfg instanceof InmarsatDTO dto) {
      changed |= ProtocolConfigFactory.update(this, dto);

      if (!Objects.equals(baseUrl, dto.getBaseUrl())) {
        baseUrl = dto.getBaseUrl();
        changed = true;
      }
      if (!Objects.equals(clientId, dto.getClientId())) {
        clientId = dto.getClientId();
        changed = true;
      }
      if (!Objects.equals(clientSecret, dto.getClientSecret())) {
        clientSecret = dto.getClientSecret();
        changed = true;
      }
      if (pollInterval != dto.getPollInterval()) {
        pollInterval = dto.getPollInterval();
        changed = true;
      }
      if (httpRequestTimeout != dto.getHttpRequestTimeout()) {
        httpRequestTimeout = dto.getHttpRequestTimeout();
        changed = true;
      }
      if (!equalMailboxes(this.mailboxes, dto.getMailboxes())) {
        this.mailboxes = dto.getMailboxes() == null ? List.of() : new ArrayList<>(dto.getMailboxes());
        changed = true;
      }
    }
    return changed;
  }

  @Override
  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties p = new ConfigurationProperties();
    ProtocolConfigFactory.pack(p, this);
    p.put("baseUrl", baseUrl);
    p.put("clientId", clientId);
    p.put("clientSecret", clientSecret);
    p.put("pollInterval", pollInterval);
    p.put("httpRequestTimeoutSec", httpRequestTimeout);

    // Write mailboxes[i].*
    if (mailboxes != null) {
      for (int i = 0; i < mailboxes.size(); i++) {
        MailboxConfig mb = mailboxes.get(i);
        p.put("mailboxes[" + i + "].id", mb.getId());
        p.put("mailboxes[" + i + "].password", mb.getPassword());
      }
    }
    return p;
  }

  private static boolean equalMailboxes(List<MailboxConfig> a, List<MailboxConfig> b) {
    if (a == b) return true;
    if (a == null || b == null) return false;
    if (a.size() != b.size()) return false;
    for (int i = 0; i < a.size(); i++) {
      MailboxConfig x = a.get(i);
      MailboxConfig y = b.get(i);
      if (!Objects.equals(x.getId(), y.getId())) return false;
      if (!Objects.equals(x.getPassword(), y.getPassword())) return false;
    }
    return true;
  }
}
