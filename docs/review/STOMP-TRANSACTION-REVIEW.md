# STOMP transaction review

## Scope

This review covers the existing STOMP transaction implementation and tests. No transaction semantics are changed by the STOMP compliance hardening branch.

## Existing implementation

Publication transactions use the server transaction API:

- `BEGIN` creates a named `Transaction` through `Session.startTransaction`.
- `SEND` with a `transaction` header stores the message transactionally.
- `COMMIT` commits the transaction and removes it from the session.
- `ABORT` aborts the transaction and removes it from the session.
- Closing the session closes every outstanding transaction, which aborts transactions that have not completed.

The underlying transaction lifecycle is coherent and already rejects duplicate transaction names and attempts to commit or abort unknown transactions.

## Confirmed gaps

### Unknown transaction on SEND does not return the constructed ERROR

`EventListener.handleMessageStoreToDestination` constructs an `ERROR` frame when a `SEND` references an unknown transaction, but does not call `engine.send(error)`. The message is not published, yet the client may receive neither an error nor its requested receipt.

This should be fixed in a transaction-specific change with a regression test asserting `ERROR` followed by connection closure.

### Transactional ACK and NACK are not implemented

`ACK` and `NACK` frames parse the optional `transaction` header, but `AckListener` and `NackListener` apply the acknowledgement immediately. STOMP permits acknowledgements to participate in transactions.

Implementing this requires a deliberate mapping between STOMP acknowledgement operations and the subscription transaction facilities. It should not be approximated by delaying an arbitrary callback in the protocol layer.

### Existing transaction tests do not prove broker-visible semantics

`StompTransactionalPublishTest` sends committed and aborted transactions but does not use a subscriber to assert that:

- committed messages become visible;
- aborted messages remain invisible;
- messages become visible atomically after commit;
- disconnect aborts outstanding transactions.

`StompPublishEventTest.testTransactionalPublishContentLength` has subscriber coverage for one commit path, but its abort verification reads from the publishing connection rather than proving the subscriber received nothing.

Several subscription transaction tests are disabled because their STOMP client is obsolete.

## Recommended transaction tests

A future transaction-specific branch should add raw-wire or replacement-client tests for:

1. committed messages are invisible before commit and visible after commit;
2. aborted messages are never visible;
3. disconnect aborts an open transaction;
4. duplicate `BEGIN` returns `ERROR`;
5. `COMMIT`, `ABORT`, and `SEND` with an unknown transaction return `ERROR`;
6. a receipt on `COMMIT` is sent only after commit completes;
7. ACK in a committed transaction acknowledges the delivery;
8. ACK in an aborted transaction leaves the delivery outstanding;
9. NACK in a committed transaction rolls the delivery back;
10. transaction isolation is maintained across two concurrent sessions.

## Recommendation

Leave publication transaction internals unchanged in the current compliance branch. Fix the missing unknown-transaction `ERROR` and transactional acknowledgement behaviour in a focused transaction branch once a maintained Java STOMP client has been selected, while retaining raw-wire tests for specification details the client may hide.
