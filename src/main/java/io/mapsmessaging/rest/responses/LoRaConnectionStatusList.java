package io.mapsmessaging.rest.responses;

import io.mapsmessaging.dto.rest.lora.LoRaEndPointConnectionInfoDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoRaConnectionStatusList {
  private List<LoRaEndPointConnectionInfoDTO> list;
}
