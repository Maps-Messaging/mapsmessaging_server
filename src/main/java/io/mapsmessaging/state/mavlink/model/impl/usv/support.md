# Stickleback ArduPilot USV MAVLink Capability Table

This table reflects the current Stickleback ArduPilot USV model after refocusing it as a surface craft using ArduPilot Plane-style navigation with a minimum speed of `0`.

Assumptions:

- Vehicle is a USV surface craft, not a UUV.
- Navigation behaviour is modelled on ArduPilot fixed-wing / Plane semantics.
- Minimum speed can be `0`, so pause/hold/stop commands are meaningful for this vehicle.
- Loiter means a surface circular loiter around a latitude/longitude point.
- Ellipse are not included because they are not code-backed capabilities.
- Depth is invalid for this model.
- Altitude is not exposed as a user-facing control for this model.

## Supported

| UXV function | Supported | MAVLink message | Message ID | MAVLink command | Command ID | Params supplied / model behaviour |
|---|---:|---|---:|---|---:|---|
| `arm(context)` | Yes | `COMMAND_LONG` | `76` | `MAV_CMD_COMPONENT_ARM_DISARM` | `400` | `param1 = 1` arm. Factory handles target system/component and sequence. |
| `disarm(context)` | Yes | `COMMAND_LONG` | `76` | `MAV_CMD_COMPONENT_ARM_DISARM` | `400` | `param1 = 0` disarm. |
| `setHome(context, request)` current position | Yes | `COMMAND_LONG` | `76` | `MAV_CMD_DO_SET_HOME` | `179` | `param1 = 1`; uses current vehicle position. |
| `setHome(context, request)` specified position | Yes | `COMMAND_LONG` | `76` | `MAV_CMD_DO_SET_HOME` | `179` | `param1 = 0`, `param5 = latitude`, `param6 = longitude`, `param7 = altitude/default`. |
| `reposition(context, request)` | Yes, verify on Stickleback firmware | `COMMAND_INT` | `75` | likely `MAV_CMD_DO_REPOSITION` | `192` | Position from `request.position()`, yaw normalised or `NaN`; speed rejected. Needs practical confirmation against the Plane-derived firmware. |
| `holdPosition(context)` | Yes | `COMMAND_LONG` | `76` | `MAV_CMD_DO_PAUSE_CONTINUE` | `193` | Pause command, effectively hold/stop because Stickleback minimum speed is `0`. |
| `stop(context)` | Yes | `COMMAND_LONG` | `76` | `MAV_CMD_DO_PAUSE_CONTINUE` | `193` | Same pause command as hold. Valid here because the craft can actually stop. |
| `pauseVehicle(context)` | Yes | `COMMAND_LONG` | `76` | `MAV_CMD_DO_PAUSE_CONTINUE` | `193` | `param1 = 0` pause, via factory. |
| `resumeVehicle(context)` | Yes | `COMMAND_LONG` | `76` | `MAV_CMD_DO_PAUSE_CONTINUE` | `193` | `param1 = 1` resume/continue, via factory. |
| `startMission(context)` | Yes | `COMMAND_LONG` | `76` | `MAV_CMD_MISSION_START` | `300` | Starts mission; factory supplies defaults for first/last item unless it exposes more. |
| `setSpeed(context, speedMetersPerSecond)` | Yes | `COMMAND_LONG` | `76` | `MAV_CMD_DO_CHANGE_SPEED` | `178` | `param1 = 1` ground speed, `param2 = speed m/s`, `param3 = -1` unchanged throttle. Zero speed allowed. |
| `setHeading(context, headingDegrees)` | Yes, verify behaviour | `COMMAND_LONG` | `76` | `MAV_CMD_CONDITION_YAW` | `115` | `param1 = normalised heading`, `param2 = 0`, `param3 = 1` clockwise, `param4 = 0` absolute yaw. Test on Plane-style USV firmware. |
| `loiter(context, request)` zero duration | Yes | `COMMAND_INT` | `75` | `MAV_CMD_NAV_LOITER_UNLIM` | `17` | Surface circular loiter. `radiusMeters` supplied, `duration = 0` means unlimited, `position = lat/lon`, yaw/altitude/depth rejected in the cleaned model. |
| `loiter(context, request)` positive duration | Yes | `COMMAND_INT` | `75` | `MAV_CMD_NAV_LOITER_TIME` | `19` | Surface circular timed loiter. `param1 = duration seconds`, `param3 = radius metres`, `x/y = lat/lon * 1e7`, `z = default/model value`. |
| `buildMission(...)` waypoint item | Yes | `MISSION_ITEM_INT` | `73` | `MAV_CMD_NAV_WAYPOINT` | `16` | `param1 = hold seconds`, `param2 = acceptance radius`, `param3 = pass radius`, `param4 = yaw or NaN`, `x/y = lat/lon * 1e7`, `z = default/model value`. |
| `buildMission(...)` loiter item zero hold | Yes | `MISSION_ITEM_INT` | `73` | `MAV_CMD_NAV_LOITER_UNLIM` | `17` | Position required, radius defaults to `2 m` if absent, zero/null hold means unlimited loiter. |
| `buildMission(...)` loiter item positive hold | Yes | `MISSION_ITEM_INT` | `73` | `MAV_CMD_NAV_LOITER_TIME` | `19` | `param1 = hold seconds`, radius supplied/defaulted, position required. |
| `buildMission(...)` return-to-home item | Yes | `MISSION_ITEM_INT` | `73` | `MAV_CMD_NAV_RETURN_TO_LAUNCH` | `20` | Return-to-launch mission item. |
| `interpretDetection(...)` | Yes | `NAMED_VALUE_FLOAT` input | `251` | Not a command | n/a | Interprets named float state values: `0 = lost`, `1 = detected`, creates `DetectionEvent`. Telemetry interpretation, not command output. |

