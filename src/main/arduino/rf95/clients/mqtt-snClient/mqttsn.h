/*
mqttsn.h

The MIT License (MIT)


*/

#ifndef __MQTTSN_H__
#define __MQTTSN_H__

//#define USE_RF12 1
//#define USE_SERIAL 1
#define USE_LORA 1

#define PROTOCOL_ID 0x01

#define FLAG_DUP 0x80
#define FLAG_QOS_0 0x00
#define FLAG_QOS_1 0x20
#define FLAG_QOS_2 0x40
#define FLAG_QOS_M1 0x60
#define FLAG_RETAIN 0x10
#define FLAG_WILL 0x08
#define FLAG_CLEAN 0x04
#define FLAG_TOPIC_NAME 0x00
#define FLAG_TOPIC_PREDEFINED_ID 0x01
#define FLAG_TOPIC_SHORT_NAME 0x02

#define QOS_MASK (FLAG_QOS_0 | FLAG_QOS_1 | FLAG_QOS_2 | FLAG_QOS_M1)
#define TOPIC_MASK (FLAG_TOPIC_NAME | FLAG_TOPIC_PREDEFINED_ID | FLAG_TOPIC_SHORT_NAME)

// Recommended values for timers and counters. All timers are in seconds.
#define T_ADV 960
#define N_ADV 3
#define T_SEARCH_GW 5
#define T_GW_INFO 5
#define T_WAIT 360
#define T_RETRY 15
#define N_RETRY 5

enum return_code_t {
    ACCEPTED,
    REJECTED_CONGESTION,
    REJECTED_INVALID_TOPIC_ID,
    REJECTED_NOT_SUPPORTED
};

enum message_type {
    ADVERTISE,
    SEARCHGW,
    GWINFO,
    CONNECT = 0x04,
    CONNACK,
    WILLTOPICREQ,
    WILLTOPIC,
    WILLMSGREQ,
    WILLMSG,
    REGISTER,
    REGACK,
    PUBLISH,
    PUBACK,
    PUBCOMP,
    PUBREC,
    PUBREL,
    SUBSCRIBE = 0x12,
    SUBACK,
    UNSUBSCRIBE,
    UNSUBACK,
    PINGREQ,
    PINGRESP,
    DISCONNECT,
    WILLTOPICUPD = 0x1a,
    WILLTOPICRESP,
    WILLMSGUPD,
    WILLMSGRESP
};

struct __attribute__ ((packed)) message_header {
    uint8_t length;
    uint8_t type;
};

struct __attribute__ ((packed)) msg_advertise : public message_header {
    uint8_t gw_id;
    uint16_t duration;
};

struct __attribute__ ((packed)) msg_searchgw : public message_header {
    uint8_t radius;
};

struct __attribute__ ((packed)) msg_gwinfo : public message_header {
    uint8_t gw_id;
    char gw_add[0];
};

struct __attribute__ ((packed)) msg_connect : public message_header {
    uint8_t flags;
    uint8_t protocol_id;
    uint16_t duration;
    char client_id[0];
};

struct __attribute__ ((packed)) msg_connack : public message_header {
    return_code_t return_code;
};

struct __attribute__ ((packed)) msg_willtopic : public message_header {
    uint8_t flags;
    char will_topic[0];
};

struct __attribute__ ((packed)) msg_willmsg : public message_header {
    char willmsg[0];
};

struct __attribute__ ((packed)) msg_register : public message_header {
    uint16_t topic_id;
    uint16_t message_id;
    char topic_name[0];
};

struct __attribute__ ((packed)) msg_regack : public message_header {
    uint16_t topic_id;
    uint16_t message_id;
    uint8_t return_code;
};

struct __attribute__ ((packed)) msg_publish : public message_header {
    uint8_t flags;
    uint16_t topic_id;
    uint16_t message_id;
    char data[0];
};

struct __attribute__ ((packed)) msg_puback : public message_header {
    uint16_t topic_id;
    uint16_t message_id;
    uint8_t return_code;
};

struct __attribute__ ((packed)) msg_pubqos2 : public message_header {
    uint16_t message_id;
};

struct __attribute__ ((packed)) msg_subscribe : public message_header {
    uint8_t flags;
    uint16_t message_id;
    union {
        char topic_name[0];
        uint16_t topic_id;
    };
};

struct __attribute__ ((packed)) msg_suback : public message_header {
    uint8_t flags;
    uint16_t topic_id;
    uint16_t message_id;
    uint8_t return_code;
};

struct __attribute__ ((packed)) msg_unsubscribe : public message_header {
    uint8_t flags;
    uint16_t message_id;
    union {
        char topic_name[0];
        uint16_t topic_id;
    };
};

struct __attribute__ ((packed)) msg_unsuback : public message_header {
    uint16_t message_id;
};

struct __attribute__ ((packed)) msg_pingreq : public message_header {
    char client_id[0];
};

struct __attribute__ ((packed)) msg_disconnect : public message_header {
    uint16_t duration;
};

struct __attribute__ ((packed)) msg_willtopicresp : public message_header {
    uint8_t return_code;
};

struct __attribute__ ((packed)) msg_willmsgresp : public message_header {
    uint8_t return_code;
};

#endif
