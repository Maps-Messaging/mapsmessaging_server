/*
 *
 *   Copyright [ 2020 - 2021 ] [Matthew Buckton]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package io.mapsmessaging.test;

import io.mapsmessaging.BaseTest;
import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.destination.DestinationManagerListener;
import io.mapsmessaging.engine.destination.subscription.SubscriptionController;
import io.mapsmessaging.engine.session.SessionImpl;
import io.mapsmessaging.engine.session.SessionManager;
import io.mapsmessaging.engine.session.SessionManagerTest;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;

public class BaseTestConfig extends BaseTest {

  protected static MessageDaemon md = null;
  private static Thread th;

  static{
    Runtime.getRuntime().addShutdownHook(new TestExitHandler());
  }

  @AfterEach
  public void clear(){
    Map<String, DestinationImpl> destinations = md.getDestinationManager().get(null);
    List<DestinationImpl> toDelete = new ArrayList<>();
    for(DestinationImpl destination:destinations.values()){
      if(!destination.getFullyQualifiedNamespace().startsWith("$")){
        toDelete.add(destination);
      }
    }
    for(DestinationImpl destination:toDelete){
      md.getDestinationManager().delete(destination);
    }
  }

  @BeforeAll
  public static void beforeAll() throws IOException {
    if(md == null) {
      File file = new File("~");
      System.out.println(file.getAbsolutePath());

      //
      // Setup Certificate
      //
      setIfNot("javax.net.ssl.keyStore","my-keystore.jks");
      setIfNot("javax.net.ssl.keyStorePassword","password");
      setIfNot("javax.net.ssl.trustStore","my-truststore.jks");
      setIfNot("javax.net.ssl.trustStorePassword","password");
      setIfNot("javax.net.debug", "none");

      setIfNot("java.security.auth.login.config", "=src/test/resources/jaasAuth.config");
      setIfNot("org.slf4j.simpleLogger.defaultLogLevel", "debug");
      md = new MessageDaemon();
      Runnable runnable = () -> {
        md.start(null);
      };
      th = new Thread(runnable);
      th.start();
      try {
        th.join();
      } catch (InterruptedException e) {
        // We don't really care, this is a test
        Thread.currentThread().interrupt();
      }
    }
  }

  private static void setIfNot(String key, String value){
    if(System.getProperty(key) == null){
      System.setProperty(key, value);
    }
  }

  @AfterEach
  public void checkSessionState()  {
    try {
      SessionManager manager = md.getSessionManager();
      List<SessionImpl> sessionImpls = manager.getSessions();
      for (SessionImpl sessionImpl : sessionImpls) {
        System.err.println("Session still active::" + sessionImpl.getName());
        sessionImpl.setExpiryTime(1);
        manager.close(sessionImpl, false);
      }
      int counter =0;
      while(!sessionImpls.isEmpty() && counter < 20) {
        TimeUnit.MILLISECONDS.sleep(100);
        counter++;
      }

      List<String> idleSessions = SessionManagerTest.getInstance().getIdleSessions();
      for (String idleSession : idleSessions) {
        System.err.println("Idle Session still active::" + idleSession);
        SubscriptionController subCtl = SessionManagerTest.getInstance().getIdleSubscriptions(idleSession);
        if (subCtl != null) {
          SessionManagerTest.getInstance().closeSubscriptionController(subCtl);
        }
      }

      Map<String, DestinationImpl> destinationImpls = md.getDestinationManager().get(null);
      for (DestinationImpl destinationImpl : destinationImpls.values()) {
        if (!destinationImpl.getFullyQualifiedNamespace().startsWith("$")) {
          md.getDestinationManager().delete(destinationImpl);
        }
      }
      Assertions.assertFalse(md.getSessionManager().hasSessions());
      long timeout = System.currentTimeMillis()+ 2000;
      while(SessionManagerTest.getInstance().hasIdleSessions() && timeout > System.currentTimeMillis()){
        delay(100);
      }
      Assertions.assertFalse(SessionManagerTest.getInstance().hasIdleSessions());

      List<DestinationManagerListener> listeners = md.getDestinationManager().getListeners();
      for (DestinationManagerListener listener : listeners) {
        System.err.println("has listener " + listener.getClass().toString());
      }
    }
    catch (Exception ex){
      ex.printStackTrace();
    }
  }

    private static final class TestExitHandler extends Thread{

    @Override
    public void run() {
      md.stop(0);
      try {
        th.join(2000);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
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
