/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
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

package io.mapsmessaging.state.auditor;


import io.mapsmessaging.audit.AppendOnlyAuditJournal;
import io.mapsmessaging.audit.AuditJournal;
import io.mapsmessaging.audit.AuditJournalConfig;
import io.mapsmessaging.audit.AuditKeyUtils;
import io.mapsmessaging.audit.AuditLogger;
import io.mapsmessaging.audit.AuditPayloadStore;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.interfaces.EdECPublicKey;

import lombok.Getter;

public class AuditorFactory {

  private static final String SECURITY_DIRECTORY_NAME = ".security";
  private static final String PRIVATE_KEY_FILE_NAME = "audit-signing-private-key.pem";
  private static final String PUBLIC_KEY_FILE_NAME = "audit-verification-public-key.pem";

  private static final String DEFAULT_AUDIT_DIRECTORY_NAME = "audit";
  private static final String JOURNAL_DIRECTORY_NAME = "journal";
  private static final String PAYLOAD_DIRECTORY_NAME = "payloads";

  private static final long DEFAULT_MAX_JOURNAL_SIZE_BYTES = 64L * 1024L * 1024L;

  private final AuditKeyUtils auditKeyUtils;

  public AuditorFactory() {
    this.auditKeyUtils = new AuditKeyUtils();
  }

  public AuditorInstance build(Path mapsDataDirectory) throws IOException {
    return build(
        mapsDataDirectory,
        mapsDataDirectory.resolve(DEFAULT_AUDIT_DIRECTORY_NAME),
        DEFAULT_MAX_JOURNAL_SIZE_BYTES
    );
  }

  public AuditorInstance build(
      Path mapsDataDirectory,
      Path auditRootDirectory
  ) throws IOException {
    return build(
        mapsDataDirectory,
        auditRootDirectory,
        DEFAULT_MAX_JOURNAL_SIZE_BYTES
    );
  }

  public AuditorInstance build(
      Path mapsDataDirectory,
      Path auditRootDirectory,
      long maxJournalSizeBytes
  ) throws IOException {
    Files.createDirectories(mapsDataDirectory);

    Path securityDirectory = mapsDataDirectory.resolve(SECURITY_DIRECTORY_NAME);
    Files.createDirectories(securityDirectory);

    Path privateKeyPath = securityDirectory.resolve(PRIVATE_KEY_FILE_NAME);
    Path publicKeyPath = mapsDataDirectory.resolve(PUBLIC_KEY_FILE_NAME);

    AuditKeys auditKeys = loadOrCreateAuditKeys(
        privateKeyPath,
        publicKeyPath
    );

    Path journalRootDirectory = auditRootDirectory.resolve(JOURNAL_DIRECTORY_NAME);
    Path payloadRootDirectory = auditRootDirectory.resolve(PAYLOAD_DIRECTORY_NAME);

    Files.createDirectories(journalRootDirectory);
    Files.createDirectories(payloadRootDirectory);

    AuditJournalConfig auditJournalConfig = AuditJournalConfig.builder()
        .journalRoot(journalRootDirectory)
        .signingKey(auditKeys.privateKey())
        .verificationKey(auditKeys.publicKey())
        .maxJournalSizeBytes(maxJournalSizeBytes)
        .rotateDaily(true)
        .failOnInvalidExistingJournal(true)
        .build();

    AppendOnlyAuditJournal auditJournal = new AppendOnlyAuditJournal(auditJournalConfig);
    AuditLogger auditLogger = new AuditLogger(auditJournal);
    AuditPayloadStore auditPayloadStore = new AuditPayloadStore(payloadRootDirectory);

    StateAuditContext auditContext = new StateAuditContext(
        auditLogger,
        auditPayloadStore
    );

    return new AuditorInstance(
        auditContext,
        auditJournal,
        auditRootDirectory,
        journalRootDirectory,
        payloadRootDirectory,
        privateKeyPath,
        publicKeyPath
    );
  }

  private AuditKeys loadOrCreateAuditKeys(
      Path privateKeyPath,
      Path publicKeyPath
  ) throws IOException {
    boolean privateKeyExists = Files.exists(privateKeyPath);
    boolean publicKeyExists = Files.exists(publicKeyPath);

    if (privateKeyExists && publicKeyExists) {
      PrivateKey privateKey = auditKeyUtils.readPrivateKey(privateKeyPath);
      EdECPublicKey publicKey = auditKeyUtils.readPublicKey(publicKeyPath);

      return new AuditKeys(
          privateKey,
          publicKey
      );
    }

    if (privateKeyExists != publicKeyExists) {
      throw new IOException(
          "Audit key pair is incomplete. Private key exists: "
              + privateKeyExists
              + ", public key exists: "
              + publicKeyExists
      );
    }

    KeyPair keyPair = auditKeyUtils.generateEd25519KeyPair();

    auditKeyUtils.writePrivateKey(
        privateKeyPath,
        keyPair.getPrivate()
    );

    auditKeyUtils.writePublicKey(
        publicKeyPath,
        keyPair.getPublic()
    );

    return new AuditKeys(
        keyPair.getPrivate(),
        (EdECPublicKey) keyPair.getPublic()
    );
  }

  private record AuditKeys(
      PrivateKey privateKey,
      EdECPublicKey publicKey
  ) {
  }

  @Getter
  public static class AuditorInstance implements AutoCloseable {

    private final StateAuditContext auditContext;
    private final AuditJournal auditJournal;
    private final Path auditRootDirectory;
    private final Path journalRootDirectory;
    private final Path payloadRootDirectory;
    private final Path privateKeyPath;
    private final Path publicKeyPath;

    private AuditorInstance(
        StateAuditContext auditContext,
        AuditJournal auditJournal,
        Path auditRootDirectory,
        Path journalRootDirectory,
        Path payloadRootDirectory,
        Path privateKeyPath,
        Path publicKeyPath
    ) {
      this.auditContext = auditContext;
      this.auditJournal = auditJournal;
      this.auditRootDirectory = auditRootDirectory;
      this.journalRootDirectory = journalRootDirectory;
      this.payloadRootDirectory = payloadRootDirectory;
      this.privateKeyPath = privateKeyPath;
      this.publicKeyPath = publicKeyPath;
    }

    @Override
    public void close() throws IOException {
      auditJournal.close();
    }
  }
}
