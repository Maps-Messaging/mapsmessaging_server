package io.mapsmessaging.engine.session;

import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.engine.session.persistence.SessionDetails;
import io.mapsmessaging.engine.session.security.SecurityContext;
import io.mapsmessaging.security.access.Identity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PersistentSessionManagerCoverageTest {

  @TempDir
  Path tempDir;

  @Test
  void constructorCreatesSessionsDirectory() {
    PersistentSessionManager manager = manager();
    assertTrue(Files.isDirectory(Path.of(manager.getDataPath())));
    assertEquals(tempDir.resolve("sessions").toString(), manager.getDataPath());
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "session-a", "session-b", "session-c", "session-d",
      "session-e", "session-f", "session-g", "session-h"
  })
  void getSessionDetailsCreatesAndCachesBySessionId(String id) {
    PersistentSessionManager manager = manager();
    SessionContext context = context(id, "unique-" + id, id.hashCode(), 60);

    SessionDetails first = manager.getSessionDetails(context);
    SessionDetails second = manager.getSessionDetails(context);

    assertSame(first, second);
    assertEquals(id, first.getSessionName());
    assertEquals("unique-" + id, first.getUniqueId());
    assertEquals(id.hashCode(), first.getInternalUnqueId());
    assertSame(first, manager.getSessionDetails(id));
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "identity-a", "identity-b", "identity-c", "identity-d",
      "identity-e", "identity-f"
  })
  void getSessionDetailsCopiesIdentityFromSecurityContext(String id) {
    PersistentSessionManager manager = manager();
    SessionContext context = context(id, "unique-" + id, 77L, 30L);
    SecurityContext securityContext = mock(SecurityContext.class);
    Identity identity = mock(Identity.class);
    when(securityContext.getIdentity()).thenReturn(identity);
    context.setSecurityContext(securityContext);

    SessionDetails details = manager.getSessionDetails(context);

    assertSame(identity, details.getIdentity());
  }

  @Test
  void removeSessionDetailsRemovesStoredEntry() {
    PersistentSessionManager manager = manager();
    SessionDetails details =
        manager.getSessionDetails(context("remove-me", "unique-remove", 44L, 30L));

    assertSame(details, manager.removeSessionDetails("remove-me"));
    assertNull(manager.getSessionDetails("remove-me"));
    assertNull(manager.removeSessionDetails("remove-me"));
  }

  @Test
  void persistentFlagControlsStoredSubscriptionLookup() {
    PersistentSessionManager manager = manager();
    SessionDetails details =
        manager.getSessionDetails(context("persistent", "unique", 12L, 30L));
    SubscriptionContext subscription = new SubscriptionContext("sensor/temp");
    subscription.setAlias("temperature");
    details.getSubscriptionContextList().add(subscription);

    Map<String, SubscriptionContext> persistent =
        manager.getSubscriptionContextMap("persistent", true);
    Map<String, SubscriptionContext> transientMap =
        manager.getSubscriptionContextMap("persistent", false);

    assertSame(subscription, persistent.get("temperature"));
    assertTrue(transientMap.isEmpty());
  }

  @Test
  void loadStateRestoresCurrentSessionFiles() throws Exception {
    Path sessions = tempDir.resolve("sessions");
    Files.createDirectories(sessions);

    for (int i = 0; i < 12; i++) {
      String session = "loaded-" + i;
      SessionDetails details = new SessionDetails(session, "unique-" + i, i + 100L, 120L);
      SubscriptionContext context = new SubscriptionContext("sensor/" + i);
      context.setAlias("alias-" + i);
      details.getSubscriptionContextList().add(context);
      save(details, sessions.resolve(session + ".bin"));
    }

    PersistentSessionManager manager = manager();

    assertEquals(12, manager.getSessionNames().size());
    for (int i = 0; i < 12; i++) {
      SessionDetails loaded = manager.getSessionDetails("loaded-" + i);
      assertNotNull(loaded);
      assertEquals("unique-" + i, loaded.getUniqueId());
      assertEquals(
          "sensor/" + i,
          loaded.getSubscriptionContextMap().get("alias-" + i).getFilter());
    }
  }

  @Test
  void loadStateDeletesFilesWithoutSubscriptions() throws Exception {
    Path sessions = tempDir.resolve("sessions");
    Files.createDirectories(sessions);

    for (int i = 0; i < 6; i++) {
      String session = "empty-" + i;
      save(
          new SessionDetails(session, "unique-" + i, i, 60L),
          sessions.resolve(session + ".bin"));
    }

    PersistentSessionManager manager = manager();

    assertTrue(manager.getSessionNames().isEmpty());
    for (int i = 0; i < 6; i++) {
      assertFalse(Files.exists(sessions.resolve("empty-" + i + ".bin")));
    }
  }

  @Test
  void loadStateIgnoresUnrelatedEntries() throws Exception {
    Path sessions = tempDir.resolve("sessions");
    Files.createDirectories(sessions);
    Files.writeString(sessions.resolve("notes.txt"), "not a session");
    Files.createDirectories(sessions.resolve("directory.bin"));

    PersistentSessionManager manager = manager();

    assertTrue(manager.getSessionNames().isEmpty());
    assertTrue(Files.exists(sessions.resolve("notes.txt")));
    assertTrue(Files.isDirectory(sessions.resolve("directory.bin")));
  }

  private PersistentSessionManager manager() {
    return new PersistentSessionManager(tempDir.toString() + File.separator);
  }

  private SessionContext context(
      String id, String uniqueId, long internalSessionId, long expirySeconds) {
    SessionContext context = new SessionContext(id, mock(ClientConnection.class));
    context.setUniqueId(uniqueId);
    context.setInternalSessionId(internalSessionId);
    context.setExpiry(expirySeconds);
    return context;
  }

  private void save(SessionDetails details, Path path) throws Exception {
    try (FileOutputStream out = new FileOutputStream(path.toFile())) {
      details.save(out);
    }
  }
}
