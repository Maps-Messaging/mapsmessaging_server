package io.mapsmessaging.network.protocol.conformance.common;

import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Tag("conformance")
@Tag("conformance-core")
class ProtocolConformanceReferenceTest {

  private static final List<String> CONFORMANCE_CLASSES = List.of(
      "io.mapsmessaging.network.protocol.conformance.amqp.Amqp10HeaderConformanceTest",
      "io.mapsmessaging.network.protocol.conformance.amqp.Amqp10FrameConformanceTest",
      "io.mapsmessaging.network.protocol.conformance.amqp.Amqp10LifecycleConformanceTest",
      "io.mapsmessaging.network.protocol.conformance.amqp.Amqp10SaslConformanceTest",
      "io.mapsmessaging.network.protocol.conformance.amqp.Amqp10LinkTransferConformanceTest",
      "io.mapsmessaging.network.protocol.conformance.jms.JmsAmqpSemanticConformanceTest",
      "io.mapsmessaging.network.protocol.conformance.jms.JmsCoreConformanceTest",
      "io.mapsmessaging.network.protocol.conformance.jms.JmsFullConformanceTest",
      "io.mapsmessaging.network.protocol.conformance.jms.JmsUnidentifiedProducerConformanceTest"
  );

  @Test
  void everyListedConformanceTestHasAuthoritativeSpecificationReference() throws Exception {
    for (String className : CONFORMANCE_CLASSES) {
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
            requirement.source().startsWith("https://"),
            () -> className + "#" + method.getName()
                + " must reference an authoritative specification URL");
      }
    }
  }
}
