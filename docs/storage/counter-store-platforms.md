# Counter-store filesystem durability

`IndexedCounterBulkLoader` publishes a new generation with an atomic directory rename. `GenerationCounterStore` publishes its active-generation manifest with an atomic file replacement. Counter files and the manifest are forced to storage before publication.

On Linux and other non-Windows platforms, publication also opens and forces the parent directory. Directory open or force failures propagate to the caller; they are not ignored.

Windows Java NIO cannot open directories as `FileChannel` objects. On Windows, both publication paths skip that directory-only barrier, matching `PacketIdJournal`. Regular file writes, file forcing, ownership locks and atomic publication remain in use, and their failures still propagate.

The Windows path supports normal close/reopen and process restart. Its directory-entry persistence across sudden power loss depends on the filesystem and operating system; it does not provide the same explicit directory durability barrier as Linux. This limitation matters for replay-counter protection after power loss.
