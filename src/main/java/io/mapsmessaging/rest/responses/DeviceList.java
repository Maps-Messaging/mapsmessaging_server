package io.mapsmessaging.rest.responses;


import io.mapsmessaging.dto.rest.devices.DeviceInfoDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceList{
  private List<DeviceInfoDTO> list;
}