package io.mapsmessaging.network.protocol.impl.z_wave;

import static io.mapsmessaging.network.protocol.impl.z_wave.commands.Constants.ACK;
import static io.mapsmessaging.network.protocol.impl.z_wave.commands.Constants.CAN;
import static io.mapsmessaging.network.protocol.impl.z_wave.commands.Constants.NAK;
import static io.mapsmessaging.network.protocol.impl.z_wave.commands.Constants.SOF;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.StreamHandler;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ZWaveStreamHandler implements StreamHandler {

  private static final int BUFFER_SIZE = 256;

  ZWaveStreamHandler() {
  }

  @Override
  public void close() {
    // There is nothing to do here
  }

  @Override
  public int parseInput(InputStream input, Packet packet) throws IOException {
    //
    // Skip any characters before the START character
    //
    int val = input.read() & 0xff;
    while (val != ACK  && val != NAK && val != SOF && val != CAN) {
      val = input.read();
      if(val == -1){
        throw new IOException("Steam has been close");
      }
    }

    //
    // Ack / Nak are single byte packets
    if(val != SOF){
      if(val == ACK) System.err.println("Stream Handler >> Input >> Received ACK");
      if(val == NAK) System.err.println("Stream Handler >> Input >> Received NAK");
      if(val == CAN) System.err.println("Stream Handler >> Input >> Received CAN");
      packet.putByte(val);
      return 1;
    }
    System.err.println("Stream Handler >> Input >> Received SOF");
    int len = input.read() & 0xff;
    int checksum = 0xff;
    checksum = (checksum ^ (len & 0xff)) & 0xff;
    byte[] inputBuffer = new byte[len+1];
    inputBuffer[0] = SOF;
    inputBuffer[1] = (byte)(len & 0xff);
    for(int x=2;x<len+1;x++){
      inputBuffer[x] = (byte)(input.read() & 0xff);
      checksum = checksum ^ inputBuffer[x];
    }
    checksum = checksum & 0xff;
    int check = input.read();
    if(checksum ==check){
      inputBuffer[len] = (byte)0xff;
    }
    else{
      inputBuffer[len] = (byte)0x00;
    }
    dumpData("Stream Handler >> Input >> ", inputBuffer, inputBuffer.length);
    packet.put(inputBuffer, 0, inputBuffer.length);
    return inputBuffer.length;
  }

  @Override
  public int parseOutput(OutputStream output, Packet packet) throws IOException {
    int command = packet.get();
    if(command != SOF ){
      if(command == ACK) System.err.println("Stream Handler >> Output >> Sending ACK");
      if(command == NAK) System.err.println("Stream Handler >> Output >> Sending NAK");
      if(command == CAN) System.err.println("Stream Handler >> Output >> Sending CAN");
      output.write(command);
      output.flush();
      return 1;
    }
    System.err.println("Stream Handler >> Output >> Sending SOF");
    byte[] tmp = new byte[packet.available()+3]; // Len, Checksum + SOF
    tmp[0] = SOF;
    tmp[1] = (byte)(packet.available()+1 &0xff);//
    packet.get(tmp, 2, packet.available());
    tmp[tmp.length -1] = checksum(tmp, 1, tmp.length-1);
    dumpData("Stream Handler >> Output >> ", tmp, tmp.length);
    output.write(tmp);
    output.flush();
    return tmp.length;
  }

  private byte checksum(byte[] data, int offset, int len){
    int checksum = 0xff;
    for(int x=offset;x<offset+len;x++){
      checksum = (checksum ^ (data[x] & 0xff)) & 0xff;
    }
    return (byte) (checksum & 0xff);
  }


  private void dumpData(String msg, byte[] data, int len){
    StringBuilder sb = new StringBuilder(msg);
    sb.append("Len:").append(data.length).append(" Hex:[");
    for(int x=0;x<len;x++){
      int val = (data[x] &0xff);
      String sVal = Integer.toHexString(val);
      if(sVal.length() < 2){
        sVal = "0"+sVal;
      }
      sb.append(sVal).append(" ");
    }
    sb.append("]");
    System.err.println(sb);
  }

}
