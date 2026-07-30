# MAVLink model capability and command review

**Repository:** `Maps-Messaging/mapsmessaging_server`  
**Branch reviewed:** `development`  
**Review date:** 2026-07-30  
**Primary package:** `io.mapsmessaging.state.mavlink.model`  
**Permitted repository change:** this report only

## Review basis and limitations

This review was performed against the `development` branch through the connected GitHub repository API. The execution container could not resolve `github.com`, and Maven is not installed, so the repository could not be cloned and the requested Maven test suite could not be executed locally. Exact commands and failures are recorded in section 7.

The branch-level GitHub code search index did not return references for several `development`-only classes. Files were therefore inspected directly by repository path and through the ServiceLoader registration file. The four registered concrete model providers are fully covered. The model factories, sender state machine, command and mission acknowledgement handlers, packet factory, message JSON encoders, and MAVLink protocol transport were also inspected.

The target behaviour table supplied with the assignment is treated as the preferred design for **ArduPilot fixed-wing aircraft**, not as a universal command table for PX4 UAVs, UGVs, or surface vehicles.

---

## 1. Executive summary

### Inventory

The `development` branch registers **four concrete model implementations**:

1. `GenericPx4UavModel`
2. `GenericPx4FixedWingUavModel`
3. `GenericPx4UgvModel`
4. `SticklebackArdupilotUsvModel`

Registration evidence: `src/main/resources/META-INF/services/io.mapsmessaging.state.mavlink.model.UxvModel:22-25`.

The review covers the **25 values in `UxvOperation`** plus the requested non-enum behaviour categories `PATROL`, `ROUTE`, `MISSION_UPLOAD`, `STATUS_TEXT`, and `NAMED_VALUE_FLOAT`, for **30 operation or behaviour categories**. The enum is defined at `src/main/java/io/mapsmessaging/state/mavlink/model/UxvOperation.java:24-56`.

### Major confirmed defects

1. **P0: shared REPOSITION emits three competing commands for every concrete model.**  
   `AbstractMissionUxvModel.reposition()` sends, in order, `COMMAND_INT/MAV_CMD_DO_REPOSITION`, an ArduPlane-specific `MAV_CMD_DO_SET_MODE` to custom mode 15, and a legacy guided `MISSION_ITEM/MAV_CMD_NAV_WAYPOINT current=2`. This is not a conditional compatibility fallback. PX4 UAV and UGV models therefore receive an ArduPlane custom-mode command, while every model receives two different goto mechanisms for one request.  
   Evidence: `src/main/java/io/mapsmessaging/state/mavlink/model/impl/AbstractMissionUxvModel.java:111-123`; `src/main/java/io/mapsmessaging/state/mavlink/messages/MavlinkCommandLongFactory.java:85-95,266-281`; `src/main/java/io/mapsmessaging/state/mavlink/messages/MavlinkMissionItemFactory.java:41-67`.

2. **P0: direct position commands use a relative-altitude frame but often populate it with MSL altitude.**  
   `COMMAND_INT` reposition and loiter use `MAV_FRAME_GLOBAL_RELATIVE_ALT_INT` but `setPosition()` selects `GeoPosition.getPreferredAltitudeMeters()`, which chooses MSL before AGL. An MSL value is therefore encoded as height relative to home.  
   Evidence: `src/main/java/io/mapsmessaging/state/mavlink/messages/MavlinkCommandIntFactory.java:45-53,65-81,100-121,140-152`; `src/main/java/io/mapsmessaging/state/drone/model/GeoPosition.java:58-77`.

3. **P0: Stickleback forces an MSL altitude and then sends it in a relative-altitude frame.**  
   Reposition and loiter mutate the caller-owned `GeoPosition` to a configurable default of 10 m MSL, then delegate to factories using frame 6. The configured “MSL” value is consequently interpreted as relative altitude.  
   Evidence: `src/main/java/io/mapsmessaging/state/mavlink/model/impl/usv/SticklebackArdupilotUsvModel.java:43-44,71-76,80-114,240-262`; `src/main/java/io/mapsmessaging/state/mavlink/messages/MavlinkCommandIntFactory.java:40,45-53,65-81`.

4. **P1: the legacy guided waypoint has incompatible altitude assumptions.**  
   It declares `MAV_FRAME_GLOBAL_RELATIVE_ALT` but reads only `altitudeMslMeters`. It treats MSL as relative altitude and throws a `NullPointerException` when only AGL altitude is present.  
   Evidence: `src/main/java/io/mapsmessaging/state/mavlink/messages/MavlinkMissionItemFactory.java:28-36,41-67`.

5. **P1: transport failures are swallowed below the sender.**  
   `MavlinkProtocol.sendMessage()` catches `Throwable`, prints the stack trace and completes the broker message. `sendData()` catches and logs encoding or endpoint failures without propagating them. `MavlinkEventListSender` therefore cannot receive the transport exception and commonly reports a later acknowledgement timeout instead of the real failure.  
   Evidence: `src/main/java/io/mapsmessaging/network/protocol/impl/mavlink/MavlinkProtocol.java:183-233`; `src/main/java/io/mapsmessaging/state/mavlink/sender/MavlinkEventListSender.java:306-310,336-340,362-368`.

6. **P1: model command source IDs are ignored and outbound JSON starts at source `0/0`.**  
   `UxvCommandContext` contains source system and component IDs, but command factories do not use them. Message JSON encoders emit `systemId=0` and `componentId=0`. `MavlinkProtocol` repairs these only when local MAVLink identity is configured; otherwise header validation rejects the event.  
   Evidence: `src/main/java/io/mapsmessaging/state/mavlink/model/UxvCommandContext.java:27-37`; `src/main/java/io/mapsmessaging/state/mavlink/messages/MavlinkCommandInt.java:53-65`; `src/main/java/io/mapsmessaging/state/mavlink/messages/MavlinkCommandLong.java:51-64`; `src/main/java/io/mapsmessaging/network/protocol/impl/mavlink/MavlinkProtocol.java:480-509`.

7. **P1: infinite polygon patrol is impossible in the current mission model.**  
   `MissionPlan.iterations` must be at least 1 and `MavlinkMissionItemIntFactory.jump()` rejects a negative repeat count, so `MAV_CMD_DO_JUMP param2=-1` cannot be generated.  
   Evidence: `src/main/java/io/mapsmessaging/state/mavlink/model/MissionPlan.java:27-42`; `src/main/java/io/mapsmessaging/state/mavlink/messages/MavlinkMissionItemIntFactory.java:134-160`.

### Major missing capabilities

- Infinite polygon patrol.
- Dedicated route or corridor execution with final loiter.
- Integrated mission upload command set containing `MISSION_COUNT` followed by requested `MISSION_ITEM_INT` messages.
- Mission execution completion based on `MISSION_CURRENT` and `MISSION_ITEM_REACHED`.
- Mission clear, replacement, preemption, and execution cancellation.
- Loiter turns, loiter-to-altitude, and continue-and-change-altitude.
- Mission-form take-off and landing for fixed-wing workflows.
- `SET_ALTITUDE`, `SET_DEPTH`, `HOLD_DEPTH`, `DIVE`, `SURFACE`, and `SET_TURN_RATE`.
- Outbound `STATUSTEXT` and `NAMED_VALUE_FLOAT`.

### Important valid platform-specific differences

