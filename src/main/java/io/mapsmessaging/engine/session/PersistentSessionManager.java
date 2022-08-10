package io.mapsmessaging.engine.session;

import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.engine.session.persistence.SessionDetails;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import org.yaml.snakeyaml.Yaml;

public class PersistentSessionManager {

  @Getter
  private final String dataPath;

  private final Map<String, SessionDetails> persistentMap;

  public PersistentSessionManager(String dataPath) {
    this.dataPath = dataPath+"sessions";
    persistentMap = new LinkedHashMap<>();
    loadState();
  }

  public List<String> getSessionNames(){
    return new ArrayList<>(persistentMap.keySet());
  }

  public SessionDetails getSessionDetails(String sessionId){
    return persistentMap.computeIfAbsent(sessionId, k -> new SessionDetails(sessionId));
  }

  public Map<String, SubscriptionContext> getSubscriptionContextMap(String sessionId, boolean isPersistent) {
    Map<String, SubscriptionContext> map = new LinkedHashMap<>();
    if (isPersistent) {
       SessionDetails sessionDetails = persistentMap.get(sessionId);
       if(sessionDetails != null){
         for(SubscriptionContext context:sessionDetails.getSubscriptionContextList()){
           map.put(context.getAlias(), context); // Pre-populate with persistent data
         }
       }
     }
    return map;
  }


  private void loadState(){
    Yaml yaml = new Yaml();
    File file = new File(dataPath);
    File[] children = file.listFiles();
    if(children != null) {
      for (File child : children) {
        if (!child.isDirectory()) {
          try (FileInputStream fileInputStream = new FileInputStream(child)) {
            Object obj = yaml.load(fileInputStream);
            if (obj instanceof SessionDetails) {
              SessionDetails details = (SessionDetails) obj;
              persistentMap.put(details.getSessionName(), details);
            }
          } catch (IOException ex) {
            ex.printStackTrace();
          }
        }
      }
    }
  }

}
