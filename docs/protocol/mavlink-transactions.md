# Server MAVLink protocol transactions (MSG-433)

This documents MAVLink message and plan submission through the server protocol implementation. The separate `mavlink` codec library, telemetry topics and existing `ID#endpoint#socketAddress` route correlation remain unchanged.

## Transport

Publish to the configured MAVLink **outbound command topic**, using the same message correlation data as ordinary MAVLink outbound messages:

`ID#<endpointId>#<remoteSocketAddress>`

The protocol instance validates that endpoint ID as before and, for UDP, uses the socket address to select the target peer.

Applications can publish normal MAVLink JSON directly: a single `{"header":...,"payload":...}` frame or an array of MAVLink `MISSION_ITEM_INT` frames representing one upload. The protocol recognises `COMMAND_LONG` and `COMMAND_INT` by message ID and derives `MISSION_COUNT` for a mission-item array. Other single frames retain the one-way send behaviour. An optional wrapper containing `messages`, `transactionId` and `statusTopic` carries messaging correlation metadata. An `operation` field is not accepted.

## Native MAVLink JSON

A MAVLink command is sent using its usual `header` and `payload` fields. The server handles the matching `COMMAND_ACK` without requiring an `operation` field.

To send a plan, publish **one JSON message** containing an ordered array of `MISSION_ITEM_INT` frames. The server validates sequence, target and mission type, generates `MISSION_COUNT`, and handles vehicle requests, retransmissions and the final `MISSION_ACK`.

```json
[
  {
    "header": {"version": "V2", "messageId": 73, "systemId": 255, "componentId": 190},
    "payload": {"target_system": 1, "target_component": 1, "seq": 0,
                "mission_type": 0, "frame": 6, "command": 16,
                "current": 1, "autocontinue": 1, "x": -338650000, "y": 1512100000, "z": 50}
  },
  {
    "header": {"version": "V2", "messageId": 73, "systemId": 255, "componentId": 190},
    "payload": {"target_system": 1, "target_component": 1, "seq": 1,
                "mission_type": 0, "frame": 6, "command": 16,
                "current": 0, "autocontinue": 1, "x": -338660000, "y": 1512200000, "z": 60}
  }
]
```

A wrapper such as `{"transactionId":"upload-1","statusTopic":"/mavlink/status","messages":[...]}` may be used when explicit status correlation is needed. Without one, the protocol generates a connection-local transaction ID for status events.

For `MAV_CMD_DO_SET_MISSION_CURRENT` (224), an accepted `COMMAND_ACK` is followed by confirmation of the requested sequence in `MISSION_CURRENT`. For `MAV_CMD_REQUEST_MESSAGE` (512), the protocol distinguishes an accepted command from observation of the requested message ID. If the requested message is also streamed, observing it alone cannot establish unique response provenance. Accepted commands awaiting confirmation are not blindly retransmitted.

## MAVLink command and plan exchanges

A single non-command MAVLink frame retains its ordinary one-way transmission. The protocol recognises `COMMAND_LONG` (76) and `COMMAND_INT` (75) and tracks matching `COMMAND_ACK` (77) messages. `MAV_RESULT_IN_PROGRESS` keeps a command pending rather than completing it. Command acknowledgement matching includes command number and target/source identity.

Before the first command ACK, the protocol performs up to six bounded retries with a 5-second timeout. `COMMAND_LONG` retries increment its MAVLink `confirmation` field. After an accepted or in-progress acknowledgement, it does not blindly retransmit the command. For command 224 (`MAV_CMD_DO_SET_MISSION_CURRENT`), the selected `MISSION_CURRENT` sequence confirms completion. For command 512 (`MAV_CMD_REQUEST_MESSAGE`), observing the requested telemetry message distinguishes response arrival from acknowledgement alone.

For an array of MAVLink mission items, the protocol generates `MISSION_COUNT`, sends items only on `MISSION_REQUEST(_INT)`, and handles the terminal `MISSION_ACK`. A successful upload does **not** imply the vehicle executed the mission. Duplicate and non-monotonic requests are answered with the corresponding requested item. An initial lost mission request triggers a bounded `MISSION_COUNT` retransmission. Once requests begin, a missing request or final acknowledgement produces a timeout with an uncertain outcome, not an unsolicited mission-item retransmission.

One transaction may be active per MAVLink protocol connection. A concurrent request is rejected without disturbing the active exchange.

## Local transaction lifecycle

Local cancellation is an internal transaction-manager action, not an additional MAVLink JSON command or vehicle flight-mode command. Transport/session shutdown closes outstanding local transactions. No `operation: CANCEL` envelope is accepted.

## Engine status

If `statusTopic` is omitted, the default is `<configured outbound topic>/transactions`. Status events use `application/json` and are published via the normal server messaging engine:

```json
{
  "transactionId":"mission-001",
  "status":"UPLOADING",
  "systemId":1,
  "componentId":1,
  "attempts":2,
  "retryCount":0,
  "requestedItems":1,
  "totalItems":1
}
```

States include `QUEUED`, `WAITING_FOR_ACK`, `WAITING_FOR_RESPONSE`, `WAITING_FOR_MISSION_REQUEST`, `UPLOADING`, `IN_PROGRESS`, `RETRYING`, `SENT`, `COMPLETED`, `FAILED`, `TIMED_OUT`, `CANCELLED`, `CLOSED` and `REJECTED`. `reason` is included for errors. Transaction submission to the messaging engine is not the same as device acknowledgement.

Mission uploads use six bounded retries and a 5-second response timeout. If the initial mission request is lost, `MISSION_COUNT` is retransmitted. After receiving the first item request, timeouts **do not** transmit unsolicited mission items. The vehicle must request any retransmission. If no further request or final ACK arrives, the protocol reports a timeout with an uncertain vehicle outcome. Repeated or non-monotonic item requests are answered with the requested item, while invalid/out-of-range requests and `MAV_MISSION_INVALID_SEQUENCE` remain recoverable. Other mission error ACK results terminate the transaction. <https://mavlink.io/en/services/mission.html>

Both command and mission transactions tolerate duplicate and delayed engine submissions carrying the same `transactionId` without retransmission, returning their current or cached terminal status. Reusing the same ID with different request content or destination is rejected. The completed-result cache is bounded to 128 entries per live connection, is **not persistent**, and is cleared on restart. Deduplication is therefore not a durable exactly-once guarantee; upstream producers must not reuse transaction IDs. UDP ACK matching also verifies the originating peer address when the endpoint is UDP, alongside MAVLink system/component matching. The MAVLink ACK format has no unique application transaction ID, so arbitrarily late acknowledgements for a different operation using the same MAVLink command ID cannot be disambiguated perfectly.

Status-delivery exceptions are logged via `ServerLogMessages` and isolated from transaction processing; successful wire transactions are not turned into failures by a status-topic error.

The transaction-manager tests cover acknowledgements, target and peer matching, command and plan timeouts, retransmission boundaries, duplicate submissions, rejection, local cancellation and shutdown. Compilation and real-vehicle interoperability are not established by the unit tests alone.

## Compatibility / limitations

- Legacy packet send and telemetry publication are unchanged.
- Existing `DroneTwin`, STANAG and `MavlinkEventListSender` control paths remain unchanged, pending [MSG-434](https://mapsmessaging.atlassian.net/browse/MSG-434).
- Protocol transactions are connection-local and do not survive connection teardown/restart.
- The response-topic/correlation format for existing messages is unchanged; the optional `statusTopic` is a separate transaction event destination.
- Server compilation, integration and real-drone validation are required before declaring snapshot integration compatible.
