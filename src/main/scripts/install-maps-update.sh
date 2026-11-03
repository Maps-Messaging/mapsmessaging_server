#!/usr/bin/env bash
#
#  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
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

if (( EUID != 0 )); then
  echo "This script must be run as root." >&2
  exit 1
fi

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
readonly UPDATE_SCRIPT="/usr/local/sbin/update-maps.sh"
readonly CRON_FILE="/etc/cron.d/maps-update"

install -m 0755 "${SCRIPT_DIR}/update-maps.sh" "${UPDATE_SCRIPT}"
printf '%s\n' '0 0 * * * root /usr/local/sbin/update-maps.sh >> /var/log/maps-update.log 2>&1' | install -m 0644 /dev/stdin "${CRON_FILE}"

echo "Maps update scheduled for midnight daily (system local time)."
