# Server MAVLink protocol transactions (MSG-433)

This is an **additive, opt-in** transaction contract in the server's MAVLink protocol implementation. It does not change the separate `mavlink` codec library, existing MAVLink telemetry topics, the ordinary single-frame outbound JSON format, or the existing `ID#endpoint#socketAddress` correlation used to select the receiving MAVLink protocol connection.

## Transport

Publish to the configured MAVLink **outbound command topic**, using the same message correlation data as ordinary MAVLink outbound messages:

`ID#<endpointId>#<remoteSocketAddress>`

The protocol instance validates that endpoint ID as before and, for UDP, uses the socket address to select the target peer.

Applications can publish normal MAVLink JSON directly: a single `{"header":...,"payload":...}` frame or an array of MAVLink `MISSION_ITEM_INT` frames representing one upload. The protocol recognises `COMMAND_LONG` and `COMMAND_INT` by message ID and derives `MISSION_COUNT` for a mission-item array. Other single frames retain the one-way send behaviour. An optional wrapper containing `messages`, `transactionId`, and `statusTopic` carries correlation metadata without a new operation vocabulary. The original `operation` envelope remains supported for compatibility.

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

## Legacy packet transaction

```json
{
  "transactionId": "packet-001",
  "operation": "PACKET",
  "statusTopic": "/mavlink/command-status",
  "messages": [
    {
      "header": {"version":"V2","systemId":255,"componentId":190,"messageId":0,"signed":false},
      "payload": {"custom_mode":0,"type":6,"autopilot":8,"base_mode":0,"system_status":4,"mavlink_version":3}
    }
  ]
}
```

`PACKET` sends one frame and finishes with `SENT`. `SENT` means the endpoint accepted the packet for transmission; it is **not** an MAVLink acknowledgement.

## Command transaction

```json
{
  "transactionId": "command-001",
  "operation": "COMMAND",
  "statusTopic": "/mavlink/command-status",
  "messages": [
    {
      "header": {"version":"V2","systemId":255,"componentId":190,"messageId":76,"signed":false},
      "payload": {
        "target_system":1,"target_component":1,"command":512,"confirmation":0,
        "param1":148,"param2":0,"param3":0,"param4":0,"param5":0,"param6":0,"param7":0
      }
    }
  ]
}
```

Only one `COMMAND_LONG` (76) or `COMMAND_INT` (75) is accepted per `COMMAND` transaction. The transaction completes on matching `COMMAND_ACK` (77); `MAV_RESULT_IN_PROGRESS` keeps it active. Existing wire encoder validation and local source identity override remain in force.

A missing initial command ACK triggers up to six bounded retransmissions (5-second response timeout). For `COMMAND_LONG`, retries increase the MAVLink `confirmation` field as prescribed by the command protocol. Once a long-running command responds `MAV_RESULT_IN_PROGRESS`, a subsequent missing progress/final ACK produces `TIMED_OUT` with an **unknown vehicle outcome**, rather than restarting an operation already in progress. ACK matching checks command number, target and source identity. <https://mavlink.io/en/services/command.html>

## Mission upload transaction

```json
{
  "transactionId": "mission-001",
  "operation": "MISSION_UPLOAD",
  "statusTopic": "/mavlink/command-status",
  "messages": [
    {
      "header": {"version":"V2","systemId":255,"componentId":190,"messageId":44},
      "payload": {"target_system":1,"target_component":1,"count":1,"mission_type":0}
    },
    {
      "header": {"version":"V2","systemId":255,"componentId":190,"messageId":73},
      "payload": {
        "target_system":1,"target_component":1,"seq":0,"mission_type":0,
        "frame":6,"command":16,"current":0,"autocontinue":1,
        "param1":0,"param2":0,"param3":0,"param4":0,
        "x":0,"y":0,"z":0
      }
    }
  ]
}
```

The first frame must be `MISSION_COUNT` (44), followed by exactly `count` ordered `MISSION_ITEM_INT` (73) frames. The protocol sends `MISSION_COUNT` first and sends an item only when the vehicle requests its sequence with `MISSION_REQUEST(_INT)`. A matching accepted `MISSION_ACK` marks **upload completed**, not mission execution completed.

One transaction may be active on each MAVLink protocol connection. Additional concurrent requests are rejected with a status event rather than interfering with the active transfer. This is a deliberate interim constraint.

## Cancellation

Publish `{"transactionId":"mission-001","operation":"CANCEL","statusTopic":"/mavlink/command-status"}` with the same engine correlation data. Cancellation stops the **local transaction** and its timeout; it does not issue a vehicle flight-mode change or alter a mission already executing on the autopilot.

## Engine status

If `statusTopic` is omitted, the default is `<configured outbound topic>/transactions`. Status events use `application/json` and are published via the normal server messaging engine:

```json
{
  "transactionId":"mission-001",
  "operation":"MISSION_UPLOAD",
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

The transaction manager's isolated tests simulate successful/rejected ACKs, wrong sources and UDP peers, lost ACKs and mission requests, out-of-order/duplicate mission items, invalid sequence recovery, retry exhaustion, duplicate and conflicting transaction IDs, status publication failures, endpoint failures, cancellation and shutdown. A server build and runtime integration test are still necessary. This is an initial implementation, not a claim of full MAVLink mission-command coverage.

## Compatibility / limitations

- Legacy packet send and telemetry publication are unchanged.
- Existing `DroneTwin`, STANAG and `MavlinkEventListSender` control paths remain in place for now, pending [MSG-434](https://mapsmessaging.atlassian.net/browse/MSG-434). They **do not automatically use** the new opt-in contract.
- Protocol transactions are connection-local and do not survive connection teardown/restart.
- The response-topic/correlation format for existing messages is unchanged; the optional `statusTopic` is a separate transaction event destination.
- Server compilation, integration and real-drone validation are required before declaring snapshot integration compatible.
