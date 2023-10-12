/*
 * Copyright [ 2020 - 2023 ] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.mapsmessaging.api;

import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * Raised if any transactional exceptions are raised due to invalid contexts, like transaction already exists or its been completed so no further actions can occur
 */
public class TransactionException extends IOException {

  TransactionException(@NonNull @NotNull String msg) {
    super(msg);
  }

}
