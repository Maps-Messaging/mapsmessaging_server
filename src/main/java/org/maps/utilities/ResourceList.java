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

package org.maps.utilities;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

/**
 * This is a helper class to load resources from the classpath
 *
 *  * @since 1.0
 *  * @author Matthew Buckton
 *  * @version 1.0
 */
public class ResourceList {

  private ResourceList() {
  }

  /**
   * for all elements of java.class.path get a Collection of resources Pattern pattern = Pattern.compile(".*"); gets all resources
   *
   * @param pattern the pattern to match
   * @return the resources in the order they are found
   * @throws IOException  if there was an issue loading values from the resource bundle
   */
  public static Collection<String> getResources(@NonNull @NotNull final Pattern pattern) throws IOException {
    final ArrayList<String> returnValue = new ArrayList<>();
    final String classPath = System.getProperty("java.class.path", ".");
    final String[] classPathElements = classPath.split(System.getProperty("path.separator"));
    for (final String element : classPathElements) {
      returnValue.addAll(getResources(element, pattern));
    }
    return returnValue;
  }

  private static Collection<String> getResources(final String element, final Pattern pattern)
      throws IOException {
    final ArrayList<String> returnValue = new ArrayList<>();
    final File file = new File(element);
    if (file.isDirectory()) {
      returnValue.addAll(getResourcesFromDirectory(file, pattern));
    } else {
      returnValue.addAll(getResourcesFromJarFile(file, pattern));
    }
    return returnValue;
  }

  private static Collection<String> getResourcesFromJarFile(final File file, final Pattern pattern)
      throws IOException {
    final ArrayList<String> returnValue = new ArrayList<>();
    ZipFile zf = new ZipFile(file);

    final Enumeration<? extends ZipEntry> e = zf.entries();
    while (e.hasMoreElements()) {
      final ZipEntry ze = e.nextElement();
      final String fileName = ze.getName();
      final boolean accept = pattern.matcher(fileName).matches();
      if (accept) {
        returnValue.add(fileName);
      }
    }
    zf.close();
    return returnValue;
  }

  private static Collection<String> getResourcesFromDirectory(
      final File directory, final Pattern pattern) throws IOException {
    final ArrayList<String> returnValue = new ArrayList<>();
    final File[] fileList = directory.listFiles();
    if (fileList != null && fileList.length > 0) {
      for (final File file : fileList) {
        if (file.isDirectory()) {
          returnValue.addAll(getResourcesFromDirectory(file, pattern));
        } else {
          final String fileName = file.getCanonicalPath();
          final boolean accept = pattern.matcher(fileName).matches();
          if (accept) {
            returnValue.add(fileName);
          }
        }
      }
    }
    return returnValue;
  }
}
