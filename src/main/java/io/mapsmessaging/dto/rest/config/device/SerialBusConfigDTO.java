package io.mapsmessaging.dto.rest.config.device;

import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;


@Data
@EqualsAndHashCode(callSuper=false)
@NoArgsConstructor
@Schema(description = "Serial Device Bus Configuration DTO")
public class SerialBusConfigDTO extends BaseConfigDTO {

  @Schema(description = "Name of the serial bus managemnt", example = "serial")
  protected String name;

  @Schema(description = "List of Serial devices devices on this bus")
  protected List<SerialDeviceDTO> devices;

  @Schema(description = "Indicates if the device bus is enabled")
  protected boolean enabled;

  @Schema(description = "Template for the topic name")
  protected String topicNameTemplate;

  @Schema(description = "Filter configuration for the device bus")
  protected String filter;

  @Schema(description = "Selector configuration for the device bus")
  protected String selector;
}