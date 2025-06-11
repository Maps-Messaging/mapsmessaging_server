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

package io.mapsmessaging.auth.priviliges;

import io.mapsmessaging.api.features.QualityOfService;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@ToString
@EqualsAndHashCode(callSuper = false)
@Getter
@Setter
public class SessionPrivileges extends Privilege {

  public static SessionPrivileges create(String username) {
    return new SessionPrivileges(username);
  }

  private UUID uniqueId;
  private final String username;
  private final List<Privilege> priviliges;

  public SessionPrivileges(UUID id, String username, List<Privilege> priviliges) {
    super("session");
    this.uniqueId = id;
    this.username = username;
    this.priviliges = priviliges;
  }

  public SessionPrivileges(String username) {
    super("session");
    this.username = username;
    priviliges = new ArrayList<>();
    priviliges.add(new BooleanPrivilege("AccessSystemTopics", true));
    priviliges.add(new BooleanPrivilege("ForceReset", false));
    priviliges.add(new BooleanPrivilege("AllowPersistentSession", true));

    priviliges.add(new LongPrivilege("WillDelay", 0)); // 1 Day
    priviliges.add(new LongPrivilege("MaxWillDelay", 86_400_000)); // 1 Day
    priviliges.add(new LongPrivilege("MaxSessionExpiry", 86_400_000)); // 1 Day
    priviliges.add(new LongPrivilege("MaxMessageSize", 1_048_576)); // 1 MB
    priviliges.add(new LongPrivilege("MaxInflightMessages", 1 << 16 - 1));       // 10 outstanding messages

    priviliges.add(new LongPrivilege("MaxPublishQoS", QualityOfService.EXACTLY_ONCE.getLevel()));
    priviliges.add(new LongPrivilege("MaxSubscribeQoS", QualityOfService.EXACTLY_ONCE.getLevel()));

    priviliges.add(new LongPrivilege("PublishPerMinute", Integer.MAX_VALUE));

    priviliges.add(new LongPrivilege("MaxConcurrentPersistentSessions", Short.MAX_VALUE));
    priviliges.add(new LongPrivilege("MaxConcurrentSubscriptions", Short.MAX_VALUE));
    priviliges.add(new LongPrivilege("MaxConcurrentConnections", Short.MAX_VALUE));
    priviliges.add(new LongPrivilege("MaxConcurrentPublishes", Short.MAX_VALUE));

  }
}
