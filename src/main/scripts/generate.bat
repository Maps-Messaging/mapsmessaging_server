::  Copyright [ 2020 - 2024 ] Matthew Buckton
::  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
::
::  Licensed under the Apache License, Version 2.0 with the Commons Clause
::  (the "License"); you may not use this file except in compliance with the License.
::  You may obtain a copy of the License at:
::
::      http://www.apache.org/licenses/LICENSE-2.0
::      https://commonsclause.com/
::
::  Unless required by applicable law or agreed to in writing, software
::  distributed under the License is distributed on an "AS IS" BASIS,
::  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
::  See the License for the specific language governing permissions and
::  limitations under the License.
::

echo on
echo "===================================================="
echo "Please note: These are self signed certicates and"
echo "not intended for production use but only local tests"
echo "===================================================="


del *.jks
del *.pem

echo "===================================================="
echo "Creating fake third-party chain root -> ca"
echo "===================================================="

set JAVA_HOME=E:\java\zulu13.28.11-ca-jdk13.0.1-win_x64
set PATH=%JAVA_HOME%/bin;%PATH%

echo %JAVA_HOME%

REM
REM  generate private keys (for root and ca)
REM 
keytool -storetype JKS  -genkeypair -alias root -dname "cn=Local Network - mapsmessaging.io Testing Only" -validity 10000 -keyalg RSA -keysize 2048 -ext bc:c -keystore root.jks -keypass password -storepass password
keytool -storetype JKS  -genkeypair -alias ca -dname "cn=Local Network - mapsmessaging.io Testing Only" -validity 10000 -keyalg RSA -keysize 2048 -ext bc:c -keystore ca.jks -keypass password -storepass password

REM 
REM  generate root certificate
REM 
keytool -exportcert -rfc -keystore root.jks -alias root -storepass password > root.pem

REM 
REM  generate a certificate for ca signed by root (root -> ca)
REM 
keytool -storetype JKS -keystore ca.jks -storepass password -certreq -alias ca | keytool -keystore root.jks -storepass password -gencert -alias root -ext bc=0 -ext san=dns:ca -rfc > ca.pem

REM 
REM  import ca cert chain into ca.jks
REM 
keytool -keystore ca.jks -storepass password -importcert -trustcacerts -noprompt -alias root -file root.pem
keytool -keystore ca.jks -storepass password -importcert -alias ca -file ca.pem

echo  "===================================================================="
echo  "Fake third-party chain generated. Now generating my-keystore.jks ..."
echo  "===================================================================="

REM 
REM  generate private keys (for server)
REM 
keytool -storetype JKS  -genkeypair -alias server -dname cn=server -validity 10000 -keyalg RSA -keysize 2048 -keystore my-keystore.jks -keypass password -storepass password

REM 
REM  generate a certificate for server signed by ca (root -> ca -> server)
REM 
keytool -storetype JKS -keystore my-keystore.jks -storepass password -certreq -alias server | keytool -keystore ca.jks -storepass password -gencert -alias ca -ext ku:c=dig,keyEnc -ext "san=dns:localhost,ip:127.0.0.1" -ext eku=sa,ca -rfc > server.pem

REM 
REM  import server cert chain into my-keystore.jks
REM 
keytool -keystore my-keystore.jks -storepass password -importcert -trustcacerts -noprompt -alias root -file root.pem
keytool -keystore my-keystore.jks -storepass password -importcert -alias ca -file ca.pem
keytool -keystore my-keystore.jks -storepass password -importcert -alias server -file server.pem

echo "================================================="
echo "Keystore generated. Now generating truststore ..."
echo "================================================="

REM 
REM  import server cert chain into my-truststore.jks
REM 
keytool -keystore my-truststore.jks -storepass password -importcert -trustcacerts -noprompt -alias root -file root.pem
keytool -keystore my-truststore.jks -storepass password -importcert -alias ca -file ca.pem
keytool -keystore my-truststore.jks -storepass password -importcert -alias server -file server.pem
