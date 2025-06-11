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

package io.mapsmessaging.auth;

import io.mapsmessaging.auth.priviliges.SessionPrivileges;
import io.mapsmessaging.auth.priviliges.subscription.SubscriptionPrivileges;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.security.Principal;

@ToString
@EqualsAndHashCode
public class QuotaPrincipal implements Principal {

  @Getter
  private final SessionPrivileges sessionPrivileges;

  @Getter
  private final SubscriptionPrivileges subscriptionPrivileges;


  public QuotaPrincipal(SessionPrivileges sessionPrivileges, SubscriptionPrivileges subscriptionPrivileges) {
    this.sessionPrivileges = sessionPrivileges;
    this.subscriptionPrivileges = subscriptionPrivileges;
  }

  @Override
  public String getName() {
    return "QuotaPrincipal";
  }
}
