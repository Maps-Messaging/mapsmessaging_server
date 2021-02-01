package org.maps.messaging.engine.destination;

import org.maps.utilities.configuration.ConfigurationProperties;

public class DestinationPathManager {

  private static final String NAME = "name";
  private static final String NAMESPACE = "namespace";
  private static final String DIRECTORY = "directory";

  private static final String OPTIONAL_PATH = "{folder}";

  private final String name;
  private final String directory;
  private final String namespace;
  private final boolean remap;

  public DestinationPathManager(ConfigurationProperties properties){
    String nam = properties.getProperty(NAMESPACE);
    directory = properties.getProperty(DIRECTORY);
    name = properties.getProperty(NAME);
    remap = (nam.endsWith(OPTIONAL_PATH) && directory.contains(OPTIONAL_PATH));
    if(remap){
      namespace = nam.substring(0, nam.indexOf(OPTIONAL_PATH));
    }
    else{
      namespace = nam;
    }
  }

  public String getName() {
    return name;
  }

  public boolean isRemap() {
    return remap;
  }

  public String getDirectory() {
    return directory;
  }

  public String getNamespace() {
    return namespace;
  }

  public String calculateDirectory(String destinationPath){
    if(remap){
      String response = "";
      String tmp = destinationPath.substring(namespace.length());
      if(tmp.contains("/")){
        String sub = tmp.substring(0, tmp.indexOf("/"));
        response = directory.replace(OPTIONAL_PATH, sub);
      }
      else {
        response = directory.replace(OPTIONAL_PATH, tmp);
      }
      return response;
    }
    return directory;
  }
}
