package io.mapsmessaging.engine.session;

import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.engine.session.persistence.SessionDetails;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Collectors;
import lombok.Getter;

public class PersistentSessionManager {

  @Getter
  private final String dataPath;

  private final Map<String, SessionDetails> persistentMap;

  public PersistentSessionManager(String dataPath) {
    this.dataPath = dataPath+"sessions";
    File testFile = new File(this.dataPath);
    if(!testFile.exists()){
      try {
        Files.createDirectories(testFile.toPath());
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    persistentMap = new ConcurrentSkipListMap<>();
    loadState();
  }

  public List<String> getSessionNames(){
    return new ArrayList<>(persistentMap.keySet());
  }

  public SessionDetails getSessionDetails(String sessionId){
    return persistentMap.computeIfAbsent(sessionId, k -> new SessionDetails(sessionId, UUID.randomUUID().toString()));
  }

  public Map<String, SubscriptionContext> getSubscriptionContextMap(String sessionId, boolean isPersistent) {
    Map<String, SubscriptionContext> map = new LinkedHashMap<>();
    if (isPersistent) {
       SessionDetails sessionDetails = persistentMap.get(sessionId);
       if(sessionDetails != null){
         return sessionDetails.getSubscriptionContextMap();
       }
     }
    return map;
  }


  private void loadState(){
    File file = new File(dataPath);
    File[] children = file.listFiles();
    if(children != null) {
      List<File> childList = new ArrayList<>(List.of(children));
      childList = childList.stream().filter(file1 -> file1.getName().endsWith(".bin") && !file1.isDirectory()).collect(Collectors.toList());
      for (File child : childList) {
        try (FileInputStream fileInputStream = new FileInputStream(child)) {
          SessionDetails details = new SessionDetails(fileInputStream);
          persistentMap.put(details.getSessionName(), details);
        } catch (IOException ex) {
          ex.printStackTrace();
        }
      }
    }
  }
}
