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
package io.mapsmessaging.state.drone.tak;

/**
 * The MTI (Message Trust Indicator) server's current view of one twin, as {@link CotEventPolicy}
 * needs it to render a CoT event - not the full MTI wire schema, just what changes about the
 * marker. Built by whatever external adapter is consuming the MTI feed (an SPI
 * {@code StateMessageAdapter} jar, not part of this server) and pushed in via
 * {@link MtiStatusRegistry}.
 *
 * <p>Deliberately does not carry the MTI state string itself, or the opaque
 * {@code domains}/{@code findings} detail - the adapter has already turned those into the three
 * things a CoT event actually has room for: an optional affiliation override, an optional marker
 * colour, and optional remarks text to append. Keeping the translation on the adapter side means
 * this server-side surface never needs to change shape if the MTI wire schema does.
 *
 * @param affiliationOverride single-letter CoT affiliation code (e.g. "u") to substitute into the
 *     twin's own {@code "a-" + affiliation + "-" + classification} type string, or {@code null} to
 *     leave the affiliation as {@link CotEventPolicy} would otherwise have resolved it.
 * @param colorArgb marker tint (signed 32-bit ARGB, e.g. {@code -65536} for opaque red) to apply
 *     to the CoT detail, or {@code null} to leave any existing colour untouched. Field-tested
 *     2026-09-15 against WebTAK: this tint is NOT visibly rendered for full MIL-STD-2525 vehicle
 *     icons (only affiliation/type changes reliably drive icon appearance there) - kept anyway
 *     since some TAK clients/icon types do respect it, and it's still present in {@code <detail>}
 *     for anyone inspecting the raw event.
 * @param remarksSuffix text to append to the event's existing {@code <remarks>} (separated with
 *     " | "), or {@code null}/blank to add nothing.
 * @param readiness standard CoT {@code <status readiness="...">} flag - {@code false} renders as
 *     a visibly degraded icon (dimmed/struck-through, client-dependent) in many TAK clients
 *     WITHOUT changing affiliation or type, added specifically because {@code colorArgb} above
 *     turned out not to be enough on its own. {@code null} leaves readiness unset (the default,
 *     healthy state).
 */
public record MtiLookupResult(String affiliationOverride, Integer colorArgb, String remarksSuffix,
                               Boolean readiness) {
}
