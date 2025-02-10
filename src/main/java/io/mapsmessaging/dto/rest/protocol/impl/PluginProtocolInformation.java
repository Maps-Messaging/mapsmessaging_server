package io.mapsmessaging.dto.rest.protocol.impl;

import io.mapsmessaging.dto.rest.protocol.ProtocolInformationDTO;
import io.mapsmessaging.dto.rest.session.SessionInformationDTO;

public class PluginProtocolInformation extends ProtocolInformationDTO {

  private SessionInformationDTO sessionInfo;

  public PluginProtocolInformation() {
    type = "plugin";
  }
}
