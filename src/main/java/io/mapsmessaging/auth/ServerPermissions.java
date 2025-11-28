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
  CONNECT("connect", "Allows the identity to open a connection", 1),
  PERSISTENT_SESSION("persistent_session", "Allows use of persistent/durable sessions", 2),
  PUBLISH_SERVER("publish_server", "Global gate: may publish to any destination", 3),
  SUBSCRIBE_SERVER("subscribe_server", "Global gate: may subscribe to any destination", 4),
  CREATE_DESTINATION("create_destination", "Global gate: may create destinations anywhere", 5),
  RETAIN_SERVER("retain_server", "Global gate: may use retained messages", 6),
  CREATE_DURABLE_SERVER("create_durable_server", "Allows creating durable subscriptions/queues", 7),
  BIND_DURABLE_SERVER("bind_durable_server", "Allows binding to existing durable resources", 8),
  PURGE_SERVER("purge_server", "Allows purging messages globally", 9),
  LIST_DESTINATIONS("list_destinations", "Allows listing all destinations", 10),
  MANAGE_DESTINATIONS("manage_destinations", "Allows managing destination configuration globally", 11),
  VIEW_STATS("view_stats", "Allows viewing server statistics", 12),
  VIEW_CONFIG("view_config", "Allows viewing server configuration", 13),
  MANAGE_CONFIG("manage_config", "Allows modifying server configuration", 14),
  MANAGE_AUTHENTICATION("manage_authentication", "Allows managing authentication", 15),
  MANAGE_AUTHORIZATION("manage_authorization", "Allows managing authorisation rules", 16),
  MANAGE_LICENSE("manage_license", "Allows managing server licensing", 17),
  SCHEMA_PUBLISH("schema_publish", "Allows publishing schema updates", 18),
  SCHEMA_SUBSCRIBE("schema_subscribe", "Allows subscribing to schema updates", 19),
  REST_API_ACCESS("rest_api_access", "Allows access to the server REST management API", 20),
  MANAGE_INTERFACES("manage_interfaces", "Allows managing network interfaces and listeners", 21),
  MANAGE_PROTOCOLS("manage_protocols", "Allows enabling, disabling and configuring protocols", 22),



  PUBLISH("publish", "Allows publishing to this destination", 32),
  SUBSCRIBE("subscribe", "Allows subscribing to this destination", 33),
  CREATE_CHILD("create_child", "Allows creating child destinations under this node", 34),
  DELETE("delete", "Allows deleting this destination", 35),
  RETAIN("retain", "Allows setting retained messages here", 36),
  CREATE_DURABLE("create_durable", "Allows creating durable resources on this destination", 37),
  BIND_DURABLE("bind_durable", "Allows binding to existing durable resources here", 38),
  PURGE("purge", "Allows purging messages from this destination", 39),
  VIEW("view", "Allows viewing/browsing this destination", 40),
  DESTINATION_MANAGE_CONFIG("manage_config_dest", "Allows modifying configuration of this destination", 41),

  ;

  private final String name;
  private final String description;
  private final long mask;

  ServerPermissions(final String name, final String description, final long mask) {
    this.name = name;
    this.description = description;
    this.mask = 1L << mask;
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
    stringBuilder.append("  relations\n\n");

    for (ServerPermissions serverPermission : ServerPermissions.values()) {
      String permissionName = serverPermission.getName();
      stringBuilder
          .append("    define ")
          .append("allow_")
          .append(permissionName)
          .append(": [user, group#member]\n");
      stringBuilder
          .append("    define ")
          .append("deny_")
          .append(permissionName)
          .append(": [user, group#member]\n");
      stringBuilder
          .append("    define ")
          .append(permissionName)
          .append(": allow_")
          .append(permissionName)
          .append(" but not deny_")
          .append(permissionName)
          .append("\n\n");

    }

    return stringBuilder.toString();
  }

  public static void main(String[] args) {
    System.err.println(generateOpenFgaModel());
  }
}
