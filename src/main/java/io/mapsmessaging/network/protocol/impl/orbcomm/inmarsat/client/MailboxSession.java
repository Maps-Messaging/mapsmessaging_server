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

package io.mapsmessaging.network.protocol.impl.orbcomm.inmarsat.client;

import com.google.gson.Gson;
import io.mapsmessaging.network.protocol.impl.orbcomm.inmarsat.client.model.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

public final class MailboxSession {
  private final URI base;                // e.g. https://apis.inmarsat.com/v1/
  private final String clientId;
  private final String clientSecret;
  private final String xMailbox;         // base64(mailboxId:mailboxPassword)

  private final HttpClient http;
  private final Gson gson;

  private final AuthClient auth;
  private final MessagesClient messages;
  private final CommandsClient commands;
  private final DevicesClient devices;
  private final MailboxClient mailbox;
  private final InfoClient info;

  private MailboxSession(URI base, String clientId, String clientSecret,
                         String mailboxId, String mailboxPassword,
                         HttpClient http, Gson gson) {
    this.base = Objects.requireNonNull(base);
    this.clientId = Objects.requireNonNull(clientId);
    this.clientSecret = Objects.requireNonNull(clientSecret);
    this.xMailbox = BaseInmarsatClient.xMailbox(
        Objects.requireNonNull(mailboxId),
        Objects.requireNonNull(mailboxPassword));

    this.http = Objects.requireNonNullElseGet(http,
        () -> HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build());
    this.gson = Objects.requireNonNullElseGet(gson, Gson::new);

    this.auth = new AuthClient(base.resolve("/"), this.http, this.gson);
    this.messages = new MessagesClient(base, this.http, this.gson);
    this.commands = new CommandsClient(base, this.http, this.gson);
    this.devices = new DevicesClient(base, this.http, this.gson);
    this.mailbox = new MailboxClient(base, this.http, this.gson);
    this.info = new InfoClient(base, this.http, this.gson);
  }

  // --------- Factory ---------
  public static MailboxSession of(String baseUrlWithVersion, // e.g. "https://apis.inmarsat.com/v1/"
                                  String clientId, String clientSecret,
                                  String mailboxId, String mailboxPassword,
                                  HttpClient http, Gson gson) {
    return new MailboxSession(URI.create(ensureTrailingSlash(baseUrlWithVersion)),
        clientId, clientSecret, mailboxId, mailboxPassword, http, gson);
  }

  private static String ensureTrailingSlash(String s) {
    return s.endsWith("/") ? s : s + "/";
  }

  // --------- Sub-clients (if you want to call them directly) ---------
  public MessagesClient messages() {
    return messages;
  }

  public CommandsClient commands() {
    return commands;
  }

  public DevicesClient devices() {
    return devices;
  }

  public MailboxClient mailbox() {
    return mailbox;
  }

  public InfoClient info() {
    return info;
  }

  // --------- Convenience wrappers (bearer handled for you) ---------
  private String bearer() {
    return auth.getValidBearer(clientId, clientSecret);
  }

  // MO
  public MobileOriginatedResponse pollMO(String startTimeIso) {
    return messages.getMobileOriginated(bearer(), xMailbox, startTimeIso);
  }

  // MT submit
  public MobileTerminatedSubmitResponse submitMT(MobileTerminatedSubmitRequest body) {
    return messages.submitMobileTerminated(bearer(), xMailbox, body);
  }

  // MT status + details
  public MobileTerminatedStatusResponse pollMTStatus(String startTimeIso) {
    return messages.getMobileTerminatedStatus(bearer(), xMailbox, startTimeIso);
  }

  public List<MobileTerminatedDetail> getMTDetails(List<String> messageIds) {
    return messages.getMobileTerminatedDetails(bearer(), xMailbox, messageIds);
  }

  // Commands (GET-with-body by default; set usePost=true if supported)
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

  // Info
  public List<ErrorDef> getErrorCodes() {
    return info.getErrorCodes(bearer());
  }
}
