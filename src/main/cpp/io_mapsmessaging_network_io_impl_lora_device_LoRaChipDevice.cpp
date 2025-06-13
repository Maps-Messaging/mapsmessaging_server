/*
 #
 # Copyright [ 2020 - 2024 ] [Matthew Buckton]
 # Copyright [ 2024 - 2025 ] [Maps Messaging B.V.]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
* This code interfaces a LoRa device with Java.
*
* Requires :
*       Java for JNI headers and libs
*       Radio Head for the datagram and radio management
*       BCM2835 for the Raspberry Pi GPIO interface
*/
#include "io_mapsmessaging_network_io_impl_lora_device_LoRaChipDevice.h"
#include <bcm2835.h>
#include <stdio.h>
#include <signal.h>
#include <unistd.h>
#include <RHReliableDatagram.h>
#include <RH_RF95.h>

#define MAX_RADIOS_SUPPORTED 10
//--------------------------------------------------------

//--------------------------------------------------------
// Chose the chipset that you have
bool bcm2835Initialised = false;

// Support for multiple radio devices.
struct RADIO {
  char* name;
  RH_RF95* rf95;
  RHReliableDatagram* reliableDatagram;
  uint8_t irq_pin;
};
RADIO radios[MAX_RADIOS_SUPPORTED];

uint8_t radioIndex = 0;

//------------------------------------------------------------------------------------------
// Send a String to the Java layer to log to the system logger
//
void log(JNIEnv *env, jobject obj, const char* message){
      jclass cls = env->GetObjectClass(obj);
      jmethodID logFunction = env->GetMethodID(cls, "log", "(Ljava/lang/String;)V");
      if (logFunction == 0) {
          return;
      }
      jstring jStringMessage = env->NewStringUTF(message);
      env->CallVoidMethod(obj, logFunction, jStringMessage);
}

//------------------------------------------------------------------------------------------
JNIEXPORT jint JNICALL Java_io_mapsmessaging_network_io_impl_lora_device_LoRaDevice_init
   (JNIEnv *env, jobject obj, jint node, jfloat frequency, jint cs, jint irq, jint rst){
   if(!bcm2835Initialised){
      bcm2835Initialised = true;
      if (!bcm2835_init()) {
        log(env, obj, "Failed to initialise the BCM2835 GPIO interface");
        return -1;
      }
      else{
        log(env, obj, "Successfully initialised the BCM2835 GPIO interface");
      }
    }
    jint radioHandle = radioIndex;
    radioIndex++;
    //
    // Setup the interrupt pin
    //
    radios[radioHandle].irq_pin = irq;
    pinMode(irq, INPUT);
    bcm2835_gpio_set_pud(irq, BCM2835_GPIO_PUD_DOWN);
    bcm2835_gpio_ren(irq);
    //
    // Reset the Wireless chip
    //
    pinMode(rst, OUTPUT);
    digitalWrite(rst, LOW );
    bcm2835_delay(150);
    digitalWrite(rst, HIGH );
    bcm2835_delay(100);

    radios[radioHandle].rf95 = new RH_RF95(cs, irq);
    radios[radioHandle].reliableDatagram = new RHReliableDatagram(*radios[radioHandle].rf95, node);
    if(!radios[radioHandle].reliableDatagram->init()){
      log(env, obj,"Failed to initialise the Datagram layer");
      return -1;
    }
    else{
      log(env, obj, "Successfully initialised the Datagram layer");
    }
    if (!radios[radioHandle].rf95->init()) {
      log(env, obj, "Failed to initialise the RF95 Radio, please check wiring configuration" );
      return -1;
    }
    else{
       log(env, obj, "Successfully initialised the RF95 Radio" );
    }

    if(!radios[radioHandle].rf95->setFrequency(frequency)){
      log(env, obj, "Failed to set the configured frequency");
    }
    else{
      log(env, obj, "Successfully set the configured frequency");
    }
    radios[radioHandle].rf95->setThisAddress(node);
    radios[radioHandle].rf95->setHeaderFrom(node);
    radios[radioHandle].rf95->setModeRx();
    return radioHandle;
  }

