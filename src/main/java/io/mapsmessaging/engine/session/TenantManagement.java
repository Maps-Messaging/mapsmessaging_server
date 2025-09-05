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

import io.mapsmessaging.config.TenantManagementConfig;
import io.mapsmessaging.dto.rest.config.tenant.TenantConfigDTO;
import io.mapsmessaging.engine.session.security.SecurityContext;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The `TenantManagement` class which is responsible for managing tenants in a MAPS instance.
 * It provides a method `build` that creates a `SessionTenantConfig` object based on the provided `ClientConnection` and `SecurityContext`.
 * The `SessionTenantConfig` object contains information about the tenant's configuration and namespace.
 *
 * The class has a private constructor and follows the singleton design pattern, ensuring that only one instance of `TenantManagement` can exist.
 *
 * The class has a list of `NamespaceMapper` objects that are used to map the namespace based on different criteria, such as username and protocol.
 *
 * The `create` method takes a `ClientConnection` and `SecurityContext` as parameters and uses the configured mappers to determine the tenant's namespace.
 * It then creates a `SessionTenantConfig` object with the calculated namespace and the global configuration properties.
 *
 * The `configurationLookup` method is used to lookup the configuration for a given username. It searches for a matching user configuration and falls back to the default configuration if no match is found.
 *
 * The `locateUserConfig` method is used to locate the user configuration based on the provided username. It iterates over the list of user configurations and returns the matching configuration or the default configuration if no match is found.
 *
 * The `NamespaceMapper` abstract class and its subclasses (`UsernameMapper` and `ProtocolMapper`) are used to perform the mapping of the namespace based on different criteria.
 * The `reMap` method in each subclass replaces the lookup string with the corresponding data obtained from the `ClientConnection` and `SecurityContext`.
 *
 * Overall, the `TenantManagement` class provides a flexible way to manage tenants and their configurations in a messaging system.
 */
public class TenantManagement {

  private static final String USER_TOKEN = "{user}";
  private static final String PROTOCOL_TOKEN = "{protocol}";
  private static final TenantManagement instance = new TenantManagement();

  /**
   * Builds a {@link SessionTenantConfig} object based on the provided {@link ClientConnection} and {@link SecurityContext}.
   *
   * This method uses the singleton instance of {@link TenantManagement} to create the {@link SessionTenantConfig} object.
   * The {@link SessionTenantConfig} object contains information about the tenant's configuration and namespace.
   *
   * @param clientConnection The {@link ClientConnection} object representing the client connection.
   * @param securityContext The {@link SecurityContext} object representing the security context.
   * @return The {@link SessionTenantConfig} object representing the tenant's configuration and namespace.
   */
  public static SessionTenantConfig build(ClientConnection clientConnection, SecurityContext securityContext) {
    return instance.create(clientConnection, securityContext);
  }


  private final Logger logger = LoggerFactory.getLogger(TenantManagement.class);
  private final List<NamespaceMapper> mappers;
  private final Map<String, List<TenantConfigDTO>> configuration;

  /**
   * Initializes the TenantManagement class by loading the configuration properties and creating the list of namespace mappers.
   * The configuration properties are retrieved using the ConfigurationManager.getInstance().getProperties() method with the "TenantManagement" key.
   * The retrieved properties are then processed to create a map of configurations based on the "scope" property.
   * The list of namespace mappers is created and populated with instances of the UsernameMapper and ProtocolMapper classes.
   */
  private TenantManagement() {
    TenantManagementConfig config = TenantManagementConfig.getInstance();
    configuration = new LinkedHashMap<>();
    for(TenantConfigDTO tenantConfig:config.getTenantConfigList()){
      if(tenantConfig.getScope() != null){
        String type = tenantConfig.getScope();
        List<TenantConfigDTO> existing = configuration.computeIfAbsent(type, k -> new ArrayList<>());
        existing.add(tenantConfig);
      }
    }
    mappers = new ArrayList<>();
    mappers.add(new UsernameMapper());
    mappers.add(new ProtocolMapper());
  }

  /**
   * Creates a {@link SessionTenantConfig} object based on the provided {@link ClientConnection} and {@link SecurityContext}.
   *
   * The username is used as the key to lookup the configuration. If the username is null, the default value "anonymous" is used.
   *
   * The method performs a lookup based on the username to retrieve the tenant's namespace configuration. If no configuration is found, the "default" value is used.
   *
   * The method then applies the configured mappers to the tenant's namespace, using the {@link ClientConnection} and {@link SecurityContext} objects.
   *
   * The resulting tenant path is logged using the {@link Logger} with the {@link ServerLogMessages.NAMESPACE_MAPPING} message.
   *
   * If the tenant path is longer than 1 character and does not end with a "/", a file separator is appended to the path.
   *
   * Finally, a new {@link SessionTenantConfig} object is created with the calculated tenant path and the global configuration properties.
   *
   * @param clientConnection The {@link ClientConnection} object representing the client connection.
   * @param securityContext The {@link SecurityContext} object representing the security context.
   * @return The {@link SessionTenantConfig} object representing the tenant's configuration and namespace.
   */
  private @NotNull @NonNull SessionTenantConfig create(ClientConnection clientConnection, SecurityContext securityContext) {
    //
    // The username is the key to the configuration, once we have that we can
    // do a look-up based on the username, if not found then we can look for the "default" value
    //
    String username = securityContext.getUsername();
    if (username == null) {
      username = "anonymous";
    }

    // Lookup the mapping by username
    String tenantPath = configurationLookup(username);

    for (NamespaceMapper mapper : mappers) {
      tenantPath = mapper.reMap(tenantPath, clientConnection, securityContext);
    }
    logger.log(ServerLogMessages.NAMESPACE_MAPPING, username, tenantPath);
    if (tenantPath.length() > 1 && !tenantPath.endsWith("/")) {
      tenantPath = tenantPath + File.separator;
    }
    return new SessionTenantConfig(tenantPath, configuration.get("global"));
  }