- PX4 `STOP` is implemented as `DO_PAUSE_CONTINUE param1=0`, not ArduPilot current-position loiter.
- PX4 UGV does not declare or implement loiter/orbit.
- PX4 UAV orbit uses `MAV_CMD_DO_ORBIT`.
- PX4 fixed-wing timed waypoint holds are converted to `NAV_LOITER_TIME` with a configurable/default radius.
- Take-off and land are immediate `COMMAND_LONG` operations in the generic PX4 UAV model rather than mission items. This requires live PX4 validation but is not automatically an ArduPilot-table contradiction.

### Sending-path assessment

The in-memory `MavlinkEventListSender` state machine is generally disciplined: it has a start guard, waits for required acknowledgements, resends the same waiting event, cancels scheduled timeouts, blocks late acknowledgements after terminal state, and delivers one terminal callback. Its existing unit tests cover much of this behaviour.

Two important weaknesses remain:

- `SEND_INDEX` processing lacks the waiting-message identity check used by `ADVANCE`, allowing concurrent duplicate mission requests to race and potentially clear or replace a newer waiting state.
- command acknowledgement matching is command-ID based and, by default, does not validate target fields; it does not match the source system/component from the received MAVLink frame.

The larger reliability defect is below the sender: encoding and endpoint failures are swallowed by `MavlinkProtocol`, so the sender cannot fail promptly or accurately.

### Mission upload readiness

**Mission upload is partially implemented, not production-ready.**

The repository contains:

- `MISSION_COUNT` message ID 44.
- `MISSION_ITEM_INT` message ID 73.
- `MISSION_REQUEST_INT` and legacy `MISSION_REQUEST` packet handling.
- `MISSION_ACK` handling.
- duplicate-latest-item retransmission.
- range, target, mission-type, timeout, and retry machinery.

However:

- model `buildMission()` returns mission items only and does not prepend `MISSION_COUNT`;
- no inspected production path assembles the model output into a complete upload envelope;
- upload completion is treated as sender completion, with no mission execution state;
- `MISSION_CURRENT` is decoded but not used by the mission acknowledgement handler;
- `MISSION_ITEM_REACHED` is not handled by `MavlinkPacketFactory`;
- cancellation, replacement, and preemption are not represented as a mission protocol state machine.

Evidence: `src/main/java/io/mapsmessaging/state/mavlink/model/impl/AbstractMissionUxvModel.java:172-207`; `src/main/java/io/mapsmessaging/state/mavlink/messages/MavlinkMissionCount.java:30-73`; `src/main/java/io/mapsmessaging/state/mavlink/sender/MavlinkMissionAcknowledgementHandler.java:34-266`; `src/main/java/io/mapsmessaging/state/mavlink/packet/MavlinkPacketFactory.java:28-52`.

---

## 2. Model inventory

| Model class | Model names and aliases | Autopilot | Vehicle type | Base class | Declared capabilities |
|---|---|---|---|---|---|
| `GenericPx4UavModel` | `generic-px4-uav`; no aliases registered | PX4 | UAV | `GenericPx4UxvModel` | ARM, DISARM, SET_HOME, RETURN_TO_HOME, REPOSITION, HOLD_POSITION, STOP, PAUSE_VEHICLE, RESUME_VEHICLE, BUILD_MISSION, START_MISSION, NAVIGATE, TAKE_OFF, LAND, SET_SPEED, SET_HEADING, ORBIT, LOITER |
| `GenericPx4FixedWingUavModel` | `generic-px4-fixed-wing-uav`; no aliases registered | PX4 | fixed-wing UAV | `GenericPx4UavModel` | Same inherited declaration as `GenericPx4UavModel` |
| `GenericPx4UgvModel` | `generic-px4-ugv`; no aliases registered | PX4 | UGV | `GenericPx4UxvModel` | ARM, DISARM, SET_HOME, RETURN_TO_HOME, REPOSITION, HOLD_POSITION, STOP, PAUSE_VEHICLE, RESUME_VEHICLE, BUILD_MISSION, START_MISSION, NAVIGATE, SET_SPEED, SET_HEADING |
| `SticklebackArdupilotUsvModel` | `stickleback-ardupilot-usv`; no aliases registered | ArduPilot | USV using fixed-wing firmware | `GenericArduPilotUxvModel` | ARM, DISARM, SET_HOME, REPOSITION, HOLD_POSITION, STOP, PAUSE_VEHICLE, RESUME_VEHICLE, BUILD_MISSION, START_MISSION, NAVIGATE, SET_SPEED, SET_HEADING, LOITER |

Evidence:

- provider inventory: `src/main/resources/META-INF/services/io.mapsmessaging.state.mavlink.model.UxvModel:22-25`;
- exact-name lookup and registration: `src/main/java/io/mapsmessaging/state/mavlink/model/ModelManager.java:42-45,60-78`;
- PX4 UAV capabilities: `src/main/java/io/mapsmessaging/state/mavlink/model/impl/uav/GenericPx4UavModel.java:45-78`;
- fixed-wing inheritance/name: `src/main/java/io/mapsmessaging/state/mavlink/model/impl/uav/GenericPx4FixedWingUavModel.java:36-50`;
- UGV capabilities: `src/main/java/io/mapsmessaging/state/mavlink/model/impl/ugv/GenericPx4UgvModel.java:37-59`;
- Stickleback capabilities: `src/main/java/io/mapsmessaging/state/mavlink/model/impl/usv/SticklebackArdupilotUsvModel.java:39-68`.

### Discovery and aliases

`ModelManager` uses `ServiceLoader<UxvModel>` and registers only `model.getModelName()` as a map key. Lookup is exact and case-sensitive. No alias method, alias collection, normalization, or fallback is present. `getRequiredModel()` is misleadingly named because it returns `null` when no model is found.

Evidence: `src/main/java/io/mapsmessaging/state/mavlink/model/ModelManager.java:31-78`.

---

## 3. Behaviour implementation matrix

Status terms use the categories requested by the assignment.

### 3.1 Target fixed-wing behaviour mapping

