#!/usr/bin/env python3
from __future__ import annotations

import sys
from pathlib import Path

import yaml


def has_mqtt_old(entries: list[dict]) -> bool:
    for item in entries:
        if not isinstance(item, dict):
            continue
        url = str(item.get("url", ""))
        if ":1883" not in url:
            continue
        protocol = str(item.get("protocol", "")).lower()
        if protocol in {"mqtt", "all"}:
            return True
        for pc in item.get("protocolConfigs", []) or []:
            if isinstance(pc, dict) and str(pc.get("type", "")).lower() == "mqtt":
                return True
    return False


def has_mqtt_new(entries: list[dict]) -> bool:
    for item in entries:
        if not isinstance(item, dict):
            continue
        url = str(item.get("url", ""))
        if ":1883" not in url:
            continue
        for pc in item.get("protocolConfigs", []) or []:
            if isinstance(pc, dict) and str(pc.get("type", "")).lower() == "mqtt":
                return True
    return False


def ensure_old_data_listener(nm: dict) -> bool:
    data = nm.get("data")
    changed = False
    if not isinstance(data, list):
        data = []
        nm["data"] = data
        changed = True
    if not has_mqtt_old(data):
        data.append(
            {
                "name": "runtime-smoke-mqtt-data-1883",
                "url": "tcp://0.0.0.0:1883/",
                "protocol": "mqtt",
                "selectorThreadCount": 5,
            }
        )
        changed = True
    return changed


def ensure_new_endpoint_listener(nm: dict) -> bool:
    eps = nm.get("endPointServerConfigList")
    if not isinstance(eps, list):
        return False
    if has_mqtt_new(eps):
        return False
    eps.append(
        {
            "authenticationRealm": "anon",
            "backlog": 100,
            "endPointConfig": {"type": "tcp", "proxyProtocolMode": "DISABLED"},
            "name": "runtime-smoke-mqtt-1883",
            "protocolConfigs": [{"type": "mqtt", "proxyProtocol": False}],
            "selectorTaskWait": 10,
            "url": "tcp://0.0.0.0:1883/",
        }
    )
    return True


def main() -> int:
    if len(sys.argv) != 2:
        print("usage: ensure_mqtt_listener.py <NetworkManager.yaml>", file=sys.stderr)
        return 2

    path = Path(sys.argv[1])
    doc = yaml.safe_load(path.read_text(encoding="utf-8")) or {}
    nm = doc.setdefault("NetworkManager", {})

    changed = False
    if ensure_old_data_listener(nm):
        changed = True
    if ensure_new_endpoint_listener(nm):
        changed = True

    if changed:
        path.write_text(yaml.safe_dump(doc, sort_keys=False, allow_unicode=False), encoding="utf-8")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
