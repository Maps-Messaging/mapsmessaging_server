/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     https://commonsclause.com/
 */

package io.mapsmessaging.network.protocol.impl.stomp.frames;

import io.mapsmessaging.network.protocol.impl.stomp.StompProtocolException;

public final class AcknowledgementToken {

  private AcknowledgementToken() {
  }

  public static String create(String subscriptionId, long messageId) {
    return subscriptionId + ":" + messageId;
  }

  public static Value parse(String token) throws StompProtocolException {
    if (token == null || token.isBlank()) {
      throw new StompProtocolException("Missing STOMP acknowledgement id");
    }
    int separator = token.lastIndexOf(':');
    if (separator <= 0 || separator == token.length() - 1) {
      throw new StompProtocolException("Invalid STOMP acknowledgement id");
    }
    String subscriptionId = token.substring(0, separator);
    try {
      long messageId = Long.parseLong(token.substring(separator + 1));
      return new Value(subscriptionId, messageId);
    } catch (NumberFormatException e) {
      throw new StompProtocolException("Invalid STOMP acknowledgement message id");
    }
  }

  public record Value(String subscriptionId, long messageId) {
  }
}
