package io.mapsmessaging.dto.rest.protocol.impl;

import io.mapsmessaging.dto.rest.protocol.ProtocolInformationDTO;
import io.mapsmessaging.dto.rest.session.SessionInformationDTO;

public class ExtensionProtocolInformation extends ProtocolInformationDTO {

  private SessionInformationDTO sessionInfo;

  public ExtensionProtocolInformation() {
    type = "plugin";
  }
}
