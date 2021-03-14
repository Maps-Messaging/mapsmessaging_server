/*
 *
 *   Copyright [ 2020 - 2021 ] [Matthew Buckton]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

/**
 * Classes in the selector package override equals and hashCode to facilitate fast validation if the selectors are the same.
 * Here we are not interested in object equivalence but, rather, context equivalence. Say you have two selectors
 * <code>
 * key &gt;= 1 and key &lt;= 10
 *
 * key between 1 and 10
 *</code>
 * These are the same in context but not in selector string.  The equal and hashcode overrides mean we can explore the actual context
 * of the selector to detect common selectors and then use the same filter for both
 *
 * @link https://www.baeldung.com/java-equals-hashcode-contracts for details on when to override or not
 */

package org.maps.selector;