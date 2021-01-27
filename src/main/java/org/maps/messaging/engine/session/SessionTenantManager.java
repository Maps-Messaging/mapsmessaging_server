package org.maps.messaging.engine.session;

import org.jetbrains.annotations.NotNull;
import org.maps.logging.LogMessages;
import org.maps.logging.Logger;
import org.maps.logging.LoggerFactory;
import org.maps.network.protocol.ProtocolImpl;
import org.maps.utilities.configuration.ConfigurationManager;
import org.maps.utilities.configuration.ConfigurationProperties;

public class SessionTenantManager {

  private static final String USER_TOKEN = "{user}";
  private static final String PROTOCOL_TOKEN = "{protocol}";

  private static final Logger logger = LoggerFactory.getLogger(SessionTenantManager.class);

  private final ConfigurationProperties configuration;
  private final ProtocolImpl protocol;
  private final SecurityContext securityContext;
  private String pathMapping;

  public SessionTenantManager(ProtocolImpl protocol, SecurityContext securityContext){
    this.securityContext = securityContext;
    this.protocol = protocol;
    configuration = ConfigurationManager.getInstance().getProperties("TenantManagement");
    pathMapping = null;
  }

  public String getMapping(){
    if(pathMapping == null){
      pathMapping = getPathConfig();
    }
    return pathMapping;
  }

  public String getAbsoluteName(String destinationName){
    return getMapping()+destinationName;
  }

  private String getPathConfig(){
    //
    // We need to be logged in before this occurs
    //
    if(!securityContext.isLoggedIn()){
      return null;
    }

    //
    // The username is the key to the configuration, once we have that we can
    // do a look up based on the username, if not found then we can look for the "default" value
    //
    String username = securityContext.getUsername();
    if(username == null){
      username = "anonymous";
    }

    // Lookup the mapping by username
    String mappingValue = configurationLookup(username);

    mappingValue = replaceToken(mappingValue, USER_TOKEN, username);
    mappingValue = replaceToken(mappingValue, PROTOCOL_TOKEN, protocol.getName());
    logger.log(LogMessages.NAMESPACE_MAPPING, username, mappingValue);
    return mappingValue;
  }

  private @NotNull String configurationLookup(String username){
    String conf = null;
    if(configuration.containsKey(username)) {
      conf = configuration.getProperty(username);
      logger.log(LogMessages.NAMESPACE_MAPPING_FOUND, username, conf);
    }
    if(conf == null){
      conf = configuration.getProperty("default");
      logger.log(LogMessages.NAMESPACE_MAPPING_DEFAULT, conf);
    }
    if(conf == null){
      conf="";
    }
    return conf;
  }


  private String replaceToken(String path, String token, String value){
    String response = path.replace(token, value);
    return response.replace("\"", ""); // Remove any "
  }
}
