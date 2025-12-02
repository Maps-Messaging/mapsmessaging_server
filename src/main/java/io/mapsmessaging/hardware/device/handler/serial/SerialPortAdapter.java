package io.mapsmessaging.hardware.device.handler.serial;

import com.fazecast.jSerialComm.SerialPort;
import io.mapsmessaging.devices.DeviceBusManager;
import io.mapsmessaging.devices.serial.SerialDeviceController;
import io.mapsmessaging.dto.rest.config.device.SerialDeviceDTO;
import io.mapsmessaging.network.io.impl.serial.SerialEndPoint;
import io.mapsmessaging.network.io.impl.serial.management.SerialPortListener;

import java.io.IOException;

public class SerialPortAdapter implements SerialPortListener {

  private final DeviceBusManager deviceBusManager;
  private final SerialDeviceDTO serialDeviceDTO;

  private SerialDeviceController deviceController;

  public SerialPortAdapter(DeviceBusManager deviceBusManage, SerialDeviceDTO serialDeviceDTO) {
    this.deviceBusManager = deviceBusManage;
    this.serialDeviceDTO = serialDeviceDTO;
  }

  @Override
  public void bind(SerialPort port) throws IOException {
    SerialEndPoint.configure(port, serialDeviceDTO.getSerialConfig());
    deviceController = deviceBusManager.getSerialBusManager().mount(serialDeviceDTO.getName(), new Serial(port));
  }

  @Override
  public void unbind(SerialPort port) throws IOException {
    deviceBusManager.getSerialBusManager().unmount(deviceController);
    deviceController.close();
    deviceController = null;
  }

  @Override
  public String getName() {
    return serialDeviceDTO.getName();
  }
}
