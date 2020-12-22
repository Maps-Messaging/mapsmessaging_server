echo off
echo "===================================================="
echo "Please note: These are self signed certicates and"
echo "not intended for production use but only local tests"
echo "===================================================="


del *.jks
del *.pem

echo "===================================================="
echo "Creating fake third-party chain root -> ca"
echo "===================================================="

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
