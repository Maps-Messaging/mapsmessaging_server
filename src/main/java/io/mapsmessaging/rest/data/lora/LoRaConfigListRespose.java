package io.mapsmessaging.rest.data.lora;

import io.mapsmessaging.rest.responses.BaseResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;

import java.util.List;

public class LoRaConfigListRespose extends BaseResponse {

  @Getter
  private final List<LoRaDeviceConfigInfo> data;


  public LoRaConfigListRespose(HttpServletRequest request, List<LoRaDeviceConfigInfo> data) {
    super(request);
    this.data = data;
  }
}