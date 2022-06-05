package io.mapsmessaging.network.protocol.impl.z_wave.commands;

import static io.mapsmessaging.network.protocol.impl.z_wave.commands.Constants.FUNC_ID_SERIAL_API_START;

import io.mapsmessaging.network.io.Packet;
import lombok.Getter;
import lombok.ToString;

@ToString
public class StartCommand extends Command {

  @Getter
  private WakeUpReason wakeUpReason;
  @Getter
  private boolean watchDogEnabled;
  @Getter
  private int deviceOptionMask;
  @Getter
  private int genericDeviceType;
  @Getter
  private int specificDeviceType;


  @Override
  public int getCommand() {
    return FUNC_ID_SERIAL_API_START;
  }

  @Override
  public void unpack(Packet packet) {
    wakeUpReason = WakeUpReason.fromId(packet.getByte());
    watchDogEnabled = packet.getByte() == 0x01;
    deviceOptionMask = packet.getByte();
    genericDeviceType = packet.getByte();
    specificDeviceType = packet.getByte();
  }

  public void pack(Packet packet) {
    super.pack(packet);
  }
}