| Behaviour | Expected mechanism | Model | Current implementation | Status | Evidence |
|---|---|---|---|---|---|
| Move to position and remain | `COMMAND_INT` 75, `DO_REPOSITION` 192 | PX4 UAV | Three events: `DO_REPOSITION`, ArduPlane mode 15, legacy guided waypoint | Implemented with confirmed defects | `src/main/java/io/mapsmessaging/state/mavlink/model/impl/AbstractMissionUxvModel.java:111-123`; `src/main/java/io/mapsmessaging/state/mavlink/messages/MavlinkCommandLongFactory.java:85-95` |
| Move to position and remain | same | PX4 fixed-wing UAV | Same inherited three-event sequence | Implemented with confirmed defects | `src/main/java/io/mapsmessaging/state/mavlink/model/impl/AbstractMissionUxvModel.java:111-123` |
| Move to position and remain | same | PX4 UGV | Same inherited sequence, including ArduPlane mode command | Implemented with confirmed defects | `src/main/java/io/mapsmessaging/state/mavlink/model/impl/AbstractMissionUxvModel.java:111-123` |
| Move to position and remain | same | Stickleback USV | Same sequence after mutating MSL altitude to configured value | Implemented with confirmed defects | `src/main/java/io/mapsmessaging/state/mavlink/model/impl/usv/SticklebackArdupilotUsvModel.java:71-76`; `src/main/java/io/mapsmessaging/state/mavlink/model/impl/AbstractMissionUxvModel.java:111-123` |
| Legacy guided goto | documented fallback only | All models | Always sent as third event, `MISSION_ITEM`, frame 3, `current=2` | Confirmed contradiction | `src/main/java/io/mapsmessaging/state/mavlink/messages/MavlinkMissionItemFactory.java:28-67` |
| Loiter at specified position indefinitely | guided reposition/hold as appropriate | PX4 UAV | `COMMAND_INT` 75, `NAV_LOITER_UNLIM` 17, frame 6 | Implemented differently; firmware validation required | `src/main/java/io/mapsmessaging/state/mavlink/model/impl/uav/GenericPx4UavModel.java:139-179`; `src/main/java/io/mapsmessaging/state/mavlink/messages/MavlinkCommandIntFactory.java:65-81` |
| Loiter at specified position indefinitely | same | PX4 fixed-wing UAV | inherited command 17, radius supplied | Implemented differently; concerns | `src/main/java/io/mapsmessaging/state/mavlink/model/impl/uav/GenericPx4FixedWingUavModel.java:36-55` |
| Loiter at specified position indefinitely | same | PX4 UGV | no LOITER capability | Explicitly unsupported | `src/main/java/io/mapsmessaging/state/mavlink/model/impl/ugv/GenericPx4UgvModel.java:41-59` |
| Loiter at specified position indefinitely | same | Stickleback USV | `COMMAND_INT` 17 after forced altitude mutation | Implemented with concerns | `src/main/java/io/mapsmessaging/state/mavlink/model/impl/usv/SticklebackArdupilotUsvModel.java:80-114` |
| Loiter at current position | mode/navigation action | PX4 UAV/fixed-wing | `STOP` maps to pause, not loiter | Valid platform-specific difference | `src/main/java/io/mapsmessaging/state/mavlink/model/impl/px4/GenericPx4UxvModel.java:48-51` |
| Loiter at current position | same | PX4 UGV | `STOP` maps to pause | Valid platform-specific difference | `src/main/java/io/mapsmessaging/state/mavlink/model/impl/px4/GenericPx4UxvModel.java:48-51` |
| Loiter at current position | same | Stickleback USV | `COMMAND_INT` 17 with frame 6 and zero coordinates/altitude | Unverified difference | `src/main/java/io/mapsmessaging/state/mavlink/model/impl/AbstractMissionUxvModel.java:131-136`; `src/main/java/io/mapsmessaging/state/mavlink/messages/MavlinkCommandIntFactory.java:84-98` |
| Mission loiter indefinitely | `MISSION_ITEM_INT` 73, command 17 | PX4 UAV/fixed-wing | supported as `PlanItemType.LOITER` | Implemented and structurally correct | `src/main/java/io/mapsmessaging/state/mavlink/model/impl/uav/GenericPx4UavModel.java:181-247`; `src/main/java/io/mapsmessaging/state/mavlink/messages/MavlinkMissionItemIntFactory.java:88-108` |
| Mission loiter indefinitely | same | PX4 UGV | rejected | Explicitly unsupported | `src/main/java/io/mapsmessaging/state/mavlink/model/impl/ugv/GenericPx4UgvModel.java:62-82` |
| Mission loiter indefinitely | same | Stickleback USV | supported | Implemented with altitude concerns | `src/main/java/io/mapsmessaging/state/mavlink/model/impl/usv/SticklebackArdupilotUsvModel.java:121-160` |
| Patrol polygon indefinitely | mission waypoints + `DO_JUMP -1` | All models | finite iterations only; negative jump repeats rejected | Missing | `src/main/java/io/mapsmessaging/state/mavlink/model/MissionPlan.java:27-42`; `src/main/java/io/mapsmessaging/state/mavlink/messages/MavlinkMissionItemIntFactory.java:134-160` |
| Route/corridor then hold final | waypoints + final loiter | PX4 UAV/fixed-wing | manually expressible with `MissionPlan`; no ROUTE operation or upload integration | Partially implemented | `src/main/java/io/mapsmessaging/state/mavlink/model/impl/uav/GenericPx4UavModel.java:181-247`; `src/main/java/io/mapsmessaging/state/mavlink/model/impl/AbstractMissionUxvModel.java:172-200` |
| Route/corridor then hold final | same | PX4 UGV | final loiter rejected | Missing | `src/main/java/io/mapsmessaging/state/mavlink/model/impl/ugv/GenericPx4UgvModel.java:62-82` |
| Route/corridor then hold final | same | Stickleback USV | manually expressible; no dedicated route/upload integration | Partially implemented | `src/main/java/io/mapsmessaging/state/mavlink/model/impl/usv/SticklebackArdupilotUsvModel.java:121-160` |
| Finite route and complete | waypoint mission + explicit terminal action | All models | `NAVIGATE` builds waypoints then separate mission start and external terminal action; no mission terminal item or telemetry completion | Implemented with concerns | `src/main/java/io/mapsmessaging/state/mavlink/model/impl/AbstractMissionUxvModel.java:64-81` |
| Pause mission | `COMMAND_LONG` 76, command 193, param1=0 | All models | shared `pauseVehicle`; PX4 stop also uses pause | Implemented and correct at command-construction level | `src/main/java/io/mapsmessaging/state/mavlink/model/impl/AbstractMissionUxvModel.java:138-149`; `src/main/java/io/mapsmessaging/state/mavlink/messages/MavlinkCommandLongFactory.java:189-203` |
| Resume mission | command 193, param1=1 | All models | shared `resumeVehicle` | Implemented and correct at command-construction level | `src/main/java/io/mapsmessaging/state/mavlink/messages/MavlinkCommandLongFactory.java:205-218` |
| Timed loiter | mission item 19 | PX4 UAV/fixed-wing | direct and mission forms | Implemented | `src/main/java/io/mapsmessaging/state/mavlink/model/impl/uav/GenericPx4UavModel.java:150-176,223-247` |
| Timed loiter | same | PX4 UGV | unsupported | Explicitly unsupported | `src/main/java/io/mapsmessaging/state/mavlink/model/impl/ugv/GenericPx4UgvModel.java:77-81` |
| Timed loiter | same | Stickleback USV | direct and mission forms | Implemented with altitude concerns | `src/main/java/io/mapsmessaging/state/mavlink/model/impl/usv/SticklebackArdupilotUsvModel.java:92-114,141-160` |
| Loiter for turns | mission item 18 | All models | no message factory or plan item | Missing | `src/main/java/io/mapsmessaging/state/mavlink/model/PlanItemType.java:24-30` is limited to waypoint/loiter/orbit/hold/RTL |
| Loiter while changing altitude | mission item 31 | All models | absent | Missing | concrete mission switches in `src/main/java/io/mapsmessaging/state/mavlink/model/impl/uav/GenericPx4UavModel.java:181-201`, `src/main/java/io/mapsmessaging/state/mavlink/model/impl/ugv/GenericPx4UgvModel.java:62-82`, `src/main/java/io/mapsmessaging/state/mavlink/model/impl/usv/SticklebackArdupilotUsvModel.java:121-138` |
| Continue and change altitude | mission item 30 | All models | absent | Missing | same mission switches as above |
| Change speed | command 178 | All models | shared `COMMAND_LONG`, groundspeed selector, speed in param2, throttle -1 | Implemented with platform-validation concerns | `src/main/java/io/mapsmessaging/state/mavlink/model/impl/AbstractMissionUxvModel.java:210-225` |
| Return to launch | command 20 | PX4 UAV/fixed-wing/UGV | declared and implemented | Implemented | `src/main/java/io/mapsmessaging/state/mavlink/model/impl/px4/GenericPx4UxvModel.java:39-46` |
| Return to launch | command 20 | Stickleback USV | inherited implementation, omitted from declared capabilities | Implemented but not declared supported | `src/main/java/io/mapsmessaging/state/mavlink/model/impl/ardupilot/GenericArduPilotUxvModel.java:41-48`; `src/main/java/io/mapsmessaging/state/mavlink/model/impl/usv/SticklebackArdupilotUsvModel.java:50-68` |
| Take off | mission item 22 preferred for fixed wing | PX4 UAV/fixed-wing | immediate `COMMAND_LONG` 22, param7 altitude | Implemented differently; firmware validation required | `src/main/java/io/mapsmessaging/state/mavlink/model/impl/uav/GenericPx4UavModel.java:81-95` |
| Take off | same | PX4 UGV / Stickleback | absent | Explicitly unsupported or missing | capability declarations above |
| Land | mission item 21 preferred for fixed wing | PX4 UAV/fixed-wing | immediate `COMMAND_LONG` 21, default zero parameters | Implemented differently; firmware validation required | `src/main/java/io/mapsmessaging/state/mavlink/model/impl/uav/GenericPx4UavModel.java:97-109` |
| Land | same | PX4 UGV / Stickleback | absent | Explicitly unsupported or missing | capability declarations above |
| Arm/disarm | command 400 | All models | shared `COMMAND_LONG`, normal arm/disarm | Implemented | `src/main/java/io/mapsmessaging/state/mavlink/model/impl/AbstractMissionUxvModel.java:83-90`; `src/main/java/io/mapsmessaging/state/mavlink/messages/MavlinkCommandLongFactory.java:284-301` |
| Set home | command 179 | All models | `COMMAND_LONG`; current or float lat/lon/altitude | Implemented with precision/frame concerns | `src/main/java/io/mapsmessaging/state/mavlink/model/impl/AbstractMissionUxvModel.java:93-109` |

