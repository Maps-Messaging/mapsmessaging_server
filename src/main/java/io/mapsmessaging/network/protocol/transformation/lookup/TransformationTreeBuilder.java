/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
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

package io.mapsmessaging.network.protocol.transformation.lookup;

import io.mapsmessaging.configuration.ConfigurationProperties;
import java.util.List;

public class TransformationTreeBuilder {

  public static TreeNode buildTree(List<ConfigurationProperties> transformations ) {
    TreeNode root = new TreeNode();

    for (ConfigurationProperties transformation : transformations) {
      String pattern = transformation.getProperty("pattern");
      String transformationValue = transformation.getProperty("transformation");
      addPatternToTree(root, pattern, transformationValue);
    }
    return root;
  }

  private static void addPatternToTree(TreeNode root, String pattern, String transformationValue) {
    String[] parts = pattern.split("://|/");
    TreeNode currentNode = root;

    for (String part : parts) {
      currentNode = currentNode.getChildren().computeIfAbsent(part.toLowerCase(), k -> new TreeNode());
    }

    currentNode.setTransformation(transformationValue);
  }
}

