package io.mapsmessaging.dto.rest.config.network;

import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class SerialDeviceDTO extends BaseConfigDTO {

  @Schema(description =
      "Serial number for the device, optional",
      example = "262144",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true

  )
  protected String serialNo;

  @Schema(
      description = "Serial port name",
      example = "/dev/ttyS0",
      requiredMode = Schema.RequiredMode.REQUIRED
  )
  protected String port;
  @Schema(
      description = "Baud rate for the serial connection",
      example = "9600",
      allowableValues = {"110", "300", "600", "1200", "2400", "4800", "9600", "14400", "19200", "28800", "38400", "57600", "115200", "230400", "460800", "921600"},
      requiredMode = Schema.RequiredMode.REQUIRED
  )
  protected int baudRate;

  @Schema(
      description = "Number of data bits in the serial connection",
      example = "8",
      allowableValues = {"5", "6", "7", "8"},
      requiredMode = Schema.RequiredMode.REQUIRED
  )
  protected int dataBits;

  @Schema(
      description = "Number of stop bits in the serial connection",
      example = "1",
      allowableValues = {"1", "1.5", "2"},
      requiredMode = Schema.RequiredMode.REQUIRED,
      nullable = false
  )
  protected float stopBits;

  @Schema(
      description = "Parity setting for the serial connection",
      example = "n",
      allowableValues = {"n", "o", "e", "m", "s"},
      requiredMode = Schema.RequiredMode.REQUIRED
  )
  protected String parity;

  @Schema(
      description = "Flow control setting for the serial connection",
      example = "1",
      allowableValues = {"0", "1", "2", "3"},
      requiredMode = Schema.RequiredMode.REQUIRED
  )
  protected int flowControl;

  @Schema(
      description = "Read timeout in milliseconds",
      example = "60000",
      minimum = "1000",
      maximum = "600000",
      requiredMode = Schema.RequiredMode.REQUIRED
  )
  protected int readTimeOut = 60000;

  @Schema(
      description = "Write timeout in milliseconds",
      example = "60000",
      minimum = "1000",
      maximum = "600000",
      defaultValue = "60000",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED
  )
  protected int writeTimeOut = 60000;

  @Schema(
      description = "Buffer size in bytes",
      example = "262144",
      minimum = "1024",
      maximum = "1048576",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      defaultValue = "102400"
  )
  protected int bufferSize = 256*1024;
}
