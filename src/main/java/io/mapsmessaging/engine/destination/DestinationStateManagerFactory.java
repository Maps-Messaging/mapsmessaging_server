/*
 *    Copyright [ 2020 - 2021 ] [Matthew Buckton]
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *
 */

package io.mapsmessaging.engine.destination;

import io.mapsmessaging.engine.Constants;
import io.mapsmessaging.engine.destination.delayed.DelayedMessageManager;
import io.mapsmessaging.engine.destination.delayed.TransactionalMessageManager;
import io.mapsmessaging.engine.destination.subscription.state.MessageStateManagerImpl;
import io.mapsmessaging.utilities.collections.bitset.BitSetFactory;
import io.mapsmessaging.utilities.collections.bitset.BitSetFactoryImpl;
import io.mapsmessaging.utilities.collections.bitset.FileBitSetFactoryImpl;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class DestinationStateManagerFactory {

  private static final DestinationStateManagerFactory instance = new DestinationStateManagerFactory();

  public static DestinationStateManagerFactory getInstance() {
    return instance;
  }

  public MessageStateManagerImpl create(DestinationImpl destinationImpl, boolean persistent, String name) throws IOException {
    BitSetFactory factory = createFactory(destinationImpl, persistent, name);
    return new MessageStateManagerImpl(name, factory);
  }

  public DelayedMessageManager createDelayed (DestinationImpl destinationImpl, boolean persistent, String name) throws IOException {
    BitSetFactory factory = createFactory(destinationImpl, persistent, name);
    return new DelayedMessageManager(factory);
  }

  public TransactionalMessageManager createTransaction (DestinationImpl destinationImpl, boolean persistent, String name) throws IOException {
    BitSetFactory factory = createFactory(destinationImpl, persistent, name);
    return new TransactionalMessageManager(factory);
  }

  private BitSetFactory createFactory(DestinationImpl destinationImpl, boolean persistent, String name ) throws IOException {
    if (persistent && destinationImpl.isPersistent()) {
      String tmpName = destinationImpl.getPhysicalLocation();
      if (File.separatorChar == '/') {
        while (tmpName.indexOf('\\') != -1) {
          tmpName = tmpName.replace("\\", File.separator);
        }
      } else {
        while (tmpName.indexOf('/') != -1) {
          tmpName = tmpName.replace("/", File.separator);
        }
      }
      // Find the path to the resource,
      tmpName += "state";
      File directory = new File(tmpName);
      Files.createDirectories(directory.toPath());
      tmpName += File.separator + name + ".bin";
      return new FileBitSetFactoryImpl(tmpName, Constants.BITSET_BLOCK_SIZE);
    } else {
      return new BitSetFactoryImpl(Constants.BITSET_BLOCK_SIZE);
    }
  }
}
