/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.conformance.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface ProtocolRequirement {

  String MQTT_311_SOURCE =
      "https://docs.oasis-open.org/mqtt/mqtt/v3.1.1/os/mqtt-v3.1.1-os.html";

  String MQTT_5_SOURCE =
      "https://docs.oasis-open.org/mqtt/mqtt/v5.0/os/mqtt-v5.0-os.html";

  String STOMP_12_SOURCE =
      "https://stomp.github.io/stomp-specification-1.2.html";

  String MQTT_SN_12_SOURCE =
      "https://www.oasis-open.org/committees/document.php?document_id=66091&wg_abbrev=mqtt";

  String NATS_CLIENT_PROTOCOL_SOURCE =
      "https://docs.nats.io/reference/protocols/client";

  String COAP_RFC7252_SOURCE =
      "https://www.rfc-editor.org/rfc/rfc7252.html";

  String COAP_RFC7641_SOURCE =
      "https://www.rfc-editor.org/rfc/rfc7641.html";

  String COAP_RFC7959_SOURCE =
      "https://www.rfc-editor.org/rfc/rfc7959.html";

  String WEBSOCKET_RFC6455_SOURCE =
      "https://www.rfc-editor.org/rfc/rfc6455.html";

  String MAVLINK_SERIALIZATION_SOURCE =
      "https://mavlink.io/en/guide/serialization.html";

  String MAVLINK_XML_SCHEMA_SOURCE =
      "https://mavlink.io/en/guide/xml_schema.html";

  String specification();

  /**
   * Normative section and, where available, the protocol requirement identifier.
   */
  String value();

  /**
   * Authoritative specification URL containing the normative requirement.
   */
  String source();
}
