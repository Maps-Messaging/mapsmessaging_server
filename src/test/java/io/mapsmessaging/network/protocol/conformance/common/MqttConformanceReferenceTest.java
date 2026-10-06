package io.mapsmessaging.network.protocol.conformance.common;

import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Tag("conformance")
@Tag("conformance-core")
class MqttConformanceReferenceTest {

  private static final String AMQP_SOURCE = "https://docs.oasis-open.org/amqp/";
  private static final String JMS_SOURCE = "https://jakarta.ee/specifications/messaging/";

  private static final List<String> MQTT_CONFORMANCE_CLASSES = List.of(
      "io.mapsmessaging.network.protocol.conformance.mqtt3.Mqtt311CoreConformanceTest",
      "io.mapsmessaging.network.protocol.conformance.mqtt3.Mqtt311SemanticConformanceTest",
      "io.mapsmessaging.network.protocol.conformance.mqtt3.Mqtt311FullConformanceTest",
      "io.mapsmessaging.network.protocol.conformance.mqtt3.Mqtt311ProtocolErrorConformanceTest",
      "io.mapsmessaging.network.protocol.conformance.mqtt5.Mqtt5CoreConformanceTest",
      "io.mapsmessaging.network.protocol.conformance.mqtt5.Mqtt5SemanticConformanceTest",
      "io.mapsmessaging.network.protocol.conformance.mqtt5.Mqtt5FullConformanceTest",
      "io.mapsmessaging.network.protocol.conformance.mqtt5.Mqtt5ProtocolErrorConformanceTest"
  );

  private static final List<String> AMQP_CONFORMANCE_CLASSES = List.of(
      "io.mapsmessaging.network.protocol.conformance.amqp.Amqp10HeaderConformanceTest",
      "io.mapsmessaging.network.protocol.conformance.amqp.Amqp10FrameConformanceTest",
      "io.mapsmessaging.network.protocol.conformance.amqp.Amqp10SaslConformanceTest"
  );

  private static final List<String> JMS_CONFORMANCE_CLASSES = List.of(
      "io.mapsmessaging.network.protocol.conformance.jms.JmsAmqpSemanticConformanceTest"
  );

  @Test
  void everyMqttConformanceTestHasAuthoritativeSpecificationReference() throws Exception {
    for (String className : MQTT_CONFORMANCE_CLASSES) {
      Class<?> type = Class.forName(className);
      for (Method method : type.getDeclaredMethods()) {
        if (!method.isAnnotationPresent(Test.class)) {
          continue;
        }
        ProtocolRequirement requirement = method.getAnnotation(ProtocolRequirement.class);
        assertNotNull(
            requirement,
            () -> className + "#" + method.getName() + " is missing @ProtocolRequirement");
        assertFalse(requirement.specification().isBlank());
        assertFalse(requirement.value().isBlank());
        assertTrue(
            requirement.source().startsWith("https://docs.oasis-open.org/mqtt/"),
            () -> className + "#" + method.getName()
                + " must reference an authoritative OASIS MQTT specification: "
                + requirement.source());
      }
    }
  }

  @Test
  void everyAmqpConformanceTestHasAuthoritativeSpecificationReference() throws Exception {
    assertReferences(AMQP_CONFORMANCE_CLASSES, AMQP_SOURCE);
  }

  @Test
  void everyJmsConformanceTestHasAuthoritativeSpecificationReference() throws Exception {
    assertReferences(JMS_CONFORMANCE_CLASSES, JMS_SOURCE);
  }

  private void assertReferences(List<String> classNames, String sourcePrefix) throws Exception {
    for (String className : classNames) {
      Class<?> type = Class.forName(className);
      for (Method method : type.getDeclaredMethods()) {
        if (!method.isAnnotationPresent(Test.class)) {
          continue;
        }
        ProtocolRequirement requirement = method.getAnnotation(ProtocolRequirement.class);
        assertNotNull(
            requirement,
            () -> className + "#" + method.getName() + " is missing @ProtocolRequirement");
        assertFalse(requirement.specification().isBlank());
        assertFalse(requirement.value().isBlank());
        assertTrue(
            requirement.source().startsWith(sourcePrefix),
            () -> className + "#" + method.getName()
                + " must reference its authoritative specification: "
                + requirement.source());
      }
    }
  }
}
