/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 */

package io.mapsmessaging.network.protocol.impl.mqtt5.listeners;

import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.features.RetainHandler;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.SubscriptionInfo;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.properties.SubscriptionIdentifier;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class SubscribeListener5ContextCoverageTest {

  @ParameterizedTest
  @MethodSource("contextCases")
  void contextPreservesSubscriptionSemantics(
      QualityOfService qos,
      boolean shared,
      boolean selectorPresent,
      boolean identifierPresent,
      boolean flagsEnabled) {
    String topic = shared ? "$share/group/sensors/value" : "sensors/value";
    String selector = selectorPresent ? "priority > 3" : null;
    SubscriptionIdentifier identifier =
        identifierPresent ? new SubscriptionIdentifier(321) : null;
    RetainHandler retainHandler = retainHandlerFor(qos);
    SubscriptionInfo info = new SubscriptionInfo(
        topic, qos, retainHandler, flagsEnabled, flagsEnabled, identifier);

    SubscriptionContext context = SubscribeListener5.createContext(info, selector, 27);

    assertEquals("sensors/value", context.getDestinationName());
    assertEquals(qos, context.getQualityOfService());
    assertEquals(qos.getClientAcknowledgement(), context.getAcknowledgementController());
    assertEquals(retainHandler, context.getRetainHandler());
    assertEquals(27, context.getReceiveMaximum());
    assertTrue(context.allowOverlap());
    assertEquals(flagsEnabled, context.noLocalMessages());
    assertEquals(flagsEnabled, context.isRetainAsPublish());
    assertEquals(shared, context.isSharedSubscription());
    assertEquals(shared ? "group" : null, context.getSharedName());
    assertEquals(selector, context.getSelector());
    assertEquals(identifierPresent ? 321L : 0L, context.getSubscriptionId());
  }

  private static RetainHandler retainHandlerFor(QualityOfService qos) {
    return switch (qos) {
      case AT_MOST_ONCE -> RetainHandler.SEND_ALWAYS;
      case AT_LEAST_ONCE -> RetainHandler.SEND_IF_NEW;
      case EXACTLY_ONCE -> RetainHandler.DO_NOT_SEND;
      default -> throw new IllegalArgumentException("Unsupported MQTT 5 subscription QoS");
    };
  }

  private static Stream<Arguments> contextCases() {
    List<Arguments> cases = new ArrayList<>();
    for (QualityOfService qos : List.of(
        QualityOfService.AT_MOST_ONCE,
        QualityOfService.AT_LEAST_ONCE,
        QualityOfService.EXACTLY_ONCE)) {
      for (boolean shared : List.of(false, true)) {
        for (boolean selectorPresent : List.of(false, true)) {
          for (boolean identifierPresent : List.of(false, true)) {
            for (boolean flagsEnabled : List.of(false, true)) {
              cases.add(Arguments.of(
                  qos, shared, selectorPresent, identifierPresent, flagsEnabled));
            }
          }
        }
      }
    }
    return cases.stream();
  }
}
