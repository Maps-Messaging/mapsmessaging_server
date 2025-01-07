package io.mapsmessaging.rest.handler;

import jakarta.servlet.annotation.WebListener;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import jakarta.servlet.http.HttpSession;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Collection;

@WebListener
public class SessionTracker implements HttpSessionListener {
  private static final ConcurrentHashMap<String, HttpSession> sessions = new ConcurrentHashMap<>();

  @Override
  public void sessionCreated(HttpSessionEvent se) {
    HttpSession session = se.getSession();
    sessions.put(session.getId(), session);
  }

  @Override
  public void sessionDestroyed(HttpSessionEvent se) {
    sessions.remove(se.getSession().getId());
  }

  public static Collection<HttpSession> getActiveSessions() {
    return sessions.values();
  }
}
