package io.mapsmessaging.engine.resources;

import io.mapsmessaging.api.message.Message;
import java.io.Closeable;
import java.io.IOException;

public interface BaseResource extends Closeable {

  //<editor-fold desc="Management API">
  // Remove any resources used. When this call finishes this object must have released any
  // File descriptors, memory maps etc that had been used
  void delete() throws IOException;
  //</editor-fold>


  //<editor-fold desc="Message access API">

  // Add a message to the resource
  void add(Message message) throws IOException;

  // Remove the message based on the key supplied
  void remove(long key) throws IOException;

  // Return the message that matches the key
  Message get(long key) throws IOException;

  // The number of messages currently stored
  long size() throws IOException;

  // If the resource has any messages
  boolean isEmpty();

  //</editor-fold>

}
