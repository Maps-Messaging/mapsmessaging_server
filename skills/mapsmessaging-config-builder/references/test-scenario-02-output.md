Requirement Mapping
- "Expose AMQP over TCP on 5672" -> `NetworkManager.yaml`: `url`, `endPointConfig.type=tcp`, `protocolConfigs[].type=amqp`
- "Set XML default payload" -> `NetworkManager.yaml`: `protocolConfigs[].messageDefaults.contentType=application/xml`
- "Enable server routing to core-hub" -> `routing.yaml`: `routing.enabled=true`, `predefinedServers[]`

Assumptions
- Realm uses `anon`.
- AMQP session defaults remain unchanged except explicit contentType.

Deployable Config Entity
```diff
*** NetworkManager.yaml
+    -
+      authenticationRealm: anon
+      name: amqp-tcp-ingress-5672
+      endPointConfig:
+        type: tcp
+      protocolConfigs:
+        -
+          type: amqp
+          messageDefaults:
+            contentType: application/xml
+      url: tcp://0.0.0.0:5672/

*** routing.yaml
+  enabled: true
+  predefinedServers:
+    -
+      name: core-hub
+      url: https://core-hub:8080/
```

Apply Steps
```bash
rg -n "type: amqp|contentType: application/xml|tcp://0.0.0.0:5672/" NetworkManager.yaml
rg -n "enabled: true|name: core-hub|url: https://core-hub:8080/" routing.yaml
```

Verification
```bash
# AMQP smoke checks are environment-specific; verify listener then route entry
rg -n "amqp-tcp-ingress-5672|type: amqp" NetworkManager.yaml
rg -n "core-hub" routing.yaml
```
