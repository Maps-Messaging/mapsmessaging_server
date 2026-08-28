/*
 * Copyright [ 2020 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 */
package io.mapsmessaging.dto.rest.protocol.impl;

import io.mapsmessaging.dto.rest.protocol.ProtocolInformationDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "MAPS native protocol information")
public class MapsProtocolInformation extends ProtocolInformationDTO {

  private long localCapabilities;
  private long remoteCapabilities;
  private int negotiatedMajor;
  private int negotiatedMinor;

  public MapsProtocolInformation() {
    setType("maps");
  }
}
