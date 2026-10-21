package io.mapsmessaging.network.protocol.impl.satellite.gateway.inmarsat.protocol.endpoints;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApiErrorTest {

  @Test
  void messageTakesPrecedenceAndCodeIsAppended() {
    ApiError error = new ApiError();
    error.setMessage("Bad request");
    error.setError("fallback");
    error.setCode(400);

    assertEquals("Bad request (code=400)", error.toString());
  }

  @Test
  void errorIsUsedWhenMessageIsAbsent() {
    ApiError error = new ApiError();
    error.setError("Satellite unavailable");

    assertEquals("Satellite unavailable", error.toString());
  }

  @Test
  void codeCanStandAloneAndEmptyErrorRendersEmptyString() {
    ApiError codeOnly = new ApiError();
    codeOnly.setCode(503);

    assertEquals(" (code=503)", codeOnly.toString());
    assertEquals("", new ApiError().toString());
  }
}