### 3.2 Additional discovered operations

| Behaviour | Expected mechanism | Model | Current implementation | Status | Evidence |
|---|---|---|---|---|---|
| HOLD_POSITION | platform hold | All models | maps to pause command | Implemented differently for platform reasons | `src/main/java/io/mapsmessaging/state/mavlink/model/impl/AbstractMissionUxvModel.java:126-129` |
| STOP | platform safe stop | PX4 models | maps to pause | Valid platform-specific difference | `src/main/java/io/mapsmessaging/state/mavlink/model/impl/px4/GenericPx4UxvModel.java:48-51` |
| STOP | surface hold | Stickleback | loiter command at zero coordinates | Implemented with unverified behaviour | `src/main/java/io/mapsmessaging/state/mavlink/messages/MavlinkCommandIntFactory.java:84-98` |
| SET_HEADING | command 115 | All models | `MAV_CMD_CONDITION_YAW`, clockwise absolute heading | Implemented with firmware concerns | `src/main/java/io/mapsmessaging/state/mavlink/model/impl/AbstractMissionUxvModel.java:227-242` |
| ORBIT | command 34 | PX4 UAV/fixed-wing | signed radius for direction, frame 0 | Implemented with altitude-frame concerns for AGL input | `src/main/java/io/mapsmessaging/state/mavlink/model/impl/uav/GenericPx4UavModel.java:111-137`; `src/main/java/io/mapsmessaging/state/mavlink/messages/MavlinkCommandIntFactory.java:55-63` |
| ORBIT | command 34 | UGV/Stickleback | absent | Explicitly unsupported | capability declarations and mission switches |
| BUILD_MISSION | mission items | All models | produces item list, optional finite jump | Implemented with concerns | `src/main/java/io/mapsmessaging/state/mavlink/model/impl/AbstractMissionUxvModel.java:172-200` |
| START_MISSION | command 300 | All models | shared `COMMAND_LONG` | Implemented | `src/main/java/io/mapsmessaging/state/mavlink/model/impl/AbstractMissionUxvModel.java:202-208` |
| CLEAR_MISSION | mission clear protocol | All models | enum/default only; not declared or overridden | Missing | `src/main/java/io/mapsmessaging/state/mavlink/model/UxvOperation.java:24-56`; `src/main/java/io/mapsmessaging/state/mavlink/model/UxvModel.java:35-123` |
| MISSION_UPLOAD | mission protocol | All models | components exist but no complete model-produced upload set demonstrated | Partially implemented | `src/main/java/io/mapsmessaging/state/mavlink/messages/MavlinkMissionCount.java:30-73`; `src/main/java/io/mapsmessaging/state/mavlink/sender/MavlinkMissionAcknowledgementHandler.java:34-266` |
| SET_ALTITUDE | platform command | UAV models | default unsupported; not declared | Missing | `src/main/java/io/mapsmessaging/state/mavlink/model/UavModel.java:34-36`; capability declarations |
| DIVE/SURFACE/SET_DEPTH/HOLD_DEPTH | platform command | All registered models | absent | Missing | `src/main/java/io/mapsmessaging/state/mavlink/model/UxvOperation.java:24-56`; capability declarations |
| SET_TURN_RATE | platform command | All registered models | absent | Missing | same |
| STATUS_TEXT outbound | `STATUSTEXT` | All models | packet decoding only; no outbound model operation found | Missing | `src/main/java/io/mapsmessaging/state/mavlink/packet/MavlinkPacketFactory.java:43-50` |
| NAMED_VALUE_FLOAT outbound | `NAMED_VALUE_FLOAT` | All models | Stickleback interprets inbound detections; no outbound operation found | Implemented inbound, missing outbound | `src/main/java/io/mapsmessaging/state/mavlink/model/impl/usv/SticklebackArdupilotUsvModel.java:186-238`; `src/main/java/io/mapsmessaging/state/mavlink/packet/MavlinkPacketFactory.java:43-48` |

---

## 4. Detailed per-model review

### 4.1 `GenericPx4UavModel`

#### Supported operations

The model declares 18 operations at `src/main/java/io/mapsmessaging/state/mavlink/model/impl/uav/GenericPx4UavModel.java:60-78`.

#### Generated commands

- ARM/DISARM: `COMMAND_LONG` 76, command 400.
- SET_HOME: `COMMAND_LONG` 76, command 179.
- RETURN_TO_HOME: `COMMAND_LONG` 76, command 20.
- REPOSITION: inherited three-event sequence:
  1. `COMMAND_INT` 75, command 192, frame 6;
  2. `COMMAND_LONG` 76, command 176, custom mode 15;
  3. legacy `MISSION_ITEM`, command 16, `current=2`.
- HOLD_POSITION/PAUSE: `COMMAND_LONG` 76, command 193, param1=0.
- RESUME: command 193, param1=1.
- STOP: pause command.
- TAKE_OFF: `COMMAND_LONG` 76, command 22, altitude in param7.
- LAND: `COMMAND_LONG` 76, command 21.
- SET_SPEED: `COMMAND_LONG` 76, command 178, groundspeed in param2.
- SET_HEADING: `COMMAND_LONG` 76, command 115.
- ORBIT: `COMMAND_INT` 75, command 34.
- LOITER: `COMMAND_INT` 75, command 17 or 19.
- BUILD_MISSION: `MISSION_ITEM_INT` 73 for waypoint, loiter, or RTL.
- START_MISSION: `COMMAND_LONG` 76, command 300.

Evidence: `src/main/java/io/mapsmessaging/state/mavlink/model/impl/uav/GenericPx4UavModel.java:81-247`; `src/main/java/io/mapsmessaging/state/mavlink/model/impl/AbstractMissionUxvModel.java:83-242`.

#### Coordinate and altitude handling

Direct reposition and loiter always use relative-altitude frame 6, but use whichever altitude `GeoPosition` prefers, MSL first. Orbit uses global frame 0 but also accepts preferred altitude, so AGL input would be encoded as global altitude. Mission items resolve MSL to frame 5 and AGL to terrain frame 11, which is internally more coherent than the direct-command path.

