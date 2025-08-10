package io.mapsmessaging.network.protocol.impl.orbcomm.modem.protocol;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.StreamHandler;
import io.mapsmessaging.network.protocol.impl.nmea.Constants;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ModemStreamHandler implements StreamHandler {

  private static final int BUFFER_SIZE = 10240;

  private final byte[] inputBuffer;
  private final byte[] outBuffer;

  ModemStreamHandler() {
    inputBuffer = new byte[BUFFER_SIZE];
    outBuffer = new byte[BUFFER_SIZE];
  }

  @Override
  public void close() {
    // There is nothing to do here
  }

  @Override
  public int parseInput(InputStream input, Packet packet) throws IOException {
    int idx = 0;
    int b;

// skip empty lines (CR/LF only)
    do {
      b = input.read();
      if (b == -1) return 0; // EOF, no data
    } while (b == Constants.CR || b == Constants.LF);

// read until first CR/LF after any data
    inputBuffer[idx++] = (byte) b;
    while (idx < inputBuffer.length) {
      b = input.read();
      if (b == -1) break;                 // EOF mid-line
      if (b == Constants.CR || b == Constants.LF) break; // end of line (we'll add CR/LF later)
      inputBuffer[idx++] = (byte) b;
    }

    if (idx == inputBuffer.length) {
      throw new IOException("Exceeded buffer size of known Modem sentences");
    }
    inputBuffer[idx++] = (byte) Constants.CR;
    inputBuffer[idx++] = (byte) Constants.LF;
    packet.put(inputBuffer, 0, idx);
    return idx;
  }

  @Override
  public int parseOutput(OutputStream output, Packet packet) throws IOException {
    int available = packet.available();
    int total = available;
    while (available > outBuffer.length) {
      packet.get(outBuffer, 0, available);
      output.write(outBuffer, 0, available);
      available = packet.available();
    }
    if (available > 0) {
      packet.get(outBuffer, 0, available);
      output.write(outBuffer, 0, available);
    }
    output.flush();
    return total;
  }
}
