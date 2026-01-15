package io.mapsmessaging.config.network;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.network.SerialDeviceDTO;

public class SerialDeviceHelper {

  public static SerialDeviceDTO getSerialDeviceDTO(ConfigurationProperties config) {
    ConfigurationProperties serial = config;
    if(config.containsKey("serial")) {
      serial = (ConfigurationProperties) config.get("serial");
    }
    SerialDeviceDTO serialDeviceDTO = new SerialDeviceDTO();

    serialDeviceDTO.setPort(serial.getProperty("port"));
    serialDeviceDTO.setBaudRate(serial.getIntProperty("baudRate", serialDeviceDTO.getBaudRate()));
    serialDeviceDTO.setDataBits(serial.getIntProperty("dataBits", serialDeviceDTO.getDataBits()));
    serialDeviceDTO.setStopBits(serial.getFloatProperty("stopBits", serialDeviceDTO.getStopBits()));
    serialDeviceDTO.setParity(serial.getProperty("parity", serialDeviceDTO.getParity()));
    serialDeviceDTO.setFlowControl(serial.getIntProperty("flowControl", serialDeviceDTO.getFlowControl()));
    serialDeviceDTO.setSerialNo(serial.getProperty("serialNo"));
    serialDeviceDTO.setReadTimeOut(config.getIntProperty("readTimeOut", serialDeviceDTO.getReadTimeOut()));
    serialDeviceDTO.setWriteTimeOut(config.getIntProperty("writeTimeOut", serialDeviceDTO.getWriteTimeOut()));
    serialDeviceDTO.setBufferSize(config.getIntProperty("bufferSize", serialDeviceDTO.getBufferSize()));
    return serialDeviceDTO;
  }
}