Evidence: `src/main/java/io/mapsmessaging/state/mavlink/messages/MavlinkCommandIntFactory.java:45-81,100-152`; `src/main/java/io/mapsmessaging/state/mavlink/messages/MavlinkMissionItemIntFactory.java:181-247`; `src/main/java/io/mapsmessaging/state/drone/model/GeoPosition.java:72-77`.

#### Acknowledgement and completion

All `COMMAND_INT` and `COMMAND_LONG` events require `COMMAND_ACK`. `MISSION_ITEM_INT` does not use command acknowledgement and must be driven by the mission handler. Accepted command acknowledgements advance the list but do not prove take-off, landing, reposition, loiter, orbit, or RTL completion. No model-specific telemetry completion logic exists here.

#### Defects and risks

- inherited ArduPlane mode command in PX4 reposition;
- duplicate goto forms in one command set;
- direct altitude/frame mismatch;
- `RepositionRequest.yawDegrees` silently unused;
- take-off/land physical completion not monitored;
- orbit AGL value can be encoded in global frame;
- mission upload envelope absent from model output.

### 4.2 `GenericPx4FixedWingUavModel`

This model inherits all generic PX4 UAV commands and capability declarations.

Its valid fixed-wing specialization is mission hold behaviour:

- a waypoint with non-zero hold duration becomes `NAV_LOITER_TIME` rather than a stationary waypoint hold;
- a default loiter radius of 50 m is used when no radius is supplied;
- non-positive fixed-wing loiter radii and fixed-wing loiter yaw are rejected.

Evidence: `src/main/java/io/mapsmessaging/state/mavlink/model/impl/uav/GenericPx4FixedWingUavModel.java:36-135`.

This is a reasonable platform-specific distinction. It does not repair the inherited direct reposition defects. It also does not populate `DO_REPOSITION` plane loiter radius or direction parameters; those remain `NaN` in the shared factory.

Evidence: `src/main/java/io/mapsmessaging/state/mavlink/messages/MavlinkCommandIntFactory.java:45-52`.

### 4.3 `GenericPx4UgvModel`

The UGV declares 14 operations and intentionally omits LOITER, ORBIT, TAKE_OFF, and LAND.

Mission support is limited to:

- waypoint command 16;
- RTL command 20.

LOITER, ORBIT, and HOLD_POSITION mission items are rejected. Mission item speed, altitude, and depth are also rejected.

Evidence: `src/main/java/io/mapsmessaging/state/mavlink/model/impl/ugv/GenericPx4UgvModel.java:37-104`.

Valid platform differences:

- no aerial altitude operations;
- no orbit or fixed-wing loiter;
- stop/pause uses `DO_PAUSE_CONTINUE`.

Confirmed problem:

- UGV reposition still receives the shared ArduPlane mode-15 command and legacy guided waypoint. This is the clearest demonstration that the shared reposition implementation is at the wrong abstraction level.

### 4.4 `SticklebackArdupilotUsvModel`

The model declares 14 operations and adds inbound `NAMED_VALUE_FLOAT` detection interpretation.

#### Commands

It inherits ARM, DISARM, SET_HOME, RETURN_TO_HOME implementation, REPOSITION, HOLD, STOP, PAUSE, RESUME, mission building/start, speed, and heading. It overrides LOITER and REPOSITION mainly to force an altitude value.

#### Capability declaration inconsistency

`GenericArduPilotUxvModel.returnToHome()` is a concrete implementation, but `RETURN_TO_HOME` is omitted from the Stickleback supported-operation set.

Evidence: `src/main/java/io/mapsmessaging/state/mavlink/model/impl/ardupilot/GenericArduPilotUxvModel.java:41-48`; `src/main/java/io/mapsmessaging/state/mavlink/model/impl/usv/SticklebackArdupilotUsvModel.java:50-68`.

#### Altitude and mutation defects

Both `reposition()` and `loiter()` mutate `request.position()` by writing `altitudeMslMeters`. The object supplied by the caller is therefore modified as a side effect. The default configured value is 10 m. The downstream command uses relative-altitude frame 6.

Evidence: `src/main/java/io/mapsmessaging/state/mavlink/model/impl/usv/SticklebackArdupilotUsvModel.java:43-44,71-76,80-114,240-262`.

#### Yaw inconsistency

The class overrides `rejectYaw()` with an empty method. Direct LOITER therefore accepts a yaw value but ignores it. Mission validation, by contrast, reports that loiter yaw is not mapped.

Evidence: `src/main/java/io/mapsmessaging/state/mavlink/model/impl/usv/SticklebackArdupilotUsvModel.java:117-119,169-170`.

#### Stop behaviour

STOP becomes `NAV_LOITER_UNLIM` with latitude, longitude, and altitude all zero. Repository source does not establish whether the deployed ArduPilot fixed-wing firmware treats this as current-position loiter rather than a global 0/0 target. This requires live validation.

#### Detection handling

Inbound valid `NAMED_VALUE_FLOAT` values near 1 produce DETECTED events and values near 0 produce LOST events. Contact identity is a name-derived UUID and detected contacts receive the configured TTL. This is implemented inbound behaviour, not an outbound named-value operation.

Evidence: `src/main/java/io/mapsmessaging/state/mavlink/model/impl/usv/SticklebackArdupilotUsvModel.java:186-238`.

---

## 5. Differences from the target mapping

### 5.1 Confirmed contradictions

1. **REPOSITION is not one selected mechanism or a documented fallback.** It always emits three mechanisms.
2. **PX4 and UGV reposition includes an ArduPlane custom-mode command.**
3. **Direct relative-altitude commands can carry MSL altitude.**
4. **Stickleback explicitly writes an MSL field before sending frame 6.**
5. **Legacy guided waypoint uses relative frame but reads MSL only.**
6. **Infinite `DO_JUMP -1` cannot be represented.**
7. **Mission item lists are not complete mission-upload command sets because they omit `MISSION_COUNT`.**
8. **Stickleback RTL is implemented but not declared.**
9. **Stickleback direct loiter accepts yaw while silently discarding it.**
10. **Transport failures are logged and swallowed, preventing correct sender failure propagation.**

### 5.2 Valid platform-specific differences

1. PX4 STOP/HOLD uses `DO_PAUSE_CONTINUE`.
2. PX4 UGV omits loiter, orbit, take-off, and landing.
3. PX4 UAV supports `DO_ORBIT`.
4. PX4 fixed-wing converts timed waypoint holds to timed loiter.
5. Fixed-wing loiter requires a positive radius and defaults to 50 m.
6. Mission altitude frame selection distinguishes MSL, terrain/AGL, and relative altitude.

### 5.3 Unverified differences

1. Whether deployed PX4 firmware accepts immediate `COMMAND_LONG` take-off and land in the intended vehicle modes.
2. Whether `COMMAND_INT NAV_LOITER_UNLIM/TIME` is accepted as a guided-position command by each PX4 and ArduPilot firmware/version.
3. Whether the Stickleback firmware treats zero coordinates in command 17 as current position.
4. Whether `MAV_CMD_CONDITION_YAW` is honoured on each registered vehicle type.
5. Whether signed orbit radius is honoured consistently for clockwise/counter-clockwise direction by the deployed PX4 version.
6. Whether Stickleback’s component ID and local GCS identity intentionally differ from standard autopilot/GCS component IDs.
7. Whether the desired surface-vehicle altitude convention is relative to home, MSL, terrain, or ignored by the vehicle.
8. Whether the deployed fixed-wing firmware honours loiter direction via `DO_REPOSITION` param4 and radius via param3.

