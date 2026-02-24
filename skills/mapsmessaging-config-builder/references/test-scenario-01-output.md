Requirement Mapping
- "Expose MQTT over TCP on 1883" -> `/Users/krital/dev/starsense/mapsmessaging_server/NetworkManager.yaml` fields: `endPointServerConfigList[].url`, `endPointConfig.type=tcp`, `protocolConfigs[].type=mqtt`
- "Default JSON payload" -> `/Users/krital/dev/starsense/mapsmessaging_server/NetworkManager.yaml` field: `protocolConfigs[].messageDefaults.contentType=application/json`
- "Map /telemetry namespace" -> `/Users/krital/dev/starsense/mapsmessaging_server/DestinationManager.yaml` fields: `namespace`, `namespaceMapping`
- "Add route server edge-a" -> `/Users/krital/dev/starsense/mapsmessaging_server/routing.yaml` fields: `routing.predefinedServers[]`, `routing.enabled=true`

Assumptions
- `authenticationRealm` defaults to `anon`.
- `proxyProtocol` remains `false`.
- No QoS override requested.

Deployable Config Entity
```diff
*** /Users/krital/dev/starsense/mapsmessaging_server/NetworkManager.yaml
+    -
+      authenticationRealm: anon
+      name: mqtt-tcp-ingress-1883
+      endPointConfig:
+        type: tcp
+      protocolConfigs:
+        -
+          type: mqtt
+          messageDefaults:
+            contentType: application/json
+      url: tcp://0.0.0.0:1883/

*** /Users/krital/dev/starsense/mapsmessaging_server/DestinationManager.yaml
+    -
+      type: Memory
+      namespace: /telemetry/
+      namespaceMapping: /telemetry/
+
*** /Users/krital/dev/starsense/mapsmessaging_server/routing.yaml
+  enabled: true
+  predefinedServers:
+    -
+      name: edge-a
+      url: https://edge-a:8080/
```

Apply Steps
```bash
# apply edits in working tree
# then verify keys exist
rg -n "type: mqtt|contentType: application/json|tcp://0.0.0.0:1883/" /Users/krital/dev/starsense/mapsmessaging_server/NetworkManager.yaml
rg -n "namespace: /telemetry/|namespaceMapping: /telemetry/" /Users/krital/dev/starsense/mapsmessaging_server/DestinationManager.yaml
rg -n "enabled: true|name: edge-a|url: https://edge-a:8080/" /Users/krital/dev/starsense/mapsmessaging_server/routing.yaml
```

Verification
```bash
# producer
mosquitto_pub -h localhost -p 1883 -t /telemetry/test -m '{"ok":true}'
# consumer
timeout 5 mosquitto_sub -h localhost -p 1883 -t /telemetry/# -C 1
```
