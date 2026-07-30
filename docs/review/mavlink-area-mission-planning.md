# MAVLink area mission planning

This branch adds the MAVLink-side support required by the STANAG state adapter to execute ordered and continuously repeating area missions.

## Changes

- explicit finite and indefinite mission repetition through `MissionPlan`
- `MAV_CMD_DO_JUMP` repeat-forever support using `param2=-1`
- mission-plan navigation overload on `UxvModel`
- canonical server-owned `MISSION_COUNT` framing and mission acknowledgement selection
- validation of mission item type, target and sequence before upload
- serialized inbound acknowledgement processing in `MavlinkEventListSender`
- explicit passive detection capability on UxV models
- Stickleback passive detection declaration while retaining fixed home-relative altitude policy

## Validation

Focused JUnit tests cover finite and indefinite repetition, mission framing, inbound acknowledgement serialization and Stickleback passive detection capability.

A temporary branch-only GitHub Actions workflow is included solely to execute these tests. It will be removed after the validation result is captured.