---

## 6. MAVLink sending-path validation

### 6.1 Path

The verified path is:

```text
model operation
  -> UxvModelCommandSet(operation, modelName, immutable message list)
  -> MavlinkEventListSender
  -> MavlinkEventSender.send(message)
  -> message.toMavlinkJsonObject()
  -> broker outbound topic/correlation routing
  -> MavlinkProtocol.sendMessage()
  -> sequence/source identity override
  -> formatter.parseFromJson()
  -> Packet(ByteBuffer)
  -> EndPoint.sendPacket()
  -> vehicle response published inbound
  -> MavlinkPacketFactory
  -> command or mission acknowledgement handler
  -> MavlinkEventListSender action
  -> completion callback
```

`UxvModelCommandSet` preserves the operation, model name, and copied message order: `src/main/java/io/mapsmessaging/state/mavlink/model/UxvModelCommandSet.java:29-49`.

The concrete binding between `MavlinkEventSender` and the broker publication layer was not discoverable through the available `development` branch index. The sender interface itself is at `src/main/java/io/mapsmessaging/state/mavlink/sender/MavlinkEventSender.java:27-31`. The transport endpoint path is explicit in `MavlinkProtocol`: `src/main/java/io/mapsmessaging/network/protocol/impl/mavlink/MavlinkProtocol.java:183-233`.

### 6.2 Event-list state transitions

Conceptually:

```text
NEW
  -> STARTED
  -> SEND next event
     -> no acknowledgement required: advance immediately
     -> acknowledgement required: WAITING
          -> NOT_RELATED: remain waiting
          -> WAIT: remain waiting and reset timeout/retry budget
          -> ADVANCE: clear waiting state, send next
          -> SEND_INDEX: send requested item
          -> COMPLETE: terminal success
          -> FAIL: terminal failure
          -> timeout: resend same event until retry exhaustion
  -> terminal SUCCESS / FAILED / TIMEOUT / CANCELLED / CLOSED
```

Evidence: `src/main/java/io/mapsmessaging/state/mavlink/sender/MavlinkEventListSender.java:111-238,240-368,386-456`.

### 6.3 Validation results

| Requirement | Assessment |
|---|---|
| First event sent exactly once | Correct under single `start()`; repeated starts are ignored |
| Ack-required events wait | Correct |
| No-ack events advance | Correct |
| `IN_PROGRESS` does not advance | Correct; maps to WAIT |
| Mission requested indices | Supported through SEND_INDEX |
| Invalid indices | Fail safely |
| Timeout retries correct event | Correct; waiting message identity and generation checked |
| Retry exhaustion | One terminal TIMEOUT |
| Cancellation | Sets terminal state, cancels timeout, blocks further sends |
| Completion callback once | Terminal guard enforces one callback |
| Late acknowledgements | Ignored after terminal state |
| Scheduler cancellation | Current acknowledgement timeout is cancelled |
| Event order | Preserved for normal sequential flow |
| Operation/model association | Preserved in immutable command set |
| Direct sender exception | Propagated into FAILED |
| Encoding/endpoint exception | **Not propagated by `MavlinkProtocol`** |
| Physical completion | Not determined by sender |

### 6.4 Command acknowledgement matching

The handler requires acknowledgements for `MavlinkCommandInt` and `MavlinkCommandLong`. It checks:

- packet type is `COMMAND_ACK`;
- packet is valid;
- command ID matches;
- optional target system/component matches configured local GCS IDs;
- result state.

Accepted advances, in-progress waits, and known rejected results fail.

Evidence: `src/main/java/io/mapsmessaging/state/mavlink/sender/MavlinkCommandAcknowledgementHandler.java:44-105`; `src/main/java/io/mapsmessaging/state/mavlink/packet/CommandAckPacket.java:37-126`.

Weaknesses:

- default constructor disables target matching;
- matching is not based on received frame source system/component;
- command acknowledgements do not carry a request sequence, so concurrent same-command operations require routing isolation outside this handler;
- duplicate accepted acknowledgements are harmless after normal ADVANCE because the waiting state changes, but cross-vehicle ambiguity remains possible if inbound packets are broadcast to multiple senders.

### 6.5 Mission upload handling

The mission handler supports:

- `MISSION_REQUEST_INT` 51;
- compatibility `MISSION_REQUEST`;
- `MISSION_ACK` 47;
- sequential requests;
- retransmission of the most recently requested item;
- stale-request ignore;
- forward-skip failure;
- out-of-range failure;
- target and mission-type filtering;
- accepted/rejected upload acknowledgement.

Evidence: `src/main/java/io/mapsmessaging/state/mavlink/sender/MavlinkMissionAcknowledgementHandler.java:34-266`.

Missing:

- integrated `MISSION_COUNT` prefix creation in model command sets;
- `MISSION_CURRENT` execution-state use;
- `MISSION_ITEM_REACHED` decoding and execution completion;
- mission replacement/preemption ownership;
- clear-before-upload policy;
- execution cancellation;
- distinction between upload completion and route completion;
- total upload deadline separate from per-event retry timeout.

### 6.6 Concurrency risks

`handleAdvance()` validates that the acknowledged message and index are still current before clearing waiting state. `handleSendIndex()` does not. The acknowledgement handler is invoked outside the sender lock, so two simultaneous mission requests can both be evaluated from the same old waiting state. A late `SEND_INDEX` action can then clear a newer waiting state and resend or reorder mission items.

Evidence: `src/main/java/io/mapsmessaging/state/mavlink/sender/MavlinkEventListSender.java:128-158,240-277`.

Recommended correction: pass the original waiting message/index into `handleSendIndex()` and apply the same identity check as `handleAdvance()` before changing state.

### 6.7 Completion semantics

- `COMMAND_ACK ACCEPTED` means command acceptance only.
- sender SUCCESS means the list was accepted/sent or an upload received `MISSION_ACK`.
- no model operation in this package determines arrival, stable loiter, take-off, landing, RTL completion, or route completion from telemetry.
- `MissionCurrentPacket` exists, but `MavlinkPacketFactory` has no `MISSION_ITEM_REACHED` mapping and the mission acknowledgement handler does not consume mission-current telemetry.

Evidence: `src/main/java/io/mapsmessaging/state/mavlink/packet/MissionCurrentPacket.java:31-55`; `src/main/java/io/mapsmessaging/state/mavlink/packet/MavlinkPacketFactory.java:28-52`.

---

## 7. Existing test coverage

### 7.1 Tests inspected

#### `MavlinkEventListSenderTest`

This test class proves:

- constructor validation;
- empty-list completion;
- no-ack ordering;
- waiting for first required acknowledgement;
- unrelated/null/WAIT acknowledgement behaviour;
- ADVANCE, COMPLETE, FAIL, and SEND_INDEX actions;
- invalid requested indices;
- initial send and requested-send exceptions;
- start idempotence;
- retries and retry exhaustion;
- acknowledgement arriving during `send()`;
- cancellation, close, one terminal callback, and terminal inbound ignore.

Evidence: `src/test/java/io/mapsmessaging/state/mavlink/sender/MavlinkEventListSenderTest.java:60-321,324-684`.

It does not provide a true multithreaded duplicate-ack/SEND_INDEX race test.

#### `MavlinkCommandAcknowledgementHandlerTest`

This proves:

- command message types require acknowledgement;
- non-command messages do not;
- invalid/wrong-command packets are unrelated;
- accepted/in-progress/rejected/unknown results map correctly.

