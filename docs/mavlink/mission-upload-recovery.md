# Mission upload sequence recovery

Refs: MSG-227

An addressed, valid MISSION_ACK with MAV_MISSION_INVALID_SEQUENCE ends the
current transfer and requests a restart from its MISSION_COUNT message.
Sequence tracking resets to zero. The sender retains the same command set,
registration and completion callback; it does not clear the onboard mission
or start execution until the final accepted mission ACK.

Two restarts are permitted per sender. Backoff is one then two acknowledgement
timeout intervals (2 s then 4 s by default). Inbound packets are ignored while
backoff is pending. Cancellation and close cancel the restart timer; generation
checks prevent a stale timer from starting a later attempt.

After the restart budget is exhausted the sender reports TIMEOUT, retaining the
last response and an explicit exhaustion reason. The existing STANAG dispatcher
keeps initial dispatch available for explicit resend and releases a failed resume
attempt. Other mission rejection results still report FAILED. Normal packet-loss
retransmission settings are unchanged. A handler without a mission prefix cannot
restart and retains its previous failure behaviour.

This is bounded transfer retry, not automatic connectivity monitoring. It does
not wait for Starlink/cellular availability. Task cancellation must continue to
close/cancel its registered sender. No new task expiration policy is introduced.
The existing ANY_LOCAL_ID preparer behaviour and repeated-request no-progress
budget remain separate follow-ups. MAVLink has no upload-attempt transaction ID;
packets delayed beyond the backoff can still be indistinguishable from a current
request. Backoff reduces exposure but does not provide strong correlation.

Regression coverage includes restart success after partial upload, ignored
packets during backoff, restart-budget exhaustion, cancellation during backoff,
and scheduled restart execution. Unsupported rejection still fails without
starting navigation.
