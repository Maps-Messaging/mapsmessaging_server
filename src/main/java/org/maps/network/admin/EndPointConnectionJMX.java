package org.maps.network.admin;

import com.udojava.jmx.wrapper.JMXBean;
import com.udojava.jmx.wrapper.JMXBeanOperation;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.management.ObjectInstance;
import org.maps.network.io.connection.EndPointConnection;
import org.maps.utilities.admin.HealthMonitor;
import org.maps.utilities.admin.HealthStatus;
import org.maps.utilities.admin.HealthStatus.LEVEL;
import org.maps.utilities.admin.JMXManager;


@JMXBean(description = "End Point Connection JMX Bean")
public class EndPointConnectionJMX implements HealthMonitor {

  private final EndPointConnection connection;
  private final List<String> typePath;
  private final ObjectInstance mbean;

  public EndPointConnectionJMX(List<String> parent, EndPointConnection connection) {
    this.connection = connection;
    typePath = new ArrayList<>(parent);
    typePath.add("connection="+connection.getProperties().getProperty("direction"));

    mbean = JMXManager.getInstance().register(this, typePath);
  }

  public void close(){
    JMXManager.getInstance().unregister(mbean);
  }

  public List<String> getTypePath() {
    return typePath;
  }


  //<editor-fold desc="JMX Bean functions">
  @JMXBeanOperation(name = "pause", description ="Pauses the connection")
  public void pauseConnection() throws IOException {
    connection.pause();
  }

  //<editor-fold desc="JMX Bean functions">
  @JMXBeanOperation(name = "resume", description ="Resumes the connection")
  public void resumeConnection() throws IOException {
    connection.resume();
  }

  //<editor-fold desc="JMX Bean functions">
  @JMXBeanOperation(name = "stop", description ="Stops the connection")
  public void stopConnection() throws IOException {
    connection.stop();
  }

  //<editor-fold desc="JMX Bean functions">
  @JMXBeanOperation(name = "start", description ="Starts the connection")
  public void startConnection() throws IOException {
    connection.start();
  }

  @Override
  public HealthStatus checkHealth() {
    return new HealthStatus("OK", LEVEL.INFO, "OK", "Network");
  }
}