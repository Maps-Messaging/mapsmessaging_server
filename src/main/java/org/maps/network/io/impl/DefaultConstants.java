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

package org.maps.network.io.impl;

public class DefaultConstants {

  static final int TCP_READ_BUFFER_SIZE  = 1000000;
  static final int TCP_WRITE_BUFFER_SIZE = 1000000;

  static final int TCP_READ_FRAGMENTATION_LIMIT = 5;
  static final int TCP_READ_DELAY_ON_FRAGMENTATION = 100; // ms
  static final boolean TCP_READ_DELAY_ENABLED = true;

  private DefaultConstants(){}
}
