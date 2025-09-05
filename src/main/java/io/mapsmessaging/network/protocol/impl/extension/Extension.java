/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
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

package io.mapsmessaging.network.protocol.impl.extension;

import io.mapsmessaging.api.message.Message;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

@Getter
public abstract class Extension {

  private ExtensionProtocol extensionProtocol;
  private boolean initialized = false;

  protected Extension() {
  }

  public final void initializeExtension() throws IOException {
    if (!initialized) {
      initialise();
      initialized = true;
    } else {
      throw new IllegalStateException("Extension is already initialized");
    }
  }

  public void close() throws IOException {
    if (extensionProtocol != null) {
      extensionProtocol.close();
    }
  }

  public String getSessionId() {
    if (extensionProtocol == null) {
      throw new IllegalStateException("ExtensionProtocol is not set");
    }
    return extensionProtocol.getSessionId();
  }

  public abstract void initialise() throws IOException;

  public abstract @NonNull @NotNull String getName();

  public abstract @NotNull String getVersion();

  public abstract boolean supportsRemoteFiltering();

  public abstract void outbound(@NonNull @NotNull String destinationName, @NonNull @NotNull Message message);

  protected void inbound(@NonNull @NotNull String destinationName,  @NonNull @NotNull Message message) throws IOException {
    if (extensionProtocol == null) {
      throw new IllegalStateException("ExtensionProtocol is not set");
    }
    try {
      extensionProtocol.saveMessage(destinationName, message);
    } catch (ExecutionException | InterruptedException | TimeoutException e) {
      throw new IOException("Error processing inbound message", e);
    }
  }

  public abstract void registerRemoteLink(@NonNull @NotNull String destination, @Nullable String filter) throws IOException;

  public abstract void registerLocalLink(@NonNull @NotNull String destination) throws IOException;

  protected final void setExtensionProtocol(@NonNull ExtensionProtocol protocol) {
    if (this.extensionProtocol != null) {
      throw new IllegalStateException("ExtensionProtocol is already set");
    }
    this.extensionProtocol = protocol;
  }
}
