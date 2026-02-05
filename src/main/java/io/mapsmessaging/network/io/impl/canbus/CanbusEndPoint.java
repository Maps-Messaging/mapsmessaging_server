package io.mapsmessaging.network.io.impl.canbus;

import com.fazecast.jSerialComm.SerialPort;
import io.mapsmessaging.canbus.device.SocketCanDevice;
import io.mapsmessaging.canbus.device.frames.CanFrame;
import io.mapsmessaging.dto.rest.config.network.SerialDeviceDTO;
import io.mapsmessaging.dto.rest.config.network.impl.CanbusConfigDTO;
import io.mapsmessaging.dto.rest.config.network.impl.SerialConfigDTO;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.network.admin.EndPointJMX;
import io.mapsmessaging.network.io.*;
import io.mapsmessaging.network.io.impl.serial.SerialEndPoint;
import io.mapsmessaging.network.io.impl.serial.SimpleStreamHandler;
import io.mapsmessaging.network.io.impl.serial.threads.SerialIoExecutors;
import io.mapsmessaging.network.io.impl.serial.threads.SerialIoPoolHandle;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SelectionKey;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

import static com.fazecast.jSerialComm.SerialPort.TIMEOUT_READ_BLOCKING;
import static com.fazecast.jSerialComm.SerialPort.TIMEOUT_WRITE_BLOCKING;

public class CanbusEndPoint  {

  private final AtomicBoolean closed;
  private final SocketCanDevice canDevice;

  public CanbusEndPoint(CanbusConfigDTO config) throws IOException {
    closed = new AtomicBoolean(false);
    try {
      canDevice = new SocketCanDevice(config.getDeviceName());
    }
    catch(Throwable th){
      throw new IOException(th);
    }
  }

  public void close() throws IOException {
    closed.set(true);
    canDevice.close();
  }

  public CanFrame readFrame() throws IOException {
    if( closed.get()){
      throw new IOException("CanbusEndPoint is closed");
    }
    return  canDevice.readFrame();
  }

  public void writeFrame(CanFrame frame) throws IOException {
    if( closed.get()){
      throw new IOException("CanbusEndPoint is closed");
    }
    canDevice.writeFrame(frame);
  }
}
