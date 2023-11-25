/*
 * Copyright [ 2020 - 2023 ] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.mapsmessaging.auth.registry.priviliges.session;

import io.mapsmessaging.auth.registry.priviliges.BooleanPrivilege;
import io.mapsmessaging.auth.registry.priviliges.LongPrivilege;
import io.mapsmessaging.auth.registry.priviliges.Privilege;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class SessionPrivileges {

  public static SessionPrivileges createAdminQuota(String username) {
    return new SessionPrivileges(username);
  }

  private UUID uniqueId;
  private final String username;
  private final List<Privilege> priviliges;

  protected SessionPrivileges(String username) {
    this.username = username;
    priviliges = new ArrayList<>();

    priviliges.add(new BooleanPrivilege("Admin", false));
    priviliges.add(new BooleanPrivilege("AccessSystemTopics", true));
    priviliges.add(new BooleanPrivilege("PublishRetainedMessages", true));
    priviliges.add(new BooleanPrivilege("ForceReset", false));
    priviliges.add(new BooleanPrivilege("AllowPersistentSession", true));

    priviliges.add(new LongPrivilege("MaxWillDelay", 86_400_000)); // 1 Day
    priviliges.add(new LongPrivilege("MaxSessionExpiry", 86_400_000)); // 1 Day
    priviliges.add(new LongPrivilege("MaxMessageSize", 1_048_576)); // 1 MB
    priviliges.add(new LongPrivilege("MaxInflightMessages", 10));       // 10 outstanding messages

    priviliges.add(new LongPrivilege("MaxPublishQoS", 0));
    priviliges.add(new LongPrivilege("MaxSubscribeQoS", 0));

    priviliges.add(new LongPrivilege("PublishPerMinute", 0));

    priviliges.add(new LongPrivilege("MaxConcurrentPersistentSessions", 0));
    priviliges.add(new LongPrivilege("MaxConcurrentSubscriptions", 0));
    priviliges.add(new LongPrivilege("MaxConcurrentConnections", 0));
    priviliges.add(new LongPrivilege("MaxConcurrentPublishes", 0));

  }

}
