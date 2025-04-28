package io.mapsmessaging.dto.rest.protocol.impl;

import io.mapsmessaging.dto.rest.protocol.ProtocolInformationDTO;
import io.mapsmessaging.dto.rest.session.SessionInformationDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class NatsProtocolInformation extends ProtocolInformationDTO {

  private SessionInformationDTO sessionInfo;

  public NatsProtocolInformation() {
    type = "nats";
  }
}
