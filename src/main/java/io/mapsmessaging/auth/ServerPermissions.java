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


import io.mapsmessaging.security.authorisation.Permission;
import lombok.Getter;

@Getter
public enum ServerPermissions implements Permission {

  READ("read", "Allows Read access to the resource", 0),
  WRITE("write", "Allows Write access to the resource", 1),
  DELETE("delete", "Allows Delete access to the resource", 2),
  CREATE("create", "Allows Create access to the resource", 3),
  SUBSCRIBE("subscribe", "Allows Subscription access to the topic", 4),
  PUBLISH("publish", "Allows Publish access to the topic", 5),
  PUBLISH_RETAINED("retain", "Allows Retain access to the topic", 6),
  CONNECT("connect", "allows user to connect", 7),
  MANAGE("manage", "allows user to manage resource", 8)
  ;

  private final String name;
  private final String description;
  private final long mask;

  ServerPermissions(final String name, final String description, final long mask) {
    this.name = name;
    this.description = description;
    this.mask = 1L <<mask;
  }

  public static String generateOpenFgaModel() {
    StringBuilder stringBuilder = new StringBuilder();

    stringBuilder.append("model\n");
    stringBuilder.append("  schema 1.1\n");
    stringBuilder.append("\n");
    stringBuilder.append("type user\n");
    stringBuilder.append("\n");
    stringBuilder.append("type group\n");
    stringBuilder.append("  relations\n");
    stringBuilder.append("    define member: [user]\n");
    stringBuilder.append("\n");
    stringBuilder.append("type resource\n");
    stringBuilder.append("  relations\n");

    for (ServerPermissions serverPermission : ServerPermissions.values()) {
      String permissionName = serverPermission.getName();
      stringBuilder
          .append("    define ")
          .append(permissionName)
          .append(": [user, group#member]\n");
    }

    return stringBuilder.toString();
  }

  public static void main(String[] args) {
    System.err.println(generateOpenFgaModel());
  }
}
