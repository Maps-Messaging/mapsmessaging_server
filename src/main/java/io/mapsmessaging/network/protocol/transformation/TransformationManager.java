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

package io.mapsmessaging.network.protocol.transformation;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.network.protocol.ProtocolMessageTransformation;
import io.mapsmessaging.network.protocol.transformation.lookup.TransformationTreeBuilder;
import io.mapsmessaging.network.protocol.transformation.lookup.TreeNode;
import io.mapsmessaging.utilities.configuration.ConfigurationManager;
import io.mapsmessaging.utilities.service.Service;
import io.mapsmessaging.utilities.service.ServiceManager;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("java:S6548") // yes it is a singleton
public class TransformationManager implements ServiceManager {

  private static class Holder {
    static final TransformationManager INSTANCE = new TransformationManager();
  }

  public static TransformationManager getInstance() {
    return Holder.INSTANCE;
  }

  private final Map<String, ProtocolMessageTransformation> lookupMap;
  private final TreeNode root;

  private TransformationManager() {
    ServiceLoader<ProtocolMessageTransformation> transformations = ServiceLoader.load(ProtocolMessageTransformation.class);
    lookupMap = new ConcurrentHashMap<>();
    for(ProtocolMessageTransformation transformation : transformations) {
      lookupMap.put(transformation.getName().toLowerCase(), transformation);
    }
    ConfigurationProperties properties = ConfigurationManager.getInstance().getProperties("TransformationManager");
    Object obj = properties.get("data");
    if(obj != null) {
      if(obj instanceof ConfigurationProperties conf){
        root = TransformationTreeBuilder.buildTree(Collections.singletonList(conf));
      }
      else if(obj instanceof List){
        List<ConfigurationProperties> data = (List<ConfigurationProperties>) obj;
        root = TransformationTreeBuilder.buildTree(data);
      }
      else{
        root = null;
      }
    } else {
      root = null;
    }
  }

  public ProtocolMessageTransformation getTransformation(@NotNull String transport, @NotNull String host, @NotNull String protocol, String username) {
    String subHost = host.substring(host.indexOf("/")+1);
    if (subHost.contains(":")) {
      subHost = subHost.substring(0, subHost.indexOf(":"));
    }
    if(username == null)username="anonymous";
    String[] parts = {transport.toLowerCase(), subHost.toLowerCase(), protocol.toLowerCase(), username.toLowerCase()};
    String name =  findTransformation(root, parts, 0);
    return getTransformation(name);
  }

  private String findTransformation(TreeNode node, String[] parts, int level) {
    if (node == null) {
      return null;
    }

    if (level == parts.length) {
      return node.getTransformation();
    }

    TreeNode exactMatch = node.getChild(parts[level]);
    TreeNode wildcardMatch = node.getChild("*");

    String result = findTransformation(exactMatch, parts, level + 1);
    if (result == null) {
      result = findTransformation(wildcardMatch, parts, level + 1);
    }

    return result;
  }

  @Override
  public Iterator<Service> getServices() {
    List<Service> service = new ArrayList<>(lookupMap.values());
    return service.listIterator();
  }

  public ProtocolMessageTransformation getTransformation(String name) {
    if(name == null || name.isEmpty()) return null;
    return lookupMap.get(name.toLowerCase());
  }

  public ProtocolMessageTransformation getTransformation(int id) {
    for(ProtocolMessageTransformation transformation : lookupMap.values()) {
      if(transformation.getId() == id) return transformation;
    }
    return null;
  }

}


