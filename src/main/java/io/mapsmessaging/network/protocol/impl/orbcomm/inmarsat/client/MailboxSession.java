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

import io.mapsmessaging.network.protocol.impl.orbcomm.inmarsat.client.endpoints.*;
import io.mapsmessaging.network.protocol.impl.orbcomm.inmarsat.client.model.*;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;

public final class MailboxSession {

  private final String mailboxId;
  private final String xMailbox;

  private final Supplier<String> bearer;
  private final MessagesClient messages;
  private final CommandsClient commands;
  private final DevicesClient devices;
  private final MailboxClient mailbox;

  private final Function<Integer, Optional<ErrorDef>> findErrorFn;
  private final IntFunction<String> explainErrorFn;

  MailboxSession(String mailboxId,
                 String xMailbox,
                 Supplier<String> bearer,
                 MessagesClient messages,
                 CommandsClient commands,
                 DevicesClient devices,
                 MailboxClient mailbox,
                 Function<Integer, Optional<ErrorDef>> findErrorFn,
                 IntFunction<String> explainErrorFn) {
    this.mailboxId = Objects.requireNonNull(mailboxId);
    this.xMailbox = Objects.requireNonNull(xMailbox);
    this.bearer = Objects.requireNonNull(bearer);
    this.messages = Objects.requireNonNull(messages);
    this.commands = Objects.requireNonNull(commands);
    this.devices = Objects.requireNonNull(devices);
    this.mailbox = Objects.requireNonNull(mailbox);
    this.findErrorFn = Objects.requireNonNull(findErrorFn);
    this.explainErrorFn = Objects.requireNonNull(explainErrorFn);
  }

  public String mailboxId() { return mailboxId; }

  // ---- Messaging ------------------------------------------------------------

  public MobileOriginatedResponse pollMO(String startTimeIso) {
    return messages.getMobileOriginated(bearer.get(), xMailbox, startTimeIso);
  }

  public MobileTerminatedSubmitResponse submitMT(MobileTerminatedSubmitRequest body) {
    return messages.submitMobileTerminated(bearer.get(), xMailbox, body);
  }

  public MobileTerminatedStatusResponse pollMTStatus(String startTimeIso) {
    return messages.getMobileTerminatedStatus(bearer.get(), xMailbox, startTimeIso);
  }

  public List<MobileTerminatedDetail> getMTDetails(List<String> messageIds) {
    return messages.getMobileTerminatedDetails(bearer.get(), xMailbox, messageIds);
  }

  // ---- Commands -------------------------------------------------------------

  public MobileTerminatedSubmitResponse changeMode(List<ChangeModeCommand> cmds, boolean usePost) {
    return commands.changeMode(bearer.get(), xMailbox, cmds, usePost);
  }

  public MobileTerminatedSubmitResponse mute(List<MuteCommand> cmds, boolean usePost) {
    return commands.mute(bearer.get(), xMailbox, cmds, usePost);
  }

  public MobileTerminatedSubmitResponse reset(List<ResetCommand> cmds, boolean usePost) {
    return commands.reset(bearer.get(), xMailbox, cmds, usePost);
  }

  // ---- Devices --------------------------------------------------------------

  public List<DeviceInfo> listDevices(Integer limit, Integer offset, String deviceId) {
    return devices.listDevices(bearer.get(), xMailbox, limit, offset, deviceId);
  }

  // ---- Mailbox admin --------------------------------------------------------

  public Mailbox getMailbox() {
    return mailbox.getMailbox(bearer.get(), xMailbox);
  }

  public MailboxCodecAck uploadCodec(MailboxCodecUploadRequest req) {
    return mailbox.uploadCodec(bearer.get(), xMailbox, req);
  }

  public void deleteCodec() {
    mailbox.deleteCodec(bearer.get(), xMailbox);
  }

  public MailboxPasswordResponse changeMailboxPassword(String newPasswordOrNull) {
    return mailbox.changePassword(bearer.get(), xMailbox, newPasswordOrNull);
  }

  // ---- Error helpers --------------------------------------------------------

  public Optional<ErrorDef> findError(int code) { return findErrorFn.apply(code); }

  public String explainError(int code) { return explainErrorFn.apply(code); }

  // ---- Convenience ----------------------------------------------------------

  /** Create a new handle with a different password (local xMailbox update). */
  public MailboxSession withPassword(String newPassword) {
    String newXMailbox = BaseInmarsatClient.xMailbox(mailboxId, Objects.requireNonNull(newPassword));
    return new MailboxSession(
        mailboxId, newXMailbox, bearer, messages, commands, devices, mailbox, findErrorFn, explainErrorFn
    );
  }
}