//------------------------------------------------------------------------------------------
JNIEXPORT jboolean JNICALL Java_io_mapsmessaging_network_io_impl_lora_device_LoRaDevice_setPower
  (JNIEnv * env, jobject obj, jint radioHandle, jint power, jboolean flag){
    if(radios[radioHandle].rf95){
      radios[radioHandle].rf95->setTxPower(power, flag);
      return 1;
    }
    return 0;
  }

//------------------------------------------------------------------------------------------
JNIEXPORT jboolean JNICALL Java_io_mapsmessaging_network_io_impl_lora_device_LoRaDevice_setCADTimeout
  (JNIEnv *env, jobject obj, jint radioHandle, jint timout){
    if(radios[radioHandle].rf95){
      radios[radioHandle].rf95->setCADTimeout(timout);
      return 1;
    }
    return 0;
  }

//------------------------------------------------------------------------------------------
JNIEXPORT jboolean JNICALL Java_io_mapsmessaging_network_io_impl_lora_device_LoRaDevice_write
  (JNIEnv * env, jobject obj, jint radioHandle, jbyteArray jbuf, jint len, jbyte from, jbyte to){

      // Copy the byte[] to a local uint8_t* structure
      jsize num_bytes = env->GetArrayLength(jbuf);
      uint8_t * buffer = (uint8_t *) malloc (num_bytes);
      jbyte  *msg = env->GetByteArrayElements(jbuf, 0);
      memcpy ( buffer , msg , num_bytes ) ;
      env->ReleaseByteArrayElements(jbuf, msg, 0);

      // Now set the FROM header for this EndPoint
      radios[radioHandle].reliableDatagram->setHeaderFrom(from);
      uint8_t ulen = len;
      uint8_t uto = to;
      // Now send the buffer
      jboolean result = radios[radioHandle].reliableDatagram->sendtoWait(buffer, ulen, uto);
      free (buffer);
      return result;
  }


//------------------------------------------------------------------------------------------
JNIEXPORT jlong JNICALL Java_io_mapsmessaging_network_io_impl_lora_device_LoRaDevice_read
  (JNIEnv *env, jobject obj, jint radioHandle, jbyteArray jbuf, jint jlen){
    jlong returnVal =0;
    if (bcm2835_gpio_eds(radios[radioHandle].irq_pin)) {
        bcm2835_gpio_set_eds(radios[radioHandle].irq_pin);
        uint8_t len=RH_RF95_MAX_MESSAGE_LEN;
        if(jlen < RH_RF95_MAX_MESSAGE_LEN){
          len = jlen;
        }
        jbyte* bufferPtr = env->GetByteArrayElements(jbuf, NULL);
        uint8_t from = 0;
        uint8_t to = 0;
        uint8_t id = 0;
        uint8_t flag = 0;

        if(radios[radioHandle].reliableDatagram->recvfromAck((uint8_t*)bufferPtr, &len, &from, &to, &id, &flag)){
            env->SetByteArrayRegion(jbuf, 0, len, bufferPtr);
            int8_t rssi  = radios[radioHandle].rf95->lastRssi();
            jlong jflag = flag;
            jlong jid = id;
            returnVal = ((jflag & 0xff)<<40) +
                        ((jid & 0xff)<<32) +
                        ((to & 0xff)<<24) +
                        ((rssi & 0xff)<<16) +
                        ((from & 0xff) << 8) +
                        (len & 0xff);
        }
        env->ReleaseByteArrayElements(jbuf, bufferPtr, 0);
    }
    return returnVal;
  }

//------------------------------------------------------------------------------------------
JNIEXPORT jint JNICALL Java_io_mapsmessaging_network_io_impl_lora_device_LoRaDevice_available
  (JNIEnv *env, jobject obj, jint radioHandle){
    return bcm2835_gpio_eds(radios[radioHandle].irq_pin);
  }

//------------------------------------------------------------------------------------------
JNIEXPORT void JNICALL Java_io_mapsmessaging_network_io_impl_lora_device_LoRaDevice_setPromiscuous
    (JNIEnv *enf, jobject obj, jint radioHandle, jboolean flag){
        radios[radioHandle].rf95->setPromiscuous(flag);
    }


//------------------------------------------------------------------------------------------
JNIEXPORT jint JNICALL Java_io_mapsmessaging_network_io_impl_lora_device_LoRaDevice_getPacketSize
  (JNIEnv *env, jobject obj, jint radioHandle){
    return radios[radioHandle].rf95->maxMessageLength( );
  }
