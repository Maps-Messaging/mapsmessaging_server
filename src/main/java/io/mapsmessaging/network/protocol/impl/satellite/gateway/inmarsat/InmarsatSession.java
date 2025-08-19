/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      https://commonsclause.com/
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.mapsmessaging.network.protocol.impl.satellite.gateway.inmarsat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.mapsmessaging.dto.rest.config.protocol.impl.SatelliteConfigDTO;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.network.protocol.impl.satellite.gateway.inmarsat.protocol.endpoints.*;
import io.mapsmessaging.network.protocol.impl.satellite.gateway.inmarsat.protocol.model.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class InmarsatSession {
  private final Gson gson = new GsonBuilder()
      .create();

  private final Logger logger = LoggerFactory.getLogger(InmarsatSession.class);


  protected final String clientId;
  protected final String clientSecret;
  private final String mailboxId;
  private final String mailboxPassword;

  private final AuthClient auth;
  private final MessagesClient messages;
  private final CommandsClient commands;
  private final DevicesClient devices;
  private final MailboxClient mailbox;
  private final InfoClient info;

  private final Duration errorCacheTtl;
  private final AtomicReference<Map<Integer, ErrorDef>> errorIndexRef = new AtomicReference<>();
  private final AtomicReference<Instant> errorIndexLoadedAt = new AtomicReference<>();

  public InmarsatSession(SatelliteConfigDTO satelliteConfigDTO) {
    String baseUrl = satelliteConfigDTO.getBaseUrl();
    if(!baseUrl.endsWith("/")) {
      baseUrl += "/";
    }
    URI base = URI.create(baseUrl);
    Duration duration = Duration.ofSeconds(satelliteConfigDTO.getHttpRequestTimeout());
    HttpClient http = HttpClient.newBuilder().connectTimeout(duration).build();

    this.clientId = satelliteConfigDTO.getRemoteAuthConfig().getUsername();
    this.clientSecret = satelliteConfigDTO.getRemoteAuthConfig().getPassword();
    this.mailboxId = satelliteConfigDTO.getMailboxId();
    this.mailboxPassword = satelliteConfigDTO.getMailboxPassword();

    this.errorCacheTtl = Duration.ofHours(24);

    this.auth = new AuthClient(base.resolve("/"), http, this.gson);
    this.messages = new MessagesClient(base, http, this.gson);
    this.commands = new CommandsClient(base, http, this.gson);
    this.devices = new DevicesClient(base, http, this.gson);
    this.mailbox = new MailboxClient(base, http, this.gson);
    this.info = new InfoClient(base, http, this.gson);
  }

  private String bearer() {
    return auth.getValidBearer(clientId, clientSecret);
  }

  // ---- Error code index -----------------------------------------------------

  public Map<Integer, ErrorDef> errorIndex() {
    Map<Integer, ErrorDef> cached = errorIndexRef.get();
    Instant loadedAt = errorIndexLoadedAt.get();
    Instant now = Instant.now();

    boolean expired = cached == null || loadedAt == null || loadedAt.plus(errorCacheTtl).isBefore(now);
    if (!expired) return cached;

    synchronized (errorIndexRef) {
      cached = errorIndexRef.get();
      loadedAt = errorIndexLoadedAt.get();
      expired = cached == null || loadedAt == null || loadedAt.plus(errorCacheTtl).isBefore(Instant.now());
      if (expired) {
        List<ErrorDef> defs = info.getErrorCodes(bearer());
        Map<Integer, ErrorDef> idx = defs == null ? Collections.emptyMap()
            : defs.stream()
            .filter(Objects::nonNull)
            .filter(d -> d.getCode() != null)
            .collect(Collectors.toUnmodifiableMap(ErrorDef::getCode, Function.identity(), (a, b) -> b));
        errorIndexRef.set(idx);
        errorIndexLoadedAt.set(Instant.now());
        return idx;
      }
      return cached;
    }
  }

  public Optional<ErrorDef> findError(int code) {
    return Optional.ofNullable(errorIndex().get(code));
  }

  public String explainError(int code) {
    ErrorDef d = errorIndex().get(code);
    if (d == null) return "Unknown error (code=" + code + ")";
    String reason = d.getReason() == null ? "" : d.getReason();
    String message = d.getMessage() == null ? "" : d.getMessage();
    return "code=" + d.getCode() + " reason='" + reason + "' message='" + message + "'";
  }

  public void refreshErrors() {
    synchronized (errorIndexRef) {
      List<ErrorDef> defs = info.getErrorCodes(bearer());
      Map<Integer, ErrorDef> idx = defs == null ? Collections.emptyMap()
          : defs.stream()
          .filter(Objects::nonNull)
          .filter(d -> d.getCode() != null)
          .collect(Collectors.toUnmodifiableMap(ErrorDef::getCode, Function.identity(), (a, b) -> b));
      errorIndexRef.set(idx);
      errorIndexLoadedAt.set(Instant.now());
    }
  }

  // ---- Global (non-mailbox) API --------------------------------------------

  public List<ErrorDef> getErrorCodes() {
    return info.getErrorCodes(bearer());
  }

  // ---- Mailbox handle -------------------------------------------------------

  public MailboxSession openMailbox() {
    String xMailbox = BaseInmarsatClient.xMailbox(
        Objects.requireNonNull(mailboxId),
        Objects.requireNonNull(mailboxPassword)
    );
    return new MailboxSession(mailboxId, xMailbox);
  }

  // Per-mailbox API
  public final class MailboxSession {
    private final String mailboxId;
    private final String xMailbox;

    private MailboxSession(String mailboxId, String xMailbox) {
      this.mailboxId = mailboxId;
      this.xMailbox = xMailbox;
    }

    private String bearer() { return InmarsatSession.this.bearer(); }
    public String mailboxId() { return mailboxId; }

    // Messaging
    public MobileOriginatedResponse pollMO(String startTimeIso) {
      return messages.getMobileOriginated(bearer(), xMailbox, startTimeIso);
    }

    public MobileTerminatedSubmitResponse submitMT(MobileTerminatedSubmitRequest body) {
      return messages.submitMobileTerminated(bearer(), xMailbox, body);
    }

    public MobileTerminatedStatusResponse pollMTStatus(String startTimeIso) {
      return messages.getMobileTerminatedStatus(bearer(), xMailbox, startTimeIso);
    }

    public List<MobileTerminatedDetail> getMTDetails(List<String> messageIds) {
      return messages.getMobileTerminatedDetails(bearer(), xMailbox, messageIds);
    }

    // Commands
    public MobileTerminatedSubmitResponse changeMode(List<ChangeModeCommand> cmds, boolean usePost) {
      return commands.changeMode(bearer(), xMailbox, cmds, usePost);
    }

    public MobileTerminatedSubmitResponse mute(List<MuteCommand> cmds, boolean usePost) {
      return commands.mute(bearer(), xMailbox, cmds, usePost);
    }

    public MobileTerminatedSubmitResponse reset(List<ResetCommand> cmds, boolean usePost) {
      return commands.reset(bearer(), xMailbox, cmds, usePost);
    }

    // Devices
    public List<DeviceInfo> listDevices(String deviceId) {
      return devices.listDevices(bearer(), xMailbox, null, null, deviceId);
    }

    public List<DeviceInfo> listDevices(Integer limit, Integer offset, String deviceId) {
      return devices.listDevices(bearer(), xMailbox, limit, offset, deviceId);
    }

    // Mailbox admin
    public Mailbox getMailbox() {
      return mailbox.getMailbox(bearer(), xMailbox);
    }

    public MailboxCodecAck uploadCodec(MailboxCodecUploadRequest req) {
      return mailbox.uploadCodec(bearer(), xMailbox, req);
    }

    public void deleteCodec() {
      mailbox.deleteCodec(bearer(), xMailbox);
    }

    public MailboxPasswordResponse changeMailboxPassword(String newPasswordOrNull) {
      return mailbox.changePassword(bearer(), xMailbox, newPasswordOrNull);
    }

    // Error help
    public Optional<ErrorDef> findError(int code) {
      return InmarsatSession.this.findError(code);
    }

    public String explainError(int code) {
      return InmarsatSession.this.explainError(code);
    }
  }
}