## Not supported

| Function / field / mission item | Supported | Reason |
|---|---:|---|
| `ORBIT` operation / `PlanItemType.ORBIT` | No | Mission item is explicitly rejected. Do not imply `ORBIT` and fixed-wing loiter are interchangeable. |
| `PlanItemType.HOLD_POSITION` inside mission | No | Explicitly rejected for mission build. Runtime hold/stop uses pause command instead. |
| `ELLIPSE` operation or mission geometry | No | Not a code-backed capability and not exposed by this model. |
| `depthMeters` | No | Surface vehicle; depth is invalid. |
| `altitudeMeters` | No | Not mapped for this surface model. MAVLink may still require a default `z`, but altitude is not user-facing here. |
| `speedMetersPerSecond` inside mission items | No | Current model rejects mission item speed. Use `setSpeed(...)` as a command instead. |
| `speedMetersPerSecond` in `reposition(...)` | No | Current model rejects it. Speed change is handled separately via `setSpeed(...)`. |
| `yawDegrees` in `loiter(...)` | No in cleaned model | Rejected to avoid implying heading control during circular loiter. |
| Negative `duration` / `holdDuration` | No | Rejected. `null` and zero become unlimited/zero hold; positive means timed. |
| Negative or non-finite `radiusMeters` | No | Rejected. Radius must be finite and `>= 0`. |
| `takeOff(...)` | No | Surface craft. No takeoff command. |
| `land(...)` | No | Surface craft. No landing command. |
| direct `returnToHome(context)` | Not currently advertised | Mission RTL item is supported, but direct `RETURN_TO_HOME` is not in the supported operation list. |

## Validation notes

The two command paths that should be confirmed against Stickleback firmware or SITL are:

1. `reposition(context, request)`, because Plane-derived ArduPilot guided behaviour may prefer `SET_POSITION_TARGET_GLOBAL_INT` over `MAV_CMD_DO_REPOSITION`.
2. `setHeading(context, headingDegrees)`, because `MAV_CMD_CONDITION_YAW` support can vary by vehicle behaviour and mode.

Everything else in this table is intentionally limited to the code-backed model surface.


## Supported command mapping

