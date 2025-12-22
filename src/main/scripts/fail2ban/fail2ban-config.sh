#!/usr/bin/env bash
#
#
#  Copyright [ 2020 - 2024 ] Matthew Buckton
#  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
#
#  Licensed under the Apache License, Version 2.0 with the Commons Clause
#  (the "License"); you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at:
#
#      http://www.apache.org/licenses/LICENSE-2.0
#      https://commonsclause.com/
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

FILTER_SRC="${SCRIPT_DIR}/maps-auth.conf"
JAIL_SRC="${SCRIPT_DIR}/maps-auth.local"

FILTER_DST="/etc/fail2ban/filter.d/maps-auth.conf"
JAIL_DST="/etc/fail2ban/jail.d/maps-auth.local"

if [[ "${EUID}" -ne 0 ]]; then
  echo "This script must be run as root (use sudo)." >&2
  exit 1
fi

if ! command -v fail2ban-client >/dev/null 2>&1; then
  echo "fail2ban-client not found. Fail2ban does not appear to be installed." >&2
  exit 1
fi

if [[ ! -f "${FILTER_SRC}" ]]; then
  echo "Missing filter file: ${FILTER_SRC}" >&2
  exit 1
fi

if [[ ! -f "${JAIL_SRC}" ]]; then
  echo "Missing jail file: ${JAIL_SRC}" >&2
  exit 1
fi

echo "Installing Fail2ban filter: ${FILTER_SRC} -> ${FILTER_DST}"
install -m 0644 -D "${FILTER_SRC}" "${FILTER_DST}"

echo "Installing Fail2ban jail override: ${JAIL_SRC} -> ${JAIL_DST}"
install -m 0644 -D "${JAIL_SRC}" "${JAIL_DST}"

echo "Validating Fail2ban configuration..."
fail2ban-client -t

echo "Reloading Fail2ban..."
if systemctl is-active --quiet fail2ban; then
  systemctl restart fail2ban
else
  # In case someone is running it without systemd, or it is stopped.
  # This will at least apply config if it's running.
  fail2ban-client reload || true
fi

echo "Checking jail status for 'maps-auth'..."
fail2ban-client status maps-auth || true

echo "Done."
