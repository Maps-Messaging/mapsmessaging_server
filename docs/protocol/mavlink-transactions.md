# Server MAVLink protocol transactions (MSG-433)

This is an **additive, opt-in** transaction contract in the server's MAVLink protocol implementation. It does not change the separate `mavlink` codec library, existing MAVLink telemetry topics, the ordinary single-frame outbound JSON format, or the existing `ID#endpoint#socketAddress` correlation used to select the receiving MAVLink protocol connection.

## Transport

Publish to the configured MAVLink **outbound command topic**, using the same message correlation data as ordinary MAVLink outbound messages:

`ID#<endpointId>#<remoteSocketAddress>`

The protocol instance validates that endpoint ID as before and, for UDP, uses the socket address to select the target peer.

A request with top-level `operation` and `messages` (or `transactionId` for cancellation) is handled as a transaction. A legacy single MAVLink `{"header":...,"payload":...}` frame keeps the original one-way send path.

## Packet transaction

```json
{
  "transactionId": "packet-001",
  "operation": "PACKET",
  "statusTopic": "/mavlink/command-status",
  "messages": [
    {
      "header": {"version":"V2","systemId":255,"componentId":190,"messageId":0,"signed":false},
      "payload": {}
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
        "target_system":1,"target_component":1,"command":400,"confirmation":0,
        "param1":0,"param2":0,"param3":0,"param4":0,"param5":0,"param6":0,"param7":0
      }
    }
  ]
}
```

Only one `COMMAND_LONG` (76) or `COMMAND_INT` (75) is accepted per `COMMAND` transaction. The transaction completes on matching `COMMAND_ACK` (77); `MAV_RESULT_IN_PROGRESS` keeps it active. Existing wire encoder validation and local source identity override remain in force.

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

States include `QUEUED`, `WAITING_FOR_ACK`, `WAITING_FOR_MISSION_REQUEST`, `UPLOADING`, `IN_PROGRESS`, `RETRYING`, `SENT`, `COMPLETED`, `FAILED`, `TIMED_OUT`, `CANCELLED`, `CLOSED` and `REJECTED`. `reason` is included for errors. Transaction submission to the messaging engine is not the same as device acknowledgement.

The current server protocol uses the existing sender retry baseline of six retries and 5 seconds response timeout for the opt-in path. This is a first implementation for integration testing, not a claim of full MAVLink mission-command coverage.

## Compatibility / limitations

- Legacy packet send and telemetry publication are unchanged.
- Existing `DroneTwin`, STANAG and `MavlinkEventListSender` control paths remain in place for now, pending [MSG-434](https://mapsmessaging.atlassian.net/browse/MSG-434). They **do not automatically use** the new opt-in contract.
- Protocol transactions are connection-local and do not survive connection teardown/restart.
- The response-topic/correlation format for existing messages is unchanged; the optional `statusTopic` is a separate transaction event destination.
- Server compilation, integration and real-drone validation are required before declaring snapshot integration compatible.
