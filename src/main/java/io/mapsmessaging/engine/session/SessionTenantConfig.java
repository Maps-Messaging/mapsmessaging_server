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

package io.mapsmessaging.engine.session;

import io.mapsmessaging.dto.rest.config.tenant.TenantConfigDTO;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a session tenant configuration.
 *
 * This class is responsible for managing the configuration properties of a session tenant.
 * It provides methods to calculate the original and destination names based on the tenant path.
 * The tenant path is used to prepend or remove the path from the names.
 *
 * The configuration properties are stored in a global list, which is a tree structure.
 * The global list is populated with the configuration properties provided during initialization.
 *
 * This class also provides a method to check if a given destination name is a global common path used by all users.
 *
 * @author Matthew Buckton
 * @version 1.0
 * @since 2020
 */
public class SessionTenantConfig {

  private final @Getter String tenantPath;
  private final List<String> globalList; // Should be a tree

  /**
   * Constructor for SessionTenantConfig class.
   *
   * @param tenantPath            the path of the tenant
   * @param globalConfiguration   the list of global configuration properties
   */
  public SessionTenantConfig(String tenantPath, List<TenantConfigDTO> globalConfiguration) {
    if (tenantPath.equals("/")) {
      tenantPath = "";
    }
    this.tenantPath = tenantPath;
    globalList = new ArrayList<>();
    if (globalConfiguration != null) {
      for (TenantConfigDTO config : globalConfiguration) {
        String namespace = config.getNamespaceRoot();
        if (namespace != null && namespace.length() > 1) {
          globalList.add(namespace);
        }
      }
    }
  }

  /**
   * Calculates the original name based on the fully qualified name (FQN).
   *
   * If the FQN starts with "$SYS" or is a global common path used by all users, the original name is the same as the FQN.
   * Otherwise, the original name is obtained by removing the tenant path from the FQN.
   * If the original name starts with an underscore, it is replaced with a forward slash ("/").
   *
   * @param fqn the fully qualified name
   * @return the original name
   */
  public String calculateOriginalName(String fqn) {
    if (fqn.startsWith("$SYS") || isGlobal(fqn)) { // This is a global common path used by all users
      return fqn; // Keep the name as it is
    }
    String name = fqn.substring(tenantPath.length());
    if (name.startsWith("_")) {
      name = "/" + name.substring(1);
    }
    return name;
  }

  // Prepends the tenant path to the destination name
  /**
   * Calculates the destination name based on the given destination name.
   *
   * If the destination name starts with "$SYS" or is a global common path used by all users, the destination name remains unchanged.
   * Otherwise, the tenant path is prepended to the destination name.
   * If the tenant path is not empty and the destination name starts with "/", the first character is replaced with an underscore ("_").
   *
   * @param destinationName the destination name
   * @return the calculated destination name
   */
  public String calculateDestinationName(String destinationName) {
    if (destinationName.startsWith("$SYS") || isGlobal(destinationName)) { // This is a global common path used by all users
      return destinationName;
    }
    if (!tenantPath.isEmpty() && destinationName.startsWith("/")) {
      destinationName = "_" + destinationName.substring(1);
    }
    return tenantPath + destinationName;
  }


  /**
   * Checks if the given destination name is a global common path used by all users.
   *
   * This method iterates through the global list of configuration properties and checks if the destination name starts with any of the global paths.
   * If a match is found, it returns true indicating that the destination name is a global common path.
   * If no match is found, it returns false indicating that the destination name is not a global common path.
   *
   * @param destinationName the destination name to be checked
   * @return true if the destination name is a global common path, false otherwise
   */
  private boolean isGlobal(String destinationName) {
    for (String test : globalList) {
      if (destinationName.startsWith(test)) {
        return true;
      }
    }
    return false;
  }
}
