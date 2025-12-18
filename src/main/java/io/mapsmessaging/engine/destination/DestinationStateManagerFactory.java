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

package io.mapsmessaging.engine.destination;

import io.mapsmessaging.engine.Constants;
import io.mapsmessaging.engine.destination.delayed.DelayedMessageManager;
import io.mapsmessaging.engine.destination.delayed.TransactionalMessageManager;
import io.mapsmessaging.engine.destination.subscription.state.LimitedMessageStateManager;
import io.mapsmessaging.engine.destination.subscription.state.MessageStateManagerImpl;
import io.mapsmessaging.engine.utils.FilePathHelper;
import io.mapsmessaging.utilities.collections.bitset.BitSetFactory;
import io.mapsmessaging.utilities.collections.bitset.BitSetFactoryImpl;
import io.mapsmessaging.utilities.collections.bitset.ConcurrentSharedFileBitSetFactoryImpl;
import io.mapsmessaging.utilities.collections.bitset.FileBitSetFactoryImpl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class DestinationStateManagerFactory {


  public static MessageStateManagerImpl create(DestinationImpl destinationImpl, boolean persistent, String name, long uniqueSessionId, int maxAtRest) throws IOException {
    BitSetFactory factory;
    if(persistent) {
      factory = destinationImpl.getSubscriptionBitsetFactory();
    }
    else {
      factory =  new BitSetFactoryImpl(Constants.BITSET_BLOCK_SIZE);
    }
    if(maxAtRest > 0){
      return new LimitedMessageStateManager(name, uniqueSessionId, factory, maxAtRest, destinationImpl.getCompletionQueue());
    }
    return new MessageStateManagerImpl(name, uniqueSessionId, factory);
  }

  public static DelayedMessageManager createDelayed(DestinationImpl destinationImpl, boolean persistent, String name) throws IOException {
    BitSetFactory factory = createFactory(destinationImpl, persistent, name);
    return new DelayedMessageManager(factory);
  }

  public static TransactionalMessageManager createTransaction(DestinationImpl destinationImpl, boolean persistent, String name) throws IOException {
    BitSetFactory factory = createFactory(destinationImpl, persistent, name);
    return new TransactionalMessageManager(factory);
  }

  private static BitSetFactory createFactory(DestinationImpl destinationImpl, boolean persistent, String name) throws IOException {
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

  public static BitSetFactory createSubscriptionFactory(DestinationImpl destinationImpl, boolean persistent, String name) throws IOException {
    if (persistent && destinationImpl.isPersistent()) {
      String fullyQualifiedPath = FilePathHelper.cleanPath(destinationImpl.getPhysicalLocation());
      fullyQualifiedPath += "state";
      File directory = new File(fullyQualifiedPath);
      if(!directory.exists()) {
        Files.createDirectories(directory.toPath());
      }
      fullyQualifiedPath = FilePathHelper.cleanPath(fullyQualifiedPath + File.separator + name + ".bin");
      return new ConcurrentSharedFileBitSetFactoryImpl(fullyQualifiedPath,4, Constants.BITSET_BLOCK_SIZE);
    } else {
      return new BitSetFactoryImpl(Constants.BITSET_BLOCK_SIZE);
    }
  }

  private static int bitsRequired(int maxValue) {
    if (maxValue < 0) throw new IllegalArgumentException("maxValue must be >= 0");
    return 32 - Integer.numberOfLeadingZeros(maxValue);
  }
  private DestinationStateManagerFactory(){}
}
