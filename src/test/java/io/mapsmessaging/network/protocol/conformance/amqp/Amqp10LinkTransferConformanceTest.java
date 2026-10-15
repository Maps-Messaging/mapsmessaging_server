/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.conformance.amqp;

import io.mapsmessaging.network.protocol.conformance.common.ProtocolRequirement;
import io.mapsmessaging.test.BaseTestConfig;
import java.nio.charset.StandardCharsets;
import org.apache.qpid.proton.Proton;
import org.apache.qpid.proton.amqp.messaging.Accepted;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.amqp.messaging.Source;
import org.apache.qpid.proton.amqp.messaging.Target;
import org.apache.qpid.proton.amqp.transport.ReceiverSettleMode;
import org.apache.qpid.proton.amqp.transport.SenderSettleMode;
import org.apache.qpid.proton.engine.Delivery;
import org.apache.qpid.proton.engine.EndpointState;
import org.apache.qpid.proton.engine.Sender;
import org.apache.qpid.proton.engine.Session;
import org.apache.qpid.proton.message.Message;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Tag("conformance")
@Tag("conformance-core")
@Tag("amqp10")
class Amqp10LinkTransferConformanceTest extends BaseTestConfig {

  private static final String SOURCE =
      "https://docs.oasis-open.org/amqp/core/v1.0/os/amqp-core-transport-v1.0-os.html";

  @Test
  @ProtocolRequirement(
      specification = "AMQP-1.0",
      value = "Part 2 sections 2.6 and 2.7.3-2.7.6: ATTACH, FLOW credit, TRANSFER and DISPOSITION",
      source = SOURCE)
  void senderLinkReceivesCreditAndTransferIsAccepted() throws Exception {
    try (ProtonSocketClient client = new ProtonSocketClient("localhost", 5672)) {
      Session session = client.connection().session();
      session.open();
      client.pumpUntil(() -> session.getRemoteState() == EndpointState.ACTIVE, 5_000);

      Sender sender = session.sender("maps-conformance-sender");
      Target target = new Target();
      target.setAddress("amq.topic");
      sender.setTarget(target);
      sender.setSource(new Source());
      sender.setSenderSettleMode(SenderSettleMode.UNSETTLED);
      sender.setReceiverSettleMode(ReceiverSettleMode.FIRST);
      sender.open();

      client.pumpUntil(
          () -> sender.getRemoteState() == EndpointState.ACTIVE && sender.getCredit() > 0,
          5_000);

      Message message = Proton.message();
      message.setAddress("amq.topic");
      message.setBody(new AmqpValue("amqp-conformance-transfer"));

      byte[] encoded = new byte[4096];
      int length = message.encode(encoded, 0, encoded.length);
      Delivery delivery = sender.delivery("delivery-1".getBytes(StandardCharsets.UTF_8));

      assertEquals(length, sender.send(encoded, 0, length));
      assertTrue(sender.advance());

      client.pumpUntil(() -> delivery.getRemoteState() != null, 5_000);
      assertInstanceOf(Accepted.class, delivery.getRemoteState());

      delivery.settle();
      sender.close();
      session.close();
    }
  }
}