It does not test configured local target IDs, wrong targets, or frame-source vehicle identity.

Evidence: `src/test/java/io/mapsmessaging/state/mavlink/sender/MavlinkCommandAcknowledgementHandlerTest.java:39-165`.

#### `MavlinkMissionAcknowledgementHandlerTest`

This proves:

- constructor and offset/count validation;
- request-int and legacy-request mapping;
- sequence order;
- latest duplicate retransmission;
- stale request handling;
- range checks;
- target and mission-type filtering;
- accepted/rejected/early mission ACK handling.

Evidence: `src/test/java/io/mapsmessaging/state/mavlink/sender/MavlinkMissionAcknowledgementHandlerTest.java:43-320`.

#### `MavlinkMissionCountTest`

This proves message ID/payload mapping and unsigned-short count validation.

Evidence: `src/test/java/io/mapsmessaging/state/mavlink/messages/MavlinkMissionCountTest.java:30-51`.

### 7.2 Missing or inadequate tests

No concrete model command-generation tests were located at the expected package paths. The important missing tests are:

- each model’s exact declared capability set;
- model discovery and exact-name lookup;
- exact REPOSITION message count and ordering;
- no ArduPlane mode command for PX4 models;
- exact `MAV_CMD` values and message types;
- frame selection from MSL versus AGL input;
- altitude conversion relative to home;
- Stickleback input immutability;
- Stickleback configured altitude semantics;
- yaw and speed rejection/mapping;
- fixed-wing loiter radius and direction;
- UGV unsupported-operation behaviour;
- mission item sequence and `autocontinue`;
- finite and infinite jump semantics;
- polygon patrol loop;
- route final loiter;
- mission-count-plus-items assembly;
- mission execution versus upload completion;
- `MISSION_CURRENT` and `MISSION_ITEM_REACHED`;
- concurrent duplicate mission requests;
- command ACK target/source isolation;
- transport encoding and endpoint error propagation;
- source identity configuration failure;
- late acknowledgement after a concurrently completed sender.

A test that merely constructs a model or confirms ServiceLoader discovery would not be meaningful command coverage.

### 7.3 Commands attempted

Environment inspection:

```text
java -version
openjdk version "21.0.10" 2026-01-20

mvn -version
bash: mvn: command not found

git --version
git version 2.47.3
```

Targeted test attempt:

```bash
mvn -Dtest='*Mavlink*Test,*Model*Test' test
```

Result:

```text
bash: mvn: command not found
exit code 127
```

Compile attempt:

```bash
mvn -DskipTests compile
```

Result:

```text
bash: mvn: command not found
exit code 127
```

Repository checkout attempt:

```bash
git clone --branch development --single-branch \
  https://github.com/Maps-Messaging/mapsmessaging_server.git \
  /mnt/data/mapsmessaging_server
```

Result:

```text
fatal: unable to access 'https://github.com/Maps-Messaging/mapsmessaging_server.git/':
Could not resolve host: github.com
exit code 128
```

### 7.4 Test execution result

| Item | Result |
|---|---|
| Tests executed | 0 |
| Tests passed | 0 |
| Tests failed | 0 |
| Tests skipped | 0 |
| Environmental failure | Maven unavailable; direct GitHub clone DNS failure |
| Source modifications made | none |
| Test modifications made | none |

---

## 8. Proposed production changes

| Priority | Model or component | Proposed change | Reason | Compatibility risk | Required tests |
|---|---|---|---|---|---|
| **P0** | `AbstractMissionUxvModel.reposition` | Replace shared three-event sequence with one platform-selected reposition command; retain legacy guided waypoint only behind an explicit ArduPilot compatibility policy | Current sequence sends competing mechanisms and ArduPlane mode to PX4/UGV | High: changes live command sequence | Per-model exact message-count/order tests; SITL regression |
| **P0** | Direct command factories | Select frame from altitude type or require a clearly named relative altitude value; never place MSL in frame 6 | Prevent unsafe altitude interpretation | High for existing callers relying on accidental semantics | MSL/AGL/relative parameter tests; home-altitude cases |
| **P0** | Stickleback model | Stop writing `altitudeMslMeters` for a relative frame; derive/receive relative altitude explicitly or ignore altitude for surface commands | Current forced value can become unintended height relative to home | High for deployed USV behaviour | configured altitude, home altitude, zero-surface tests |
| **P1** | `MavlinkMissionItemFactory.guidedWaypoint` | Use a coherent altitude field/frame; preferably add an INT fallback form; reject missing altitude cleanly | Current relative frame reads MSL and can NPE | Medium | MSL/AGL/null altitude tests |
| **P1** | `MavlinkProtocol` | Propagate encoding and endpoint failures to the publication/send completion path; remove `printStackTrace()` swallowing | Sender currently misreports real failures as timeouts | Medium: failure timing changes | formatter failure, endpoint failure, sender result tests |
| **P1** | Outbound identity | Use configured local identity explicitly at sender construction or populate source fields from context; fail before publication if unavailable | Zero source IDs are invalid until transport repair | Medium | configured/unconfigured identity tests |
| **P1** | Mission upload orchestration | Add a mission upload command-set builder containing `MISSION_COUNT` plus mission items and the mission handler metadata | Existing model output is not a complete upload protocol | Medium-high | full request/response sequence tests |
| **P1** | Mission execution tracker | Separate upload completion from execution completion using mission-current/item-reached and terminal policy | `MISSION_ACK` is not route completion | Medium | execution progress, terminal item, final loiter tests |
| **P1** | Patrol mission | Permit explicit infinite repeat and encode `DO_JUMP param2=-1` | Required polygon patrol behaviour is impossible | Medium | finite and infinite loop tests |
| **P1** | Route builder | Add explicit route/corridor plan with terminal loiter/hold policy | Required behaviour only manually expressible | Medium | final-loiter sequence tests |
| **P2** | `MavlinkEventListSender` | Apply waiting-message/index identity validation to SEND_INDEX actions | Avoid concurrent mission-request race | Low | multithreaded duplicate/out-of-order request tests |
| **P2** | Command ACK routing | Match received frame source vehicle and configured local target; document routing isolation | Same-command ACK ambiguity | Medium | two-vehicle same-command tests |
| **P2** | Stickleback capability set | Declare RETURN_TO_HOME or remove/override implementation | Capability inconsistency | Low | capability-set test |
| **P2** | Stickleback yaw handling | Reject unsupported yaw or map it; do not silently accept and ignore | Silent contract violation | Low | yaw supplied/absent tests |
| **P2** | Mission replacement/cancel | Define clear, replacement, upload cancellation, and execution cancellation state transitions | Current protocol ownership is incomplete | Medium-high | cancellation at each phase |
| **P2** | Model validation | Validate target/source IDs, sequence range, and required altitude before command construction | Context currently validates only vehicle UUID | Low-medium | invalid ID/sequence tests |
| **P3** | `ModelManager` | Add explicit aliases/normalization if required; make `getRequiredModel` throw or rename it | Current API semantics and alias requirement mismatch | Low | exact name, alias, duplicate tests |
| **P3** | Shared abstractions | Split PX4 and ArduPilot direct navigation policy from common mission construction | Current base class contains platform-specific behaviour | Medium refactor | parity tests before/after |
| **P3** | Constants and generated types | Replace duplicated numeric MAV_CMD/frame constants with one authoritative constants layer | Reduce drift and review burden | Low | compile/static mapping tests |
| **P3** | Configuration parsing | Avoid swallowing `Throwable`; validate Stickleback altitude/TTL ranges and log invalid values | Silent invalid configuration | Low | malformed/range tests |

