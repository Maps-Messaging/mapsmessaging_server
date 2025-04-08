package io.mapsmessaging.rest.responses;

import io.mapsmessaging.dto.rest.system.SubSystemStatusDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubSystemStatusList {
  private List<SubSystemStatusDTO> list;
}
