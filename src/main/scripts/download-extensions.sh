#!/usr/bin/env bash
#
#
#  Copyright [ 2020 - 2024 ] Matthew Buckton
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
#!/usr/bin/env bash
set -euo pipefail

REPO_BASE="https://repository.mapsmessaging.io/repository/maps_snapshots"

download_latest_snapshot_jar_and_rename() {
  local group_id="$1"
  local artifact_id="$2"
  local version="$3"          # e.g. 1.0.0-SNAPSHOT
  local classifier="${4:-}"   # optional

  local group_path
  group_path="$(echo "${group_id}" | tr '.' '/')"

  local artifact_dir="${REPO_BASE}/${group_path}/${artifact_id}/${version}"
  local metadata_url="${artifact_dir}/maven-metadata.xml"

  echo "==> ${group_id}:${artifact_id}:${version}${classifier:+:${classifier}}"
  echo "    metadata: ${metadata_url}"

  local metadata
  metadata="$(curl -fsSL "${metadata_url}")"

  local jar_value=""
  if [[ -n "${classifier}" ]]; then
    jar_value="$(echo "${metadata}" | awk -v cls="${classifier}" '
      BEGIN { RS="</snapshotVersion>"; FS="\n" }
      $0 ~ /<snapshotVersion>/ && $0 ~ "<extension>jar</extension>" && $0 ~ "<classifier>"cls"</classifier>" {
        if (match($0, /<value>[^<]+<\/value>/)) {
          v=substr($0, RSTART+7, RLENGTH-15); print v; exit
        }
      }')"
  else
    jar_value="$(echo "${metadata}" | awk '
      BEGIN { RS="</snapshotVersion>"; FS="\n" }
      $0 ~ /<snapshotVersion>/ && $0 ~ "<extension>jar</extension>" && $0 !~ "<classifier>" {
        if (match($0, /<value>[^<]+<\/value>/)) {
          v=substr($0, RSTART+7, RLENGTH-15); print v; exit
        }
      }')"
  fi

  if [[ -z "${jar_value}" ]]; then
    echo "    ERROR: Could not find a jar snapshotVersion in metadata." >&2
    exit 1
  fi

  local timestamped_file="${artifact_id}-${jar_value}.jar"
  local jar_url="${artifact_dir}/${timestamped_file}"

  local final_file="${artifact_id}-${version}.jar"  # <-- the name you want

  echo "    download: ${jar_url}"
  curl -fL --retry 3 --retry-delay 1 -o "${timestamped_file}" "${jar_url}"

  mv -f "${timestamped_file}" "${final_file}"
  echo "    saved as: ${final_file}"
  echo
}

# ---- Examples (fill in the exact artifactIds/versions you use) ----
download_latest_snapshot_jar_and_rename "io.mapsmessaging" "aws-sns-extension"   "1.0.0-SNAPSHOT"
download_latest_snapshot_jar_and_rename "io.mapsmessaging" "ibm-mq-extension"    "1.0.0-SNAPSHOT"
download_latest_snapshot_jar_and_rename "io.mapsmessaging" "pulsar-extension"    "1.0.0-SNAPSHOT"
download_latest_snapshot_jar_and_rename "io.mapsmessaging" "v2x-step-extension"  "1.0.0-SNAPSHOT"