---

## 9. Proposed test changes

Add deterministic tests before or alongside implementation.

### Model command-set tests

- `GenericPx4UavModelCommandSetTest`
  - exact capability set;
  - reposition produces exactly one PX4-appropriate command;
  - take-off/land command IDs and parameters;
  - loiter unlimited/timed;
  - orbit direction/radius;
  - unsupported depth/duration/speed cases.

- `GenericPx4FixedWingUavModelCommandSetTest`
  - default/custom loiter radius;
  - timed waypoint becomes command 19;
  - zero/negative radius rejection;
  - fixed-wing reposition radius/direction policy.

- `GenericPx4UgvModelCommandSetTest`
  - no ArduPlane mode command;
  - waypoint mission fields;
  - unsupported loiter/orbit/altitude/depth;
  - stop/pause semantics.

- `SticklebackArdupilotUsvModelCommandSetTest`
  - no caller-position mutation;
  - explicit surface altitude semantics;
  - reposition/loiter exact frame and z;
  - yaw reject or mapping;
  - RETURN_TO_HOME declaration consistency;
  - current-position stop command.

### Factory tests

- `MavlinkCommandIntFactoryAltitudeFrameTest`
- `MavlinkGuidedWaypointFactoryTest`
- `MavlinkMissionItemIntFactoryPatrolTest`
- `MavlinkMissionUploadCommandSetFactoryTest`

Assertions should include:

- message ID;
- command;
- target IDs;
- source identity policy;
- frame;
- x/y scaling;
- z semantics;
- all command parameters;
- `current`;
- `autocontinue`;
- mission type;
- event order.

### Sender and acknowledgement tests

Extend `MavlinkEventListSenderTest` with:

- two threads delivering the same SEND_INDEX request;
- SEND_INDEX racing with cancellation;
- SEND_INDEX racing with ADVANCE/new waiting state;
- late timeout callback after requested-index transition;
- one completion callback under concurrent terminal actions;
- total deadline for repeated IN_PROGRESS if introduced.

Extend `MavlinkCommandAcknowledgementHandlerTest` with:

- configured local target match/mismatch;
- absent target extensions;
- source system/component isolation at the routing layer;
- two vehicles with the same outstanding command.

Extend `MavlinkMissionAcknowledgementHandlerTest` with:

- integrated `MISSION_COUNT` plus item sequence;
- duplicate first/final request through `MavlinkEventListSender`;
- request after terminal mission ACK;
- cancellation during upload;
- replacement during upload;
- unsupported mission type;
- zero-item policy;
- invalid indices under concurrent requests.

### Mission execution tests

Suggested classes:

- `MavlinkMissionExecutionTrackerTest`
- `PolygonPatrolMissionBuilderTest`
- `RouteWithFinalLoiterMissionBuilderTest`
- `MissionReplacementAndCancellationTest`

Test:

- infinite polygon jump to first patrol waypoint;
- finite repeat count;
- route once then final unlimited loiter;
- route complete with explicit terminal action;
- `MISSION_CURRENT` progression;
- `MISSION_ITEM_REACHED`;
- upload complete versus execution complete;
- final loiter remaining active;
- cancellation/preemption in upload and execution phases.

### Transport tests

- `MavlinkProtocolOutboundFailureTest`
- `MavlinkOutboundIdentityTest`

Test formatter exceptions, invalid source identity, endpoint send failures, correlation routing errors, and exact propagation to the sender completion result.

---

## 10. Recommended implementation sequence

### Stage 1: command-generation safety and regression tests

1. Add per-model tests for existing REPOSITION output.
2. Replace the shared three-event reposition sequence with platform-specific policies.
3. Correct altitude frame/source semantics.
4. Remove Stickleback caller-object mutation.
5. Correct or explicitly gate the legacy guided fallback.
6. Make transport encoding/send failures observable by the sender.
7. Run PX4 and ArduPilot SITL tests for reposition and stop.

This stage should be completed before adding new mission behaviours because current direct movement can select the wrong mode or altitude.

### Stage 2: capability and direct-command consistency

1. Fix Stickleback RTL declaration.
2. Resolve yaw acceptance/mapping.
3. Validate speed and heading support per platform.
4. Define outbound source identity ownership.
5. Add aliases only if configuration compatibility requires them.

### Stage 3: production mission upload

1. Build `MISSION_COUNT + items` as one upload transaction.
2. Bind `MavlinkMissionAcknowledgementHandler` explicitly to the upload.
3. Harden SEND_INDEX concurrency.
4. Add clear/replacement/cancellation policy.
5. Distinguish upload success from execution start.

### Stage 4: patrol and route behaviours

1. Add infinite `DO_JUMP -1`.
2. Add polygon patrol builder.
3. Add line/corridor route builder.
4. Add explicit final loiter/hold.
5. Add finite-route terminal policy.

### Stage 5: mission execution and advanced navigation

1. Track `MISSION_CURRENT`.
2. Add `MISSION_ITEM_REACHED`.
3. Implement pause/resume execution state.
4. Add loiter turns, loiter-to-altitude, and continue/change-altitude.
5. Add fixed-wing mission take-off and landing where required.
6. Add physical-completion monitors for direct commands.

### Stage 6: refactoring and documentation

1. Split autopilot-specific direct navigation from shared mission validation.
2. Centralize MAVLink constants.
3. Document frames, altitude conventions, completion semantics, and firmware assumptions.

---

## 11. Open questions requiring validation

1. Which exact PX4 and ArduPilot firmware versions are deployed for each model?
2. Does each firmware accept `COMMAND_INT NAV_LOITER_UNLIM` and `NAV_LOITER_TIME` outside AUTO mission execution?
3. For Stickleback, should altitude be ignored, fixed relative to home, fixed MSL, or derived from telemetry?
4. Does command 17 with zero latitude/longitude mean “loiter here” on the deployed ArduPilot Plane firmware?
5. Is the Stickleback autopilot intentionally addressed through a non-standard component ID?
6. Should PX4 take-off and land remain immediate commands, or must fixed-wing models use mission items?
7. Is route completion defined as reaching the final waypoint, entering final loiter, remaining stable for a period, or receiving another task?
8. Should an infinite patrol remain active until explicit cancellation, and what command should cancellation send for each model?
9. Should mission uploads always clear the previous mission first, or atomically replace it?
10. Is `MAV_CMD_CONDITION_YAW` supported and meaningful for the PX4 UGV and Stickleback fixed-wing USV?
11. Does the deployed PX4 orbit implementation honour signed radius for direction and frame-0 altitude as constructed?
12. Should `COMMAND_ACK` routing rely on per-vehicle topics, MAVLink frame source IDs, or both?

---

## Concise review result

- **Models reviewed:** 4.
- **Confirmed high-severity defects:** shared multi-command reposition, ArduPlane mode applied to PX4/UGV, altitude-frame mismatch, Stickleback forced-altitude mismatch, legacy guided altitude error, swallowed transport failure.
- **Missing core behaviours:** infinite patrol, dedicated route/final loiter, integrated mission upload, mission execution completion, advanced loiter/altitude commands.
- **Sending path:** sender state machine is mostly sound, but mission SEND_INDEX concurrency and acknowledgement isolation need work; transport failures are not propagated.
- **Recommended first implementation stage:** regression tests and fixes for reposition command selection, altitude semantics, Stickleback mutation, legacy fallback, and transport error propagation.
- **Report path:** `mavlink-model-review.md`.
