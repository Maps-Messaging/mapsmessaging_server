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

package org.maps.test;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.maps.BaseTest;
import org.maps.messaging.MessageDaemon;
import org.maps.messaging.engine.destination.DestinationImpl;
import org.maps.messaging.engine.destination.DestinationManagerListener;
import org.maps.messaging.engine.destination.subscription.SubscriptionController;
import org.maps.messaging.engine.session.SessionImpl;
import org.maps.messaging.engine.session.SessionManager;
import org.maps.messaging.engine.session.SessionManagerTest;

public class BaseTestConfig extends BaseTest {

  protected static MessageDaemon md = null;
  private static Thread th;

  static{
    Runtime.getRuntime().addShutdownHook(new TestExitHandler());
  }

  @AfterEach
  public void clear(){
    Runtime.getRuntime().gc(); // Try and free up memory before the next test kicks off
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
        manager.close(sessionImpl);
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

      List<DestinationImpl> destinationImpls = md.getDestinationManager().getDestinations();
      for (DestinationImpl destinationImpl : destinationImpls) {
        if (!destinationImpl.getName().startsWith("$")) {
          md.getDestinationManager().delete(destinationImpl);
        }
      }
      Assertions.assertFalse(md.getSessionManager().hasSessions());
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
        e.printStackTrace();
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

  protected void delayMS(long milliseconds){
    LockSupport.parkNanos(milliseconds * 1000L * 1000L);
  }
}
