package io.mapsmessaging.network.protocol.impl.n2k;

import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.MessageListener;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SubscriptionContextBuilder;
import io.mapsmessaging.api.features.ClientAcknowledgement;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.canbus.device.SocketCanDevice;
import io.mapsmessaging.canbus.device.frames.CanFrame;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.test.BaseTestConfig;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class N2kViaCanBusTest extends BaseTestConfig {

  private static final Pattern CANDUMP_PATTERN =
      Pattern.compile("^\\(\\d+\\.\\d+\\)\\s+(\\S+)\\s+([0-9A-Fa-f]+)#([0-9A-Fa-f]*)\\s*$");

  private static final String[] CANDUMP_LINES = new String[]{
      "(1745600961.335462) can0 11FC1063#003DFFFF02000100",
      "(1745600961.335462) can0 11FC1063#0101000C0153514C",
      "(1745600961.336482) can0 11FC1063#0220536572766572",
      "(1745600961.336482) can0 11FC1063#0310FBCF24EB4EF3",
      "(1745600961.336482) can0 11FC1063#0402004500000000",
      "(1745600961.336482) can0 11FC1063#0504000002000501",
      "(1745600961.336482) can0 11FC1063#0644423210FBCF24",
      "(1745600961.336482) can0 11FC1063#07EB4EFF20006000",
      "(1745600961.336482) can0 11FC1063#080000F33F0000FF",
      "(1745600961.336482) can0 11FC1163#002BFFFF02001600",
      "(1745600961.336482) can0 11FC1163#01FFFF01000D0148",
      "(1745600961.336482) can0 11FC1163#02657265546F5468",
      "(1745600961.336482) can0 11FC1163#0365726527020010",
      "(1745600961.336482) can0 11FC1163#0401416E64204261",
      "(1745600961.336482) can0 11FC1163#05636B2041676169",
      "(1745600961.337436) can0 11FC1163#066E1BFFFFFFFFFF",
      "(1745600961.337436) can0 11FC1263#001DFFFF02000B01",
      "(1745600961.337436) can0 11FC1263#0142656E746C6569",
      "(1745600961.337436) can0 11FC1263#02676810FBCF24EB",
      "(1745600961.337436) can0 11FC1263#034E02180003F01E",
      "(1745600961.337436) can0 11FC1263#0400FFFFFFFFFFFF",
      "(1745600961.337436) can0 11FC1363#0039FFFF02006400",
      "(1745600961.337436) can0 11FC1363#01FFFF020001000B",
      "(1745600961.337436) can0 11FC1363#020142656E746C65",
      "(1745600961.337436) can0 11FC1363#0369676800A01CE9",
      "(1745600961.337436) can0 11FC1363#0440F32056210010",
      "(1745600961.337436) can0 11FC1363#0501456173742042",
      "(1745600961.337436) can0 11FC1363#06656E746C656967",
      "(1745600961.337436) can0 11FC1363#076800F141EB8047",
      "(1745600961.337436) can0 11FC1363#08AA56FFFFFFFFFF",
      "(1745600961.337436) can0 11FC1463#003BFFFF03006400",
      "(1745600961.337436) can0 11FC1463#01FFFF020001000B",
      "(1745600961.337436) can0 11FC1463#020142656E746C65",
      "(1745600961.337436) can0 11FC1463#0369676821001001",
      "(1745600961.337436) can0 11FC1463#0445617374204265",
      "(1745600961.337436) can0 11FC1463#056E746C65696768",
      "(1745600961.337436) can0 11FC1463#0663001001466177",
      "(1745600961.337436) can0 11FC1463#076B6E6572204265",
      "(1745600961.337436) can0 11FC1463#0861636F6EFFFFFF",
      "(1745600961.337436) can0 11FC1563#0014FFFF02006400",
      "(1745600961.337436) can0 11FC1563#01FFFF020001001E",
      "(1745600961.337436) can0 11FC1563#0200FC02001400FD",
      "(1745600961.337436) can0 11FC1663#0035FFFF02006400",
      "(1745600961.337436) can0 11FC1663#01FFFF0200010013",
      "(1745600961.337436) can0 11FC1663#020142656E746C65",
      "(1745600961.337436) can0 11FC1663#036967682D436F6D",
      "(1745600961.337436) can0 11FC1663#046D656E74020014",
      "(1745600961.337436) can0 11FC1663#0501476C656E6875",
      "(1745600961.337436) can0 11FC1663#066E746C792D436F",
      "(1745600961.338397) can0 11FC1663#076D6D656E74FFFF",
      "(1745600961.338397) can0 11FC1763#003DFFFF02000F00",
      "(1745600961.338397) can0 11FC1763#01FFFF0200170148",
      "(1745600961.338397) can0 11FC1763#0265726520746F20",
      "(1745600961.338397) can0 11FC1763#0354686572652D43",
      "(1745600961.338397) can0 11FC1763#046F6D6D656E7405",
      "(1745600961.338397) can0 11FC1763#05001A01416E6420",
      "(1745600961.338397) can0 11FC1763#064261636B204167",
      "(1745600961.338397) can0 11FC1763#0761696E202D2043",
      "(1745600961.338397) can0 11FC1763#086F6D6D656E74FF",
      "(1745600961.338397) can0 11FC1863#002FFFFF02000300",
      "(1745600961.338397) can0 11FC1863#010100160153514C",
      "(1745600961.338397) can0 11FC1863#0220536572766572",
      "(1745600961.338397) can0 11FC1863#03202D20436F6D6D",
      "(1745600961.338397) can0 11FC1863#04656E7402000F01",
      "(1745600961.338397) can0 11FC1863#05444232202D2043",
      "(1745600961.338397) can0 11FC1863#066F6D6D656E74FF",
      "(1745600961.338397) can0 11FC1A63#0030000002000200",
      "(1745600961.338397) can0 11FC1A63#010100FFFF6C000A",
      "(1745600961.338397) can0 11FC1A63#02014D634B696E6E",
      "(1745600961.338397) can0 11FC1A63#036F6E404E08ECC0",
      "(1745600961.338397) can0 11FC1A63#049649505C000801",
      "(1745600961.338397) can0 11FC1A63#054F726D6F6E64A0",
      "(1745600961.338397) can0 11FC1A63#06311FEC607E2650",
      "(1745600961.338397) can0 11F10163#0023A012AB0F00F8",
      "(1745600961.338397) can0 11F10163#0140C2931EFFEB4E",
      "(1745600961.338397) can0 11F10163#0210FBCF243D8533",
      "(1745600961.338397) can0 11F10163#03E9285F4056FC5B",
      "(1745600961.338397) can0 11F10163#043D33009218D439",
      "(1745600961.338397) can0 11F10163#05F8FFFFFFFFFFFF",
      "(1745600961.338397) can0 11F90463#0022FFA00F000044",
      "(1745600961.338397) can0 11F90463#01109D1A29EB4EDB",
      "(1745600961.338397) can0 11F90463#0253385500000000",
      "(1745600961.338397) can0 11F90463#03010000000060E3",
      "(1745600961.338397) can0 11F90463#041600CA91FE0404",
      "(1745600961.338397) can0 11F90563#0040FFFF02000100",
      "(1745600961.338397) can0 11F90563#010200E00801526F",
      "(1745600961.338397) can0 11F90563#0275746531FF0100",
      "(1745600961.338397) can0 11F90563#030D01576179706F",
      "(1745600961.338397) can0 11F90563#04696E744F6E6500",
      "(1745600961.338397) can0 11F90563#0560E31600CA91FE",
      "(1745600961.338397) can0 11F90563#0602000D01576179",
      "(1745600961.338397) can0 11F90563#07706F696E745477",
      "(1745600961.338397) can0 11F90563#086F0069201700C1",
      "(1745600961.338397) can0 11F90563#0954FEFFFFFFFFFF",
      "(1745600961.338397) can0 11FB1063#004E70FF17324C27",
      "(1745600961.338397) can0 11FB1063#011E6D6439303030",
      "(1745600961.338397) can0 11FB1063#0236393930303036",
      "(1745600961.338397) can0 11FB1063#03390B0131383030",
      "(1745600961.338397) can0 11FB1063#042048656C70C066",
      "(1745600961.338397) can0 11FB1063#054AE9802CF35510",
      "(1745600961.338397) can0 11FB1063#06FBCF2417324C27",
      "(1745600961.338397) can0 11FB1063#071E7FFD39303030",
      "(1745600961.338397) can0 11FB1063#0837303930303037",
      "(1745600961.338397) can0 11FB1063#093010FBCF24EB4E",
      "(1745600961.338397) can0 11FB1063#0AFFFF680601496F",
      "(1745600961.338397) can0 11FB1063#0B6E61FFFFFFFFFF"
  };

  @Test
  void testSimpleSend() throws LoginException, IOException, InterruptedException {
    AtomicInteger receivedCount = new AtomicInteger(0);
    CountDownLatch firstMessageLatch = new CountDownLatch(1);
    List<Message> messages = new CopyOnWriteArrayList<>();

    MessageListener listener = messageEvent -> {
      receivedCount.incrementAndGet();
      firstMessageLatch.countDown();
      messages.add(messageEvent.getMessage());
      messageEvent.getCompletionTask().run();
    };

    Session session = createSession("n2kSimpleTest", 60, 60, false, listener);
    Assertions.assertNotNull(session);

    try {
      SubscriptionContextBuilder subscriptionContextBuilder =
          new SubscriptionContextBuilder("/vcan0/#", ClientAcknowledgement.AUTO);

      SubscriptionContext context = subscriptionContextBuilder
          .setQos(QualityOfService.AT_MOST_ONCE)
          .setReceiveMaximum(100)
          .build();

      session.addSubscription(context);

      injectCandumpFramesIntoVcan("vcan0", CANDUMP_LINES);

      boolean received = firstMessageLatch.await(5, TimeUnit.SECONDS);
      Assertions.assertTrue(received, "No N2K messages were published to /vcan0/# after CAN injection");
      Assertions.assertTrue(receivedCount.get() > 0, "Expected at least one message after injection");


    } finally {
      close(session);
    }
  }

  private void injectCandumpFramesIntoVcan(String interfaceName, String[] candumpLines) throws IOException {
    try (SocketCanDevice socketCanDevice = new SocketCanDevice(interfaceName)) {
      for (String line : candumpLines) {
        ParsedFrame parsedFrame = parseCandumpLine(line);
        if (parsedFrame == null) {
          continue;
        }

        boolean extendedFrame = parsedFrame.canId > 0x7FF;
        CanFrame frame = new CanFrame(parsedFrame.canId, extendedFrame, parsedFrame.dataLength, parsedFrame.data);
        socketCanDevice.writeFrame(frame);
        delay(10);
      }
    }
  }

  private ParsedFrame parseCandumpLine(String line) {
    Matcher matcher = CANDUMP_PATTERN.matcher(line);
    if (!matcher.matches()) {
      return null;
    }

    String canIdHex = matcher.group(2);
    String dataHex = matcher.group(3);

    if (dataHex.length() % 2 != 0) {
      throw new IllegalArgumentException("Odd-length CAN data field: " + line);
    }

    int dataLength = dataHex.length() / 2;
    if (dataLength > 8) {
      throw new IllegalArgumentException("CAN frame payload exceeds 8 bytes (not a raw frame): " + line);
    }

    int canId = (int) Long.parseLong(canIdHex, 16);
    byte[] data = hexToBytes(dataHex);

    return new ParsedFrame(canId, data, dataLength);
  }

  private byte[] hexToBytes(String hex) {
    int length = hex.length();
    byte[] bytes = new byte[length / 2];

    for (int i = 0; i < length; i += 2) {
      int value = Integer.parseInt(hex.substring(i, i + 2), 16);
      bytes[i / 2] = (byte) value;
    }

    return bytes;
  }

  private static final class ParsedFrame {
    private final int canId;
    private final byte[] data;
    private final int dataLength;

    private ParsedFrame(int canId, byte[] data, int dataLength) {
      this.canId = canId;
      this.data = data;
      this.dataLength = dataLength;
    }
  }
}