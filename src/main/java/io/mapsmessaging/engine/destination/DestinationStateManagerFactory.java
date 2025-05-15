/*
 *
 *  Copyright [ 2020 - 2024 ] [Matthew Buckton]
 *  Copyright [ 2024 - 2025 ] [Maps Messaging B.V.]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package io.mapsmessaging.engine.destination;

import io.mapsmessaging.engine.Constants;
import io.mapsmessaging.engine.destination.delayed.DelayedMessageManager;
import io.mapsmessaging.engine.destination.delayed.TransactionalMessageManager;
import io.mapsmessaging.engine.destination.subscription.state.LimitedMessageStateManager;
import io.mapsmessaging.engine.destination.subscription.state.MessageStateManagerImpl;
import io.mapsmessaging.engine.utils.FilePathHelper;
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

  public MessageStateManagerImpl create(DestinationImpl destinationImpl, boolean persistent, String name, int maxAtRest) throws IOException {
    BitSetFactory factory = createFactory(destinationImpl, persistent, name);
    if(maxAtRest > 0){
      return new LimitedMessageStateManager(name, factory, maxAtRest, destinationImpl.getCompletionQueue());
    }
    return new MessageStateManagerImpl(name, factory);
  }

  public DelayedMessageManager createDelayed(DestinationImpl destinationImpl, boolean persistent, String name) throws IOException {
    BitSetFactory factory = createFactory(destinationImpl, persistent, name);
    return new DelayedMessageManager(factory);
  }

  public TransactionalMessageManager createTransaction(DestinationImpl destinationImpl, boolean persistent, String name) throws IOException {
    BitSetFactory factory = createFactory(destinationImpl, persistent, name);
    return new TransactionalMessageManager(factory);
  }

  private BitSetFactory createFactory(DestinationImpl destinationImpl, boolean persistent, String name) throws IOException {
    if (persistent && destinationImpl.isPersistent()) {
      String fullyQualifiedPath = FilePathHelper.cleanPath(destinationImpl.getPhysicalLocation());
      fullyQualifiedPath += "state";
      File directory = new File(fullyQualifiedPath);
      if(!directory.exists()) {
        Files.createDirectories(directory.toPath());
      }
      fullyQualifiedPath = FilePathHelper.cleanPath(fullyQualifiedPath + File.separator + name + ".bin");
      return new FileBitSetFactoryImpl(fullyQualifiedPath, Constants.BITSET_BLOCK_SIZE);
    } else {
      return new BitSetFactoryImpl(Constants.BITSET_BLOCK_SIZE);
    }
  }
}
