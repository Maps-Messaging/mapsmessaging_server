package io.mapsmessaging.hardware.device.handler.serial;


import com.fazecast.jSerialComm.SerialPort;
import io.mapsmessaging.devices.serial.devices.sensors.SerialDevice;

public class Serial implements SerialDevice {

  private final SerialPort serial;

  public Serial(SerialPort serial) {
    this.serial = serial;
  }

  @Override
  public boolean isOpen() {
    return serial.isOpen();
  }

  @Override
  public boolean openPort() {
    return serial.openPort();
  }

  @Override
  public String getSystemPortName() {
    return serial.getSystemPortName();
  }

  @Override
  public void closePort() {
    serial.closePort();
  }

  @Override
  public int writeBytes(byte[] request, int length) {
    return serial.writeBytes(request, length);
  }

  @Override
  public int readBytes(byte[] buffer, int remaining) {
    return serial.readBytes(buffer, remaining);
  }
}