| UXV function | MAVLink message | Message ID | MAVLink command | Command ID | Message fields | Param mapping |
|---|---|---:|---|---:|---|---|
| `arm(context)` | `COMMAND_LONG` | `76` | `MAV_CMD_COMPONENT_ARM_DISARM` | `400` | `target_system = context.targetSystem()`<br>`target_component = context.targetComponent()` | `param1 = 1` arm<br>`param2 = 0` normal safety checks<br>`param3-7 = default` |
| `disarm(context)` | `COMMAND_LONG` | `76` | `MAV_CMD_COMPONENT_ARM_DISARM` | `400` | `target_system = context.targetSystem()`<br>`target_component = context.targetComponent()` | `param1 = 0` disarm<br>`param2 = 0` normal safety checks<br>`param3-7 = default` |
| `setHome(context, request)` current position | `COMMAND_LONG` | `76` | `MAV_CMD_DO_SET_HOME` | `179` | `target_system = context.targetSystem()`<br>`target_component = context.targetComponent()` | `param1 = 1` use current position<br>`param2-7 = default` |
| `setHome(context, request)` specified position | `COMMAND_LONG` | `76` | `MAV_CMD_DO_SET_HOME` | `179` | `target_system = context.targetSystem()`<br>`target_component = context.targetComponent()` | `param1 = 0` use specified position<br>`param5 = latitude` degrees<br>`param6 = longitude` degrees<br>`param7 = altitude/default metres`<br>`param2-4 = default` |
| `reposition(context, request)` | `COMMAND_INT` | `75` | `MAV_CMD_DO_REPOSITION` | `192` | `target_system = context.targetSystem()`<br>`target_component = context.targetComponent()`<br>`frame = factory-defined global frame`<br>`current = 0`<br>`autocontinue = 0` | `param1 = factory/default speed`<br>`param2 = factory/default flags`<br>`param3 = factory/default radius`<br>`param4 = yawDegrees` or `NaN`<br>`x = latitude * 1e7`<br>`y = longitude * 1e7`<br>`z = altitude/default` |
| `holdPosition(context)` | `COMMAND_LONG` | `76` | `MAV_CMD_DO_PAUSE_CONTINUE` | `193` | `target_system = context.targetSystem()`<br>`target_component = context.targetComponent()` | `param1 = 0` pause/hold<br>`param2-7 = default` |
| `stop(context)` | `COMMAND_LONG` | `76` | `MAV_CMD_DO_PAUSE_CONTINUE` | `193` | Same as `holdPosition(context)` | `param1 = 0` pause/hold<br>`param2-7 = default` |
| `pauseVehicle(context)` | `COMMAND_LONG` | `76` | `MAV_CMD_DO_PAUSE_CONTINUE` | `193` | Same as `holdPosition(context)` | `param1 = 0` pause/hold<br>`param2-7 = default` |
| `resumeVehicle(context)` | `COMMAND_LONG` | `76` | `MAV_CMD_DO_PAUSE_CONTINUE` | `193` | `target_system = context.targetSystem()`<br>`target_component = context.targetComponent()` | `param1 = 1` continue/resume<br>`param2-7 = default` |
| `startMission(context)` | `COMMAND_LONG` | `76` | `MAV_CMD_MISSION_START` | `300` | `target_system = context.targetSystem()`<br>`target_component = context.targetComponent()` | `param1 = first mission item/default`<br>`param2 = last mission item/default`<br>`param3-7 = default` |
| `setSpeed(context, speedMetersPerSecond)` | `COMMAND_LONG` | `76` | `MAV_CMD_DO_CHANGE_SPEED` | `178` | `target_system = context.targetSystem()`<br>`target_component = context.targetComponent()` | `param1 = 1` ground speed<br>`param2 = speedMetersPerSecond` m/s<br>`param3 = -1` unchanged throttle<br>`param4-7 = default` |
| `setHeading(context, headingDegrees)` | `COMMAND_LONG` | `76` | `MAV_CMD_CONDITION_YAW` | `115` | `target_system = context.targetSystem()`<br>`target_component = context.targetComponent()` | `param1 = normalised headingDegrees` `[0, 360)`<br>`param2 = 0` angular speed/default<br>`param3 = 1` clockwise<br>`param4 = 0` absolute heading<br>`param5-7 = default` |
| `loiter(context, request)` with zero duration | `COMMAND_INT` | `75` | `MAV_CMD_NAV_LOITER_UNLIM` | `17` | `target_system = context.targetSystem()`<br>`target_component = context.targetComponent()`<br>`frame = factory-defined global frame`<br>`current = 0`<br>`autocontinue = 0` | `param1 = default`<br>`param2 = default`<br>`param3 = radiusMeters`<br>`param4 = NaN/default`<br>`x = latitude * 1e7`<br>`y = longitude * 1e7`<br>`z = altitude/default` |
| `loiter(context, request)` with positive duration | `COMMAND_INT` | `75` | `MAV_CMD_NAV_LOITER_TIME` | `19` | Same as unlimited loiter | `param1 = duration seconds`<br>`param2 = default`<br>`param3 = radiusMeters`<br>`param4 = NaN/default`<br>`x = latitude * 1e7`<br>`y = longitude * 1e7`<br>`z = altitude/default` |
| `buildMission(...)` waypoint item | `MISSION_ITEM_INT` | `73` | `MAV_CMD_NAV_WAYPOINT` | `16` | `target_system = context.targetSystem()`<br>`target_component = context.targetComponent()`<br>`seq = mission item index`<br>`frame = factory-defined mission/global frame`<br>`current = factory/default`<br>`autocontinue = factory/default` | `param1 = holdDuration seconds`, default `0`<br>`param2 = acceptance radius`, default `2 m`<br>`param3 = pass radius`, default `0 m`<br>`param4 = yawDegrees` or `NaN`<br>`x = latitude * 1e7`<br>`y = longitude * 1e7`<br>`z = altitude/default` |
| `buildMission(...)` loiter item with zero hold duration | `MISSION_ITEM_INT` | `73` | `MAV_CMD_NAV_LOITER_UNLIM` | `17` | `target_system = context.targetSystem()`<br>`target_component = context.targetComponent()`<br>`seq = mission item index`<br>`frame = factory-defined mission/global frame` | `param1 = default`<br>`param2 = default`<br>`param3 = radiusMeters`, default `2 m`<br>`param4 = NaN/default`<br>`x = latitude * 1e7`<br>`y = longitude * 1e7`<br>`z = altitude/default` |
| `buildMission(...)` loiter item with positive hold duration | `MISSION_ITEM_INT` | `73` | `MAV_CMD_NAV_LOITER_TIME` | `19` | Same as mission unlimited loiter | `param1 = holdDuration seconds`<br>`param2 = default`<br>`param3 = radiusMeters`, default `2 m`<br>`param4 = NaN/default`<br>`x = latitude * 1e7`<br>`y = longitude * 1e7`<br>`z = altitude/default` |
| `buildMission(...)` return-to-home item | `MISSION_ITEM_INT` | `73` | `MAV_CMD_NAV_RETURN_TO_LAUNCH` | `20` | `target_system = context.targetSystem()`<br>`target_component = context.targetComponent()`<br>`seq = mission item index`<br>`frame = factory-defined mission/global frame` | `param1-7 = default` |

