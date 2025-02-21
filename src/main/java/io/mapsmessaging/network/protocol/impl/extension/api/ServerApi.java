package io.mapsmessaging.network.protocol.impl.extension.api;

import io.mapsmessaging.network.protocol.impl.extension.ExtensionProtocol;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ServerApi {

  private final Map<String, SessionContext> sessions;

  public ServerApi() {
    sessions = new ConcurrentHashMap<>();
  }

  public void close(){
    for(SessionContext session : sessions.values()){
      try {
        session.close();
      } catch (IOException e) {
        // To Do
      }
      }
  }

  public Subject getSubject(){
    sessions.values().stream().findFirst().ifPresent(session -> session.getSubject());
    return null;
  }

  public void closeSession(@NonNull @NotNull SessionContext session) throws IOException {
    if(sessions.containsKey(session.getSessionId())){
      sessions.remove(session.getSessionId());
    }
    session.close();
  }

  public SessionContext createSession(@NonNull @NotNull ExtensionProtocol extension, @NonNull @NotNull String sessionId, @Nullable String username, @Nullable String password) throws IOException {
    try {
      if(sessions.containsKey(sessionId)) {
        return sessions.get(sessionId);
      }
      SessionContext session = new SessionContext(extension, sessionId, username, password);
      sessions.put(sessionId, session);
      return session;
    } catch (LoginException e) {
      throw new IOException("Failed to create session", e);
    }
  }

}
