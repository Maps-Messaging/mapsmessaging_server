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

package io.mapsmessaging.utilities.collections;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

/**
 * This class is inspired from the following entry in
 * <a href="https://stackoverflow.com/questions/2972986/how-to-unmap-a-file-from-memory-mapped-using-filechannel-in-java"> Stack Overflow</a>
 * It has just been split into old and new JDK support functions.
 * <p>
 * <b>
 * Until Java enables a safe way to ensure a map is in fact unmapped when requested than rather wait for a GC to clean which is outside
 * of any programmatic control this is will continue to be the only way to deterministically ensure the buffer and mapped memory
 * is released and closed. This then allows us to delete any underlying file or, in fact, any IO operations on this file.
 * </b><p><b>
 * I am hopeful in some future version of the JDK this is addressed
 * </b>
 *
 * @since 1.0
 * @author Matthew Buckton
 * @version 1.0
 */
public class MappedBufferHelper {

  /**
   * Ensures the ByteBuffer is a direct buffer and then ensures the "cleaner" function is run and, thus, freeing up any resources it may have open.
   * <br>
   * <b>Warning:</b> once this is called the byte buffers state is now in an unknown state and any access or operations on it may cause the JVM to crash.
   * Do not use this unless you are confidant that the byte buffer will no longer be used
   *
   * @param byteBuffer to close and release all resources
   */
  public static void closeDirectBuffer(@NonNull @NotNull ByteBuffer byteBuffer) {
    if (byteBuffer.isDirect()) {
      try {
        newClean(byteBuffer);
      } catch (Exception ex) {
        // we can ignore this really, worst case is the JVM needs to release the byte buffer
      }
    }
  }

  /*
  New JVM mechanism that requires UnSafe to access the method
   */
  // we need access to the cleaner method to ensure the buffers are released in a more deterministic manner
  @java.lang.SuppressWarnings("squid:S3011")
  private static void newClean(ByteBuffer cb) throws ClassNotFoundException, NoSuchFieldException, InvocationTargetException, IllegalAccessException, NoSuchMethodException {
    Class<?> unsafeClass;
    try {
      unsafeClass = Class.forName("sun.misc.Unsafe");
    } catch (Exception ex) {
      // jdk.internal.misc.Unsafe doesn't yet have an invokeCleaner() method,
      // but that method should be added if sun.misc.Unsafe is removed.
      unsafeClass = Class.forName("jdk.internal.misc.Unsafe");
    }
    Method clean = unsafeClass.getMethod("invokeCleaner", ByteBuffer.class);
    clean.setAccessible(true);
    Field theUnsafeField = unsafeClass.getDeclaredField("theUnsafe");
    theUnsafeField.setAccessible(true);
    Object theUnsafe = theUnsafeField.get(null);
    clean.invoke(theUnsafe, cb);
  }

  private MappedBufferHelper(){
    // Nothing to do here
  }
}
