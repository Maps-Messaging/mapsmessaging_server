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

package io.mapsmessaging.utilities.filtering;

import io.mapsmessaging.configuration.ConfigurationProperties;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class NamespaceFilters {
  private final TrieNode root = new TrieNode();

  public NamespaceFilters(ConfigurationProperties props) {
    if(props != null) {
      Object obj = props.get("namespaceFilters");
      if (obj instanceof List) {
        List<ConfigurationProperties> list = (List<ConfigurationProperties>) obj;
        for (ConfigurationProperties prop : list) {
          addFilter(prop);
        }
      } else if (obj instanceof ConfigurationProperties prop) {
        addFilter(prop);
      }
    }
  }

  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties props = new ConfigurationProperties();
    List<ConfigurationProperties> list = new ArrayList();
    for(NamespaceFilter filter:getAllFilters()){
      list.add(filter.toConfigurationProperties());
    }
    props.put("namespaceFilters", list);
    return props;
  }

  public List<NamespaceFilter> getAllFilters() {
    List<NamespaceFilter> result = new ArrayList<>();
    collectFilters(root, result);
    return result;
  }

  private void collectFilters(TrieNode node, List<NamespaceFilter> out) {
    if (node.filter != null) {
      out.add(node.filter);
    }
    for (TrieNode child : node.children.values()) {
      collectFilters(child, out);
    }
  }

  private void addFilter(ConfigurationProperties prop) {
    try {
      NamespaceFilter filter = new NamespaceFilter(prop);
      addToTrie(filter);
    } catch (IOException e) {
      //ToDo: log this
    }
  }

  private void addToTrie(NamespaceFilter filter) {
    String[] parts = normalize(filter.getNamespace()).split("/");
    TrieNode node = root;
    for (String part : parts) {
      if (part.isEmpty()) continue; // skip root
      node = node.children.computeIfAbsent(part, k -> new TrieNode());
    }
    node.filter = filter;
  }

  public synchronized NamespaceFilter findMatch(String topic) {
    String[] parts = normalize(topic).split("/");
    TrieNode node = root;
    NamespaceFilter best = null;

    if (node.filter != null) {
      best = node.filter;
    }

    for (String part : parts) {
      if (part.isEmpty()) continue;
      node = node.children.get(part);
      if (node == null) break;
      if (node.filter != null) {
        best = node.filter;
      }
    }
    return best;
  }

  private String normalize(String ns) {
    if (ns == null || ns.isEmpty()) return "/";
    String result = ns.trim();
    if (!result.startsWith("/")) result = "/" + result;
    if (result.length() > 1 && result.endsWith("/")) {
      result = result.substring(0, result.length() - 1);
    }
    return result;
  }

  private static class TrieNode {
    Map<String, TrieNode> children = new LinkedHashMap<>();
    NamespaceFilter filter;
  }
}
