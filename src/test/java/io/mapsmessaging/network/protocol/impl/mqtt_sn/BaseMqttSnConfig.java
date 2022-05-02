package io.mapsmessaging.network.protocol.impl.mqtt_sn;

import static org.junit.jupiter.params.provider.Arguments.arguments;

import io.mapsmessaging.test.BaseTestConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;

public class BaseMqttSnConfig extends BaseTestConfig {

  public static final int[] QOS_LIST = {0, 1, 2};
  public static final int[] VERSIONS = {1, 2};

  public static Stream<Arguments> createQoSVersionStream() {
    List<Arguments> args = new ArrayList<>();
    for (int qos : QOS_LIST) {
      for (int verion : VERSIONS) {
        args.add(arguments(qos, verion));
      }
    }
    return args.stream();
  }
}
