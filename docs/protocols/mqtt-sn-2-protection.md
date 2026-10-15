# MQTT-SN 2.0 protection (CSD01)

MQTT-SN 2.0 protection uses Java ServiceLoader implementations of
`MqttSn2CryptoProvider`, the Authentication library's
`SecretKeyManager`, and the durable indexed counter store.

## Configuration

The MQTT-SN protocol configuration may contain a private `protection`
section (it is deliberately not exposed in the REST protocol DTO).

```yaml
protection:
  enabled: true
  senderIdentifier: "0011223344556677"
  outboundScheme: 0x48
  acceptedSchemes: "0x48,0x49"
  counterDirectory: "/opt/maps_data/mqtt-sn/protection"
  keyEntryPasswordEnv: "MQTT_SN_KEY_ENTRY_PASSWORD"
  keyStore:
    type: JCEKS
    store: file
    path: "/opt/maps_data/security/mqtt-sn.jceks"
    passphrase: "<resolve through existing secure configuration>"
  senderAliases:
    "0011223344556677:72": "gateway-aes-256"
    "aabbccddeeff0011:72": "sensor-aes-256"
```

This block is nested inside the MQTT-SN protocol configuration, not at the
top level of the server configuration. Set the named environment variable in
the server process to the **key-entry** password. The keystore passphrase is
loaded by the existing Authentication `KeyStoreManager` configuration.
Do not place a real passphrase into source-controlled configuration files.
Sender identifiers must be 8 octets, hex encoded, with lowercase keys in
`senderAliases`; suffix is the decimal scheme identifier.

An endpoint fails initialization when a selected ServiceLoader provider,
outbound key, or durable counter store is unavailable. Registration for UDP
reads takes place only after successful protection initialization.

## Supported scheme families

* `0x00`, `0x01`: HMAC-SHA256 / HMAC-SHA3-256
* `0x40`–`0x45`: AES-CCM 64/128 tag, 128/192/256 keys (Bouncy Castle)
* `0x46`–`0x48`: AES-GCM 128/192/256 keys
* `0x49`: ChaCha20-Poly1305

Providers are discovered through the standard
`META-INF/services/...MqttSn2CryptoProvider` resource. Discovery is not
authorization: only `acceptedSchemes` are enabled on the endpoint.

The encryption nonce is derived from SHA-256 of the serialized envelope
prefix. Authenticate/decrypt and validate the inner packet **before**
advancing durable replay state. Outgoing counters use durable block
reservations; unused values are discarded across restart.

## Validation before deployment

Do not enable on production endpoints until the server has compiled against
`authentication_library:3.0.3-SNAPSHOT`, focused provider and envelope
tests have passed, and independent interop vectors have confirmed
CSD01 nonce/tag handling. The sender/security association must have a
well-defined key rotation and counter continuity policy.
