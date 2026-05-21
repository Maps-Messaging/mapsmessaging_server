/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
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

package io.mapsmessaging.test;

import io.mapsmessaging.BaseTest;
import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.api.MessageListener;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SessionContextBuilder;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.auth.AuthManager;
import io.mapsmessaging.auth.ServerPermissions;
import io.mapsmessaging.auth.priviliges.SessionPrivileges;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.destination.DestinationManagerListener;
import io.mapsmessaging.engine.destination.subscription.SubscriptionController;
import io.mapsmessaging.engine.session.FakeProtocol;
import io.mapsmessaging.engine.session.SessionImpl;
import io.mapsmessaging.engine.session.SessionManager;
import io.mapsmessaging.engine.session.SessionManagerTest;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import io.mapsmessaging.network.ProtocolClientConnection;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.security.access.Group;
import io.mapsmessaging.security.access.Identity;
import io.mapsmessaging.security.authorisation.ProtectedResource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Timeout;

import javax.security.auth.login.LoginException;

@Timeout(value = 240000, unit = TimeUnit.MILLISECONDS)
public class BaseTestConfig extends BaseTest {

  private static final String[] EXCLUDE_LIST = {"/aggregator", "/vcan0", "/mavlink","/semtech"};
  private static final String[] USERNAMES = {"user1", "admin", "user2", "anonymous"};
  private static final char[][] PASSWORDS = {"password1".toCharArray(), "admin1".toCharArray(), "password2".toCharArray(), "".toCharArray()};
  private static final String[] GROUPS = {"everyone"};

  @BeforeAll
  static void setUp() {
  }


  protected static MessageDaemon md = null;
  private static Thread th;

  private static Map<String, String> usernamePasswordMap = null;

  static{
    Runtime.getRuntime().addShutdownHook(new TestExitHandler());
  }

  @AfterEach
  void clear(){
    deleteUnknownDestinations();
  }

  @BeforeAll
  static void beforeAll() throws IOException {
    if(md == null) {
      File file = new File(".");
      System.out.println(file.getAbsolutePath());
      File jaasConf = new File(file, "conf");

      setIfNot(
          "java.security.auth.login.config",
          "src"
              + File.separator
              + "test"
              + File.separator
              + "resources"
              +File.separator+"jaasAuth.config");
      //
      // Setup Certificate
      //
      setIfNot("javax.net.ssl.keyStore","my-keystore.jks");
      setIfNot("javax.net.ssl.keyStorePassword","password");
      setIfNot("javax.net.ssl.trustStore","my-truststore.jks");
      setIfNot("javax.net.ssl.trustStorePassword","password");
      setIfNot("javax.net.debug", "none");

      setIfNot("org.slf4j.simpleLogger.defaultLogLevel", "debug");
      md = new MessageDaemon(new TestFeatureManager(new ArrayList<>()));
      Runnable runnable = () -> {
        try {
          md.start();
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      };
      th = new Thread(runnable);
      th.start();
      try {
        th.join();
        if(AuthManager.getInstance().isAuthorisationEnabled()) {
          Group group = AuthManager.getInstance().getGroupIdentity("everyone");
          for (int i = 0; i < USERNAMES.length; i++) {
            if (AuthManager.getInstance().getUserIdentity(USERNAMES[i]) == null) {
              AuthManager.getInstance().addUser(USERNAMES[i], PASSWORDS[i], SessionPrivileges.create(USERNAMES[i]), GROUPS);
            }
            AuthManager.getInstance().addUserToGroup(USERNAMES[i], group.getName());
          }
          Identity identity = AuthManager.getInstance().getUserIdentity("anonymous");
          ProtectedResource resource = new ProtectedResource(DestinationType.TOPIC.getName(), "test/nosubscribe", null);
          AuthManager.getInstance().deny(identity, ServerPermissions.SUBSCRIBE, resource);
        }

      } catch (InterruptedException e) {
        // We don't really care, this is a test
        Thread.currentThread().interrupt();
      }
    }
  }


  public Session createSession(String name, int keepAlive, int expiry, boolean persistent, MessageListener listener) throws LoginException, IOException {
    return createSession(name, keepAlive, expiry, persistent, listener, false);
  }

  public Session createSession(String name, int keepAlive, int expiry, boolean persistent, MessageListener listener, boolean resetState) throws LoginException, IOException {
    Protocol fakeProtocol = new FakeProtocol(listener);
    SessionContextBuilder scb = new SessionContextBuilder(name, new ProtocolClientConnection(fakeProtocol));
    scb.setPersistentSession(true)
        .setPersistentSession(persistent)
        .setResetState(resetState)
        .setReceiveMaximum(100)
        .setSessionExpiry(expiry);
    return createSession(scb, fakeProtocol);
  }

  public Session createSession(SessionContextBuilder scb, MessageListener listener) throws LoginException, IOException {
    Session session = io.mapsmessaging.api.SessionManager.getInstance().create(scb.build(), listener);
    session.login();
    session.resumeState();
    return session;
  }

  public void close(Session session) throws IOException {
    io.mapsmessaging.api.SessionManager.getInstance().close(session, false);
  }


  private static void setIfNot(String key, String value){
    if(System.getProperty(key) == null){
      System.setProperty(key, value);
    }
  }

  @AfterEach
  void checkSessionState()  {
    try {
      deleteUnknownDestinations();
    }
    catch (Exception ex){
      ex.printStackTrace();
    }
  }

  public static String getPassword(String user) throws IOException {
    if (usernamePasswordMap == null) {
      if (md != null && md.isStarted() && AuthManager.getInstance().isAuthenticationEnabled()) {
        ConfigurationProperties properties = new ConfigurationProperties(AuthManager.getInstance().getConfig().getAuthConfig());
        String path = properties.getProperty("configDirectory");
        usernamePasswordMap = Files.lines(Paths.get(path + File.separator + "admin_password"))
            .map(line -> line.split("="))
            .collect(Collectors.toMap(arr -> arr[0], arr -> arr[1]));

      }
    }
    if (usernamePasswordMap != null) {
      return usernamePasswordMap.get(user);
    }
    return "";
  }

  private static final class TestExitHandler extends Thread {

    @Override
    public void run() {
      md.stop();
      try {
        th.join(2000);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }

  private void deleteUnknownDestinations(){
    Map<String, DestinationImpl> destinations = md.getDestinationManager().get(null);
    List<DestinationImpl> toDelete = new ArrayList<>();
    for(DestinationImpl destination:destinations.values()){
      boolean allow = true;
      for(String exclude:EXCLUDE_LIST){
        if(destination.getFullyQualifiedNamespace().startsWith(exclude)){
          allow = false;
        }
      }
      if(allow){
        toDelete.add(destination);
      }
    }
    for(DestinationImpl destination:toDelete){
      md.getDestinationManager().delete(destination);
    }
  }

  private static final AtomicInteger destinationInc = new AtomicInteger(0);

  protected String getTopicName(){
    return "/topic/topic"+destinationInc.incrementAndGet();
  }

  protected String getQueueName(){
    return "/queue/queue"+destinationInc.incrementAndGet();
  }

}
