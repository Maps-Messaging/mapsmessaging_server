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
