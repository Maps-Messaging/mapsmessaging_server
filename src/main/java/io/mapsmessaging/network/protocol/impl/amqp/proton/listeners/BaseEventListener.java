/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
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

package io.mapsmessaging.network.protocol.impl.amqp.proton.listeners;

import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SessionContextBuilder;
import io.mapsmessaging.api.SessionManager;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.network.ProtocolClientConnection;
import io.mapsmessaging.network.protocol.impl.amqp.AMQPProtocol;
import io.mapsmessaging.network.protocol.impl.amqp.proton.ProtonEngine;
import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.amqp.messaging.Source;
import org.apache.qpid.proton.amqp.transport.Target;
import org.apache.qpid.proton.engine.Connection;
import org.apache.qpid.proton.engine.Receiver;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class BaseEventListener implements EventListener {

  protected static final Symbol DeliveryError = Symbol.getSymbol("deliveryError");
  protected static final Symbol NoSuchDestinationError = Symbol.getSymbol("noSuchDestination");
  protected static final Symbol TransactionError = Symbol.getSymbol("transactionError");
  protected static final Symbol SUBSCRIPTION_ERROR = Symbol.valueOf("messaging:subscription:exception");
  protected static final Symbol DYNAMIC_CREATION_ERROR = Symbol.valueOf("messaging:dynamic_creation:exception");
  protected static final Symbol SHARE_NAME_ERROR = Symbol.valueOf("messaging:subscription:no_share_name");
  protected static final Symbol SELECTOR_ERROR = Symbol.valueOf("messaging:selector:exception");
  protected static final Symbol SESSION_ERROR = Symbol.valueOf("messaging:session:exception");
  protected static final Symbol SESSION_CREATION = Symbol.valueOf("session:creation:exception");

  private final int window;

  protected final AMQPProtocol protocol;
  protected final ProtonEngine engine;

  protected BaseEventListener(AMQPProtocol protocol, ProtonEngine engine) {
    this.protocol = protocol;
    this.engine = engine;
    window = 10;
  }

  protected void topUp(Receiver rcv) {
    int delta = window - rcv.getCredit();
    rcv.flow(delta);
  }

  protected String parseSessionId(String sessionId) {
    StringBuilder sb = new StringBuilder();
    for (char ch : sessionId.toCharArray()) {
      if (Character.isAlphabetic(ch) || Character.isDigit(ch)) {
        sb.append(ch);
      } else {
        sb.append("_");
      }
    }

    return sb.toString();
  }

  protected io.mapsmessaging.network.protocol.impl.amqp.SessionManager createOrReuseSession(Connection connection) throws LoginException, IOException {
    String sessionId = parseSessionId(connection.getRemoteContainer());
    io.mapsmessaging.network.protocol.impl.amqp.SessionManager sessionManager = protocol.getSession(sessionId);
    if (sessionManager == null) {
      String username = protocol.getUsername();
      boolean isAuthorised = protocol.isAuthorised();
      SessionContextBuilder contextBuilder = new SessionContextBuilder(sessionId, new ProtocolClientConnection(protocol));
      contextBuilder
          .setPersistentSession(true)
          .setSessionExpiry(600)
          .setPassword(new char[0])
          .isAuthorized(isAuthorised)
          .setUsername(username);
      Session session = SessionManager.getInstance().create(contextBuilder.build(), protocol);
      session.start();
      session.login();
      sessionManager = protocol.addSession(sessionId, session);
      protocol.getLogger().log(ServerLogMessages.AMQP_CREATED_SESSION, sessionId);
    } else {
      sessionManager.increment();
    }
    return sessionManager;
  }

  protected DestinationType getDestinationType(Receiver receiver) {
    Target target = receiver.getTarget();
    boolean dynamic = false;
    Symbol[] symbols;
    if (target instanceof org.apache.qpid.proton.amqp.messaging.Target) {
      symbols = ((org.apache.qpid.proton.amqp.messaging.Target) target).getCapabilities();
      dynamic = ((org.apache.qpid.proton.amqp.messaging.Target) target).getDynamic();
    } else {
      symbols = new Symbol[0];
    }
    if (symbols == null) {
      symbols = new Symbol[0];
    }
    return locateType(symbols, dynamic);
  }

  private DestinationType locateType(Symbol[] symbols, boolean dynamic) {
    for (Symbol symbol : symbols) {
      if (symbol.equals(Symbol.getSymbol("queue"))) {
        if (dynamic) {
          return DestinationType.TEMPORARY_QUEUE;
        } else {
          return DestinationType.QUEUE;
        }
      }
      if (symbol.equals(Symbol.getSymbol("topic"))) {
        if (dynamic) {
          return DestinationType.TEMPORARY_TOPIC;
        } else {
          return DestinationType.TOPIC;
        }
      }
    }
    return DestinationType.TOPIC;
  }

  protected DestinationType getDestinationType(Source source) {
    Symbol[] symbols = source.getCapabilities();
    if (symbols != null) {
      for (Symbol symbol : symbols) {
        if (symbol.equals(Symbol.getSymbol("queue"))) {
          return DestinationType.QUEUE;
        }
        if (symbol.equals(Symbol.getSymbol("topic"))) {
          return DestinationType.TOPIC;
        }
      }
    }
    return DestinationType.TOPIC;
  }

  protected boolean isShared(Source source) {
    Symbol[] symbols = source.getCapabilities();
    if (symbols != null) {
      for (Symbol symbol : symbols) {
        if (symbol.equals(Symbol.getSymbol("shared"))) {
          return true;
        }
      }
    }
    return false;
  }

  protected List<Symbol> getRemoteCapabilities(Symbol[] symbols) {
    List<Symbol> offered = new ArrayList<>();
    if (symbols != null) {
      for (Symbol symbol : symbols) {
        if (symbol.equals(Symbol.getSymbol("SHARED-SUBS")) ||
            symbol.equals(Symbol.getSymbol("sole-connection-for-container")) ||
            symbol.equals(Symbol.getSymbol("ANONYMOUS-RELAY"))) {
          offered.add(symbol);
        }
      }
    }
    return offered;
  }
}
