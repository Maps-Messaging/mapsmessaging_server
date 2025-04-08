package io.mapsmessaging.rest.responses;

import io.mapsmessaging.dto.rest.integration.IntegrationStatusDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IntegrationListStatus {
  private List<IntegrationStatusDTO> list;
}
