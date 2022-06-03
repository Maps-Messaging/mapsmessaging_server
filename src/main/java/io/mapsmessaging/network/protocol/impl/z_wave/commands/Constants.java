package io.mapsmessaging.network.protocol.impl.z_wave.commands;

public class Constants {

  public static final int ACK = 0x06;
  public static final int NAK = 0x15;
  public static final int CAN = 0x18;
  public static final int SOF = 0x01;

  public static final int REQUEST = 0x0;
  public static final int RESPONSE = 0x1;


  public static final int FUNC_ID_SERIAL_API_GET_INIT_DATA = 0x02;

  public static final int FUNC_ID_SERIAL_API_APPL_NODE_INFORMATION = 0x03;

  public static final int FUNC_ID_APPLICATION_COMMAND_HANDLER = 0x04;

  public static final int FUNC_ID_ZW_GET_CONTROLLER_CAPABILITIES = 0x05;

  public static final int FUNC_ID_SERIAL_API_SET_TIMEOUTS = 0x06;

  public static final int FUNC_ID_SERIAL_API_GET_CAPABILITIES = 0x07;

  public static final int FUNC_ID_SERIAL_API_SOFT_RESET = 0x08;

  public static final int FUNC_ID_SERIAL_API_SETUP = 0x0b;

  public static final int FUNC_ID_ZW_SEND_NODE_INFORMATION = 0x12;

  public static final int FUNC_ID_ZW_SEND_DATA = 0x13;

  public static final int FUNC_ID_ZW_GET_VERSION = 0x15;

  public static final int FUNC_ID_ZW_R_F_POWER_LEVEL_SET = 0x17;

  public static final int FUNC_ID_ZW_GET_RANDOM = 0x1c;

  public static final int FUNC_ID_ZW_MEMORY_GET_ID = 0x20;

  public static final int FUNC_ID_MEMORY_GET_BYTE = 0x21;

  public static final int FUNC_ID_ZW_READ_MEMORY = 0x23;

  public static final int FUNC_ID_ZW_SET_LEARN_NODE_STATE = 0x40;

  public static final int FUNC_ID_ZW_GET_NODE_PROTOCOL_INFO = 0x41;

  public static final int FUNC_ID_ZW_SET_DEFAULT = 0x42;

  public static final int FUNC_ID_ZW_NEW_CONTROLLER = 0x43;

  public static final int FUNC_ID_ZW_REPLICATION_COMMAND_COMPLETE = 0x44;

  public static final int FUNC_ID_ZW_REPLICATION_SEND_DATA = 0x45;

  public static final int FUNC_ID_ZW_ASSIGN_RETURN_ROUTE = 0x46;

  public static final int FUNC_ID_ZW_DELETE_RETURN_ROUTE = 0x47;

  public static final int FUNC_ID_ZW_REQUEST_NODE_NEIGHBOR_UPDATE = 0x48;

  public static final int FUNC_ID_ZW_APPLICATION_UPDATE = 0x49;

  public static final int FUNC_ID_ZW_ADD_NODE_TO_NETWORK = 0x4a;

  public static final int FUNC_ID_ZW_REMOVE_NODE_FROM_NETWORK = 0x4b;

  public static final int FUNC_ID_ZW_CREATE_NEW_PRIMARY = 0x4c;

  public static final int FUNC_ID_ZW_CONTROLLER_CHANGE = 0x4d;

  public static final int FUNC_ID_ZW_SET_LEARN_MODE = 0x50;

  public static final int FUNC_ID_ZW_ASSIGN_SUC_RETURN_ROUTE = 0x51;

  public static final int FUNC_ID_ZW_ENABLE_SUC = 0x52;

  public static final int FUNC_ID_ZW_REQUEST_NETWORK_UPDATE = 0x53;

  public static final int FUNC_ID_ZW_SET_SUC_NODE_ID = 0x54;

  public static final int FUNC_ID_ZW_DELETE_SUC_RETURN_ROUTE = 0x55;

  public static final int FUNC_ID_ZW_GET_SUC_NODE_ID = 0x56;

  public static final int FUNC_ID_ZW_REQUEST_NODE_NEIGHBOR_UPDATE_OPTIONS = 0x5a;

  public static final int FUNC_ID_ZW_EXPLORE_REQUEST_INCLUSION = 0x5e;

  public static final int FUNC_ID_ZW_REQUEST_NODE_INFO = 0x60;

  public static final int FUNC_ID_ZW_REMOVE_FAILED_NODE_ID = 0x61;

  public static final int FUNC_ID_ZW_IS_FAILED_NODE_ID = 0x62;

  public static final int FUNC_ID_ZW_REPLACE_FAILED_NODE = 0x63;

  public static final int FUNC_ID_ZW_GET_ROUTING_INFO = 0x80;

  public static final int FUNC_ID_SERIAL_API_SLAVE_NODE_INFO = 0xA0;

  public static final int FUNC_ID_APPLICATION_SLAVE_COMMAND_HANDLER = 0xA1;

  public static final int FUNC_ID_ZW_SEND_SLAVE_NODE_INFO = 0xA2;

  public static final int FUNC_ID_ZW_SEND_SLAVE_DATA = 0xA3;

  public static final int FUNC_ID_ZW_SET_SLAVE_LEARN_MODE = 0xA4;

  public static final int FUNC_ID_ZW_GET_VIRTUAL_NODES = 0xA5;

  public static final int FUNC_ID_ZW_IS_VIRTUAL_NODE = 0xA6;

  public static final int FUNC_ID_ZW_SET_PROMISCUOUS_MODE = 0xD0;

  public static final int FUNC_ID_PROMISCUOUS_APPLICATION_COMMAND_HANDLER = 0xD1;

}
