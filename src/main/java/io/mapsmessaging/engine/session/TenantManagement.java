package io.mapsmessaging.engine.session;

import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.network.protocol.ProtocolImpl;
import io.mapsmessaging.utilities.configuration.ConfigurationManager;
import io.mapsmessaging.utilities.configuration.ConfigurationProperties;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

public class TenantManagement {

  private static final String CONFIG_KEY_NAME = "scope";
  private static final String USER_TOKEN = "{user}";
  private static final String PROTOCOL_TOKEN = "{protocol}";
  private static final TenantManagement instance = new TenantManagement();

  public static SessionTenantConfig build(ProtocolImpl protocol, SecurityContext securityContext) {
    return instance.create(protocol, securityContext);
  }


  private final Logger logger = LoggerFactory.getLogger(TenantManagement.class);
  private final List<NamespaceMapper> mappers;
  private final Map<String, List<ConfigurationProperties>> configuration;

  private TenantManagement() {
    ConfigurationProperties rawConfig = ConfigurationManager.getInstance().getProperties("TenantManagement");
    configuration = new LinkedHashMap<>();
    List<ConfigurationProperties> config = (List<ConfigurationProperties>) rawConfig.get("data");
    if (config != null) {
      for (ConfigurationProperties configurationProperties : config) {
        if (configurationProperties.containsKey(CONFIG_KEY_NAME)) {
          String type = configurationProperties.getProperty(CONFIG_KEY_NAME);
          List<ConfigurationProperties> existing = configuration.computeIfAbsent(type, k -> new ArrayList<>());
          existing.add(configurationProperties);
        }
      }
    }
    mappers = new ArrayList<>();
    mappers.add(new UsernameMapper());
    mappers.add(new ProtocolMapper());
  }

  private @NotNull @NonNull SessionTenantConfig create(ProtocolImpl protocol, SecurityContext securityContext) {
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
      tenantPath = mapper.reMap(tenantPath, protocol, securityContext);
    }
    logger.log(ServerLogMessages.NAMESPACE_MAPPING, username, tenantPath);
    if (tenantPath.length() > 1 && !tenantPath.endsWith("/")) {
      tenantPath = tenantPath + File.separator;
    }
    return new SessionTenantConfig(tenantPath, configuration.get("global"));
  }

  private @NonNull @NotNull String configurationLookup(String username) {
    String conf;
    ConfigurationProperties userConfig = locateUserConfig(username);
    if (userConfig == null) {
      conf = "";
    } else {
      conf = userConfig.getProperty("namespaceRoot", "");
      logger.log(ServerLogMessages.NAMESPACE_MAPPING_FOUND, username, conf);
    }
    return conf;
  }

  private ConfigurationProperties locateUserConfig(String username) {
    ConfigurationProperties defConfig = null;
    List<ConfigurationProperties> configurationList = configuration.get("user");
    if (configurationList != null) {
      for (ConfigurationProperties config : configurationList) {
        String name = config.getProperty("name", "");
        if (name.equals(username)) {
          return config;
        } else if (name.equals("default")) {
          defConfig = config; // keep going, we may find a hit on the user
        }
      }
    }
    return defConfig;
  }

  private abstract static class NamespaceMapper {

    protected final String lookupString;

    public NamespaceMapper(String lookupString) {
      this.lookupString = lookupString;
    }

    public String reMap(String namespace, ProtocolImpl protocol, SecurityContext securityContext) {
      String response = namespace.replace(lookupString, getData(protocol, securityContext));
      return response.replace("\"", ""); // Remove any "
    }

    protected abstract String getData(ProtocolImpl protocol, SecurityContext securityContext);
  }

  private static class UsernameMapper extends NamespaceMapper {

    public UsernameMapper() {
      super(USER_TOKEN);
    }

    @Override
    protected String getData(ProtocolImpl protocol, SecurityContext securityContext) {
      String user = securityContext.getUsername();
      if (user == null) {
        user = "anonymous";
      }
      return user;
    }
  }

  private static class ProtocolMapper extends NamespaceMapper {

    public ProtocolMapper() {
      super(PROTOCOL_TOKEN);
    }

    @Override
    protected String getData(ProtocolImpl protocol, SecurityContext securityContext) {
      return protocol.getName();
    }
  }

}
