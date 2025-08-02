package io.mapsmessaging.ml.streams;

import io.mapsmessaging.api.*;
import io.mapsmessaging.api.features.ClientAcknowledgement;
import io.mapsmessaging.api.features.DestinationMode;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.dto.rest.config.ml.MLEventStreamDTO;
import io.mapsmessaging.engine.session.ClientConnection;
import io.mapsmessaging.selector.ParseException;
import io.mapsmessaging.selector.SelectorParser;
import io.mapsmessaging.selector.operators.ParserExecutor;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.security.Principal;

public class StreamHandler implements ClientConnection, MessageListener {

  private final MLEventStreamDTO eventStream;
  private SubscribedEventManager subscribedEventManager;
  private ParserExecutor parserExecutor;
  private Session session;

  public StreamHandler(MLEventStreamDTO eventStream) {
    this.eventStream = eventStream;
    try {
      parserExecutor = SelectorParser.compile(eventStream.getSelector());
      // I need to get the model name, and the keys that it needs
    } catch (ParseException e) {
      // log this
    }
    try {
      session = createSession(eventStream);
    } catch (LoginException e) {
      // log this
    } catch (IOException e) {
      // log this
    }
  }

  public void start() {
    try {
      session.start();
      SubscriptionContextBuilder scb = new SubscriptionContextBuilder(eventStream.getTopicFilter(), ClientAcknowledgement.AUTO);
      scb.setMode(DestinationMode.NORMAL)
          .setQos(QualityOfService.AT_MOST_ONCE)
          .setSelector(eventStream.getSelector())
          .setAlias(eventStream.getId());
      subscribedEventManager = session.addSubscription(scb.build());
    } catch (IOException e) {
      // log this
    }
  }

  public void stop() {
    try {
      session.removeSubscription(subscribedEventManager.getContext().getAlias());
      SessionManager.getInstance().close(session, true);
    } catch (IOException e) {
      // log this
    }
  }

  private Session createSession(MLEventStreamDTO eventStream) throws LoginException, IOException {
    SessionContextBuilder sessionContextBuilder = new SessionContextBuilder(eventStream.getId(), this);
    sessionContextBuilder.setPersistentSession(false)
        .setKeepAlive(0)
        .setSessionExpiry(0);
    return SessionManager.getInstance().create(sessionContextBuilder.build(), this);
  }

  @Override
  public long getTimeOut() {
    return 0;
  }

  @Override
  public String getName() {
    return eventStream.getId();
  }

  @Override
  public String getVersion() {
    return "1.0";
  }

  @Override
  public void sendKeepAlive() {
    // No Op, nothing to do here
  }

  @Override
  public Principal getPrincipal() {
    return null;
  }

  @Override
  public String getAuthenticationConfig() {
    return "";
  }

  @Override
  public String getUniqueName() {
    return eventStream.getId();
  }

  @Override
  public void sendMessage(@NotNull @NonNull MessageEvent messageEvent) {
    System.err.println("Message Received: " + messageEvent.getMessage());
    parserExecutor.evaluate(messageEvent.getMessage());
    // here we process the event and the model
  }
}
