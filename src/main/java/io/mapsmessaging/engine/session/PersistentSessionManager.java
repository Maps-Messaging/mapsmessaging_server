/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.mapsmessaging.engine.session;

import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.engine.session.persistence.SessionDetails;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.security.uuid.UuidGenerator;
import lombok.Getter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Collectors;

import static io.mapsmessaging.logging.ServerLogMessages.SESSION_INIT_ERROR;
import static io.mapsmessaging.logging.ServerLogMessages.SESSION_LOAD_STATE_ERROR;

public class PersistentSessionManager {

  private final Logger logger = LoggerFactory.getLogger(PersistentSessionManager.class);

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
        logger.log(SESSION_INIT_ERROR, this.dataPath);
      }
    }
    persistentMap = new ConcurrentSkipListMap<>();
    loadState();
  }

  public List<String> getSessionNames(){
    return new ArrayList<>(persistentMap.keySet());
  }

  public SessionDetails getSessionDetails(String sessionId){
    return persistentMap.computeIfAbsent(sessionId, k -> new SessionDetails(sessionId, UuidGenerator.getInstance().generate().toString()));
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
          logger.log(SESSION_LOAD_STATE_ERROR, child.getAbsolutePath(), ex);
        }
      }
    }
  }
}