## Telemetry interpretation, not command output

| UXV function | MAVLink message | Message ID | Command ID | Fields used | Model behaviour |
|---|---|---:|---:|---|---|
| `interpretDetection(droneTwin, event)` | `NAMED_VALUE_FLOAT` | `251` | n/a | `name`<br>`value` | If `value` rounds to `1`, emits `DETECTED`; if `value` rounds to `0`, emits `LOST`; contact id is derived from the MAVLink named value name. Telemetry interpretation, not command output. |

## MAVLink notes that matter

`COMMAND_INT` message id `75` carries params 1-4 as floats, then `x` and `y` as scaled integer position fields, normally latitude and longitude multiplied by `1e7`, and `z` as altitude.

`COMMAND_INT` is preferred over `COMMAND_LONG` when sending latitude/longitude because it provides better precision and an explicit frame.

`MISSION_ITEM_INT` message id `73` is the mission item form with `seq`, `frame`, `command`, `current`, `autocontinue`, `param1-4`, `x`, `y`, and `z`. Its `x` and `y` fields are also scaled integer coordinates for global frames.

For loiter, `MAV_CMD_NAV_LOITER_UNLIM` command `17` uses `param3` as loiter radius and position in params 5-7 / `x`, `y`, `z` depending on message type.

`MAV_CMD_NAV_LOITER_TIME` command `19` uses `param1` as loiter time, `param2` as heading-required, `param3` as radius, `param4` as xtrack behaviour, and position in params 5-7 / `x`, `y`, `z` depending on message type.

For fixed-wing / forward-only style vehicles, loiter means circling the point with the specified radius/direction. For Stickleback, this is interpreted as a surface circular loiter.

For pause/hold/resume, `MAV_CMD_DO_PAUSE_CONTINUE` command `193` uses `param1 = 0` to pause/hold and `param1 = 1` to continue. For this USV, pause/hold is meaningful because minimum speed can be `0`.

`reposition(context, request)` and `setHeading(context, headingDegrees)` should be confirmed against Stickleback firmware or SITL because Plane-derived ArduPilot guided command behaviour can vary by mode and firmware configuration.