  /**
   * Retrieves the configuration for the given username.
   *
   * @param username The username for which to retrieve the configuration.
   * @return The configuration for the given username, or an empty string if no configuration is found.
   */
  private @NonNull @NotNull String configurationLookup(String username) {
    String conf;
    TenantConfigDTO userConfig = locateUserConfig(username);
    if (userConfig == null) {
      conf = "";
    } else {
      conf = userConfig.getNamespaceRoot();
      logger.log(ServerLogMessages.NAMESPACE_MAPPING_FOUND, username, conf);
    }
    return conf;
  }

  /**
   * Retrieves the configuration for the given username.
   *
   * This method searches for the configuration properties associated with the given username in the 'user' configuration list.
   * If a configuration with a matching username is found, it is returned.
   * If no configuration with a matching username is found, the method returns the configuration with the username 'default' if it exists.
   * If no matching configuration is found, the method returns null.
   *
   * @param username The username for which to retrieve the configuration.
   * @return The configuration for the given username, or null if no configuration is found.
   */
  private TenantConfigDTO locateUserConfig(String username) {
    TenantConfigDTO defConfig = null;
    List<TenantConfigDTO> configurationList = configuration.get("user");
    if (configurationList != null) {
      for (TenantConfigDTO config : configurationList) {
        String name = config.getName();
        if (name.equals(username)) {
          return config;
        } else if (name.equals("default")) {
          defConfig = config; // keep going, we may find a hit on the user
        }
      }
    }
    return defConfig;
  }

  /**
   * The NamespaceMapper class is an abstract class that provides a mapping mechanism for namespaces.
   * It allows for the remapping of namespaces based on a lookup string and provides a method to retrieve the remapped data.
   *
   * The lookupString parameter is used to identify the portion of the namespace that needs to be replaced.
   *
   * The reMap method takes in a namespace, a ClientConnection object, and a SecurityContext object as parameters.
   * It replaces the lookupString in the namespace with the remapped data obtained from the getData method.
   * It then removes any double quotes from the resulting remapped namespace.
   *
   * The getData method is an abstract method that needs to be implemented by subclasses.
   * It takes in a ClientConnection object and a SecurityContext object as parameters and returns the remapped data.
   *
   * This class is meant to be extended by concrete implementations that provide specific remapping logic.
   *
   * Example usage:
   *
   * NamespaceMapper mapper = new CustomNamespaceMapper("lookupString");
   * String remappedNamespace = mapper.reMap(namespace, clientConnection, securityContext);
   *
   * The remappedNamespace will contain the namespace with the lookupString replaced by the remapped data.
   *
   * Note: This class is private and can only be accessed within the io.mapsmessaging.engine.session package.
   */
  private abstract static class NamespaceMapper {

    protected final String lookupString;

    public NamespaceMapper(String lookupString) {
      this.lookupString = lookupString;
    }

    public String reMap(String namespace, ClientConnection clientConnection, SecurityContext securityContext) {
      String response = namespace.replace(lookupString, getData(clientConnection, securityContext));
      return response.replace("\"", ""); // Remove any "
    }

    protected abstract String getData(ClientConnection clientConnection, SecurityContext securityContext);
  }

  /**
   * The `UsernameMapper` class is a concrete implementation of the abstract `NamespaceMapper` class.
   * It provides a mapping mechanism for namespaces based on the username obtained from the `SecurityContext`.
   *
   * The `UsernameMapper` constructor initializes the `lookupString` with the value of the `USER_TOKEN` constant.
   *
   * The `getData` method takes a `ClientConnection` object and a `SecurityContext` object as parameters and returns the username obtained from the `SecurityContext`.
   * If the username is null, the method returns the string "anonymous".
   *
   * The `reMap` method overrides the abstract method in the parent class and replaces the `lookupString` in the namespace with the username obtained from the `SecurityContext`.
   * It then removes any double quotes from the resulting remapped namespace.
   *
   * Example usage:
   *
   * UsernameMapper mapper = new UsernameMapper();
   * String remappedNamespace = mapper.reMap(namespace, clientConnection, securityContext);
   *
   * The `remappedNamespace` will contain the namespace with the `lookupString` replaced by the username obtained from the `SecurityContext`.
   *
   * Note: This class is a private static nested class and can only be accessed within the `io.mapsmessaging.engine.session` package.
   */
  private static class UsernameMapper extends NamespaceMapper {

    public UsernameMapper() {
      super(USER_TOKEN);
    }

    @Override
    protected String getData(ClientConnection clientConnection, SecurityContext securityContext) {
      String user = securityContext.getUsername();
      if (user == null) {
        user = "anonymous";
      }
      return user;
    }
  }

  /**
   * The ProtocolMapper class is a subclass of NamespaceMapper that is used to map the protocol
   * of a client connection to a namespace. It replaces the PROTOCOL_TOKEN in the namespace with
   * the name of the client connection.
   *
   * This class provides the implementation for the getData method, which returns the name of the
   * client connection.
   *
   * Example usage:
   * ProtocolMapper mapper = new ProtocolMapper();
   * String namespace = mapper.reMap(namespace, clientConnection, securityContext);
   *
   * @see NamespaceMapper
   */
  private static class ProtocolMapper extends NamespaceMapper {

    public ProtocolMapper() {
      super(PROTOCOL_TOKEN);
    }

    @Override
    protected String getData(ClientConnection clientConnection, SecurityContext securityContext) {
      return clientConnection.getName();
    }
  }

}
