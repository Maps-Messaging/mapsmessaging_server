package io.mapsmessaging.network.protocol.impl.z_wave;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.EndOfBufferException;
import io.mapsmessaging.network.protocol.detection.Detection;
import io.mapsmessaging.network.protocol.impl.z_wave.commands.Constants;

public class ZWaveProtocolDetection  implements Detection {

  @Override
  public boolean detected(Packet packet) throws EndOfBufferException {
    int pos = packet.position();
    try {
      return (packet.get(0) == Constants.SOF);
    } catch (Exception e) {
      return false;
    } finally {
      packet.position(pos); // roll it back
    }
  }

  @Override
  public int getHeaderSize() {
    return 1;
  }

}
