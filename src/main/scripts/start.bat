::
:: Copyright [ 2020 - 2024 ] [Matthew Buckton]
:: Copyright [ 2024 - 2025 ] [Maps Messaging B.V.]
::
::  Licensed under the Apache License, Version 2.0 (the "License");
::  you may not use this file except in compliance with the License.
::  You may obtain a copy of the License at
::
::      http://www.apache.org/licenses/LICENSE-2.0
::
:: Unless required by applicable law or agreed to in writing, software
:: distributed under the License is distributed on an "AS IS" BASIS,
:: WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
:: See the License for the specific language governing permissions and
:: limitations under the License.
::
@echo off
setlocal

:: Define the home directory for the messaging daemon
set "VERSION=%%MAPS_VERSION%%"

if "%CD%"=="%~dp0" (
    set "parent_dir=%~dp0.."
) else (
    set "parent_dir=%CD%"
)

if "%MAPS_HOME%"=="" (
    set "MAPS_HOME=%parent_dir%"
)

if "%MAPS_DATA%"=="" (
    set "MAPS_DATA=%MAPS_HOME%\data"
)

:: Check if FLY_CONSUL_URL is set and not empty
if not "%FLY_CONSUL_URL%"=="" (
    :: If FLY_CONSUL_URL is set, use its value for CONSUL_URL
    set "CONSUL_URL=%FLY_CONSUL_URL%"
) else (
    :: If FLY_CONSUL_URL is not set, use the default value
    set "CONSUL_URL=http://127.0.0.1/"
)

echo Maps Home is set to '%MAPS_HOME%'
echo Maps Data is set to '%MAPS_DATA%'

set "MAPS_LIB=%MAPS_HOME%\lib"
set "MAPS_CONF=%MAPS_HOME%\conf"

:: From there configure all the paths.
:: Note::: The conf directory must be at the start else the configuration is loaded from the jars
set "CLASSPATH=%MAPS_CONF%;%MAPS_LIB%\maps-%VERSION%.jar;%MAPS_LIB%\*"

:loop
:: Now start the daemon
java   -Xss256k ^
    -classpath %CLASSPATH% %JAVA_OPTS% ^
    -DUSE_UUID=false ^
    -DConsulUrl="%CONSUL_URL%" ^
    -DConsulPath="%CONSUL_PATH%" ^
    -DConsulToken="%CONSUL_TOKEN%" ^
    -Djava.security.auth.login.config="%MAPS_CONF%\jaasAuth.config" ^
    -DMAPS_HOME="%MAPS_HOME%" ^
    io.mapsmessaging.MessageDaemon


:: Check the exit code from Java application
if %ERRORLEVEL% EQU 8 (
    echo Restarting the server...
    goto loop
) else (
    echo Exiting with code %ERRORLEVEL%
    endlocal
    exit /b %ERRORLEVEL%
)

