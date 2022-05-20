package io.mapsmessaging.network.io.impl.hmac;

import io.mapsmessaging.network.admin.EndPointManagerJMX;
import io.mapsmessaging.network.io.EndPointServer;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.impl.Selector;
import io.mapsmessaging.network.io.impl.udp.UDPEndPoint;
import io.mapsmessaging.network.io.security.NodeSecurity;
import io.mapsmessaging.network.io.security.PacketIntegrity;
import io.mapsmessaging.utilities.scheduler.SimpleTaskScheduler;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class HmacUDPEndPoint extends UDPEndPoint {

  private final Map<String, NodeSecurity> securityMap;
  private final Map<String, IntegrityAddressMap> cacheMap;
  private final Future cacheMonitor;
  private final long cacheExpiryTime;


  public HmacUDPEndPoint(
      InetSocketAddress inetSocketAddress,
      Selector selector,
      long id,
      EndPointServer server,
      String authConfig,
      EndPointManagerJMX managerMBean,
      Map<String, NodeSecurity> securityMap
  ) throws IOException {
    super(inetSocketAddress, selector, id, server, authConfig, managerMBean);
    this.securityMap = securityMap;
    cacheMap = new ConcurrentHashMap<>();
    cacheExpiryTime = getConfig().getProperties().getLongProperty("HMACHostLookupCacheExpiry", 60);
    cacheMonitor = SimpleTaskScheduler.getInstance().scheduleAtFixedRate(new CacheTrimmer(), cacheExpiryTime, cacheExpiryTime, TimeUnit.SECONDS);

  }

  @Override
  public void close(){
    super.close();
    cacheMonitor.cancel(true);
    cacheMap.clear();
  }

  @Override
  public int sendPacket(Packet packet) throws IOException {
    PacketIntegrity packetIntegrity = lookup((InetSocketAddress) packet.getFromAddress());
    if(packetIntegrity == null){
      packet.clear();
      return 0;
//      throw new IOException("No HMAC configuration found for "+packet.getFromAddress());
    }
    packet = packetIntegrity.secure(packet);
    return super.sendPacket(packet);
  }

  @Override
  public int readPacket(Packet packet) throws IOException {
    int res = super.readPacket(packet);
    packet.flip();
    PacketIntegrity packetIntegrity = lookup((InetSocketAddress) packet.getFromAddress());
    if(packetIntegrity == null){
      packet.clear();
      res = 0;
//      throw new IOException("No HMAC configuration found for "+packet.getFromAddress());
    }
    else {
      if (packet.hasRemaining()) {
        if (!packetIntegrity.isSecure(packet)) {
          packet.clear();
          return 0;
        }
      }
    }
    return res;
  }

  @Override
  public String getProtocol() {
    return "hmac";
  }

  private PacketIntegrity lookup(InetSocketAddress address){
    if(address == null){
      return null;
    }
    IntegrityAddressMap addressMap = cacheMap.get(address.toString());
    if(addressMap != null){
      return addressMap.getPacketIntegrity();
    }

    List<String> potentialKeys = new ArrayList<>();
    potentialKeys.add(address.getAddress().getHostName()+":"+address.getPort());
    potentialKeys.add(address.getAddress().getHostName()+":0");
    potentialKeys.add(address.getAddress().getHostAddress()+":"+address.getPort());
    potentialKeys.add(address.getAddress().getHostAddress()+":0");

    for(String key:potentialKeys){
      PacketIntegrity packetIntegrity = lookup(key);
      if(packetIntegrity != null){
        cacheMap.put(address.toString(), new IntegrityAddressMap(packetIntegrity));
        return packetIntegrity;
      }
    }
    return null;
  }


  private PacketIntegrity lookup(String key){
    NodeSecurity lookup = securityMap.get(key);
    if(lookup != null){
      return lookup.getPacketIntegrity();
    }
    return null;
  }


  private static class IntegrityAddressMap {
    private final PacketIntegrity packetIntegrity;
    private long lastAccess;

    public IntegrityAddressMap(PacketIntegrity packetIntegrity){
      this.packetIntegrity = packetIntegrity;
      lastAccess = System.currentTimeMillis();
    }

    public PacketIntegrity getPacketIntegrity(){
      lastAccess = System.currentTimeMillis();
      return packetIntegrity;
    }
  }

  private final class CacheTrimmer implements Runnable{

    @Override
    public void run() {
      List<String> expiredKeys = new ArrayList<>();
      if(!cacheMap.isEmpty()) {
        long expiryTime = System.currentTimeMillis() - cacheExpiryTime;
        for (Entry<String, IntegrityAddressMap> entry : cacheMap.entrySet()) {
          if (entry.getValue().lastAccess < expiryTime) {
            expiredKeys.add(entry.getKey());
          }
        }

        for (String key : expiredKeys) {
          cacheMap.remove(key);
        }
      }
    }
  }
}
