@echo off
REM
REM  This script downloads Smile ML libraries (GPL-3.0 licensed) from Maven Central.
REM  You are responsible for complying with the Smile license (https://github.com/haifengl/smile).
REM  MapsMessaging does not distribute Smile or bundle it directly.
REM
REM  Copyright [ 2020 - 2024 ] Matthew Buckton
REM  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
REM
REM  Licensed under the Apache License, Version 2.0 with the Commons Clause
REM  http://www.apache.org/licenses/LICENSE-2.0
REM  https://commonsclause.com/
REM

SETLOCAL ENABLEDELAYEDEXPANSION

REM Determine current dir and parent if run from \bin
SET "CURRENT_DIR=%CD%"
FOR %%I IN ("%CURRENT_DIR%") DO SET "PARENT_DIR=%%~dpI"
IF /I "%CURRENT_DIR:~-4%"=="\bin" (
    SET "MAPS_HOME=%PARENT_DIR:~0,-5%"
) ELSE (
    SET "MAPS_HOME=%CURRENT_DIR%"
)

REM Allow override
IF NOT DEFINED MAPS_HOME (
    SET "MAPS_HOME=%CD%"
)

SET "VERSION=4.3.0"
SET "BASE_URL=https://repo1.maven.org/maven2"
SET "GROUP_ID=dev/smile"
SET "TARGET_DIR=%MAPS_HOME%\lib"

IF NOT EXIST "%TARGET_DIR%" (
    mkdir "%TARGET_DIR%"
)

ECHO Downloading Smile %VERSION% JARs...

SET MODULES=smile-core smile-data smile-math smile-graph smile-plot

FOR %%M IN (%MODULES%) DO (
    SET "MODULE=%%M"
    SET "FILE=!MODULE!-%VERSION%.jar"
    SET "URL=%BASE_URL%/%GROUP_ID%/!MODULE!/%VERSION%/!FILE!"
    ECHO -> !FILE!
    curl -sSL "!URL!" -o "%TARGET_DIR%\!FILE!"
)

ECHO All Smile JARs downloaded to %TARGET_DIR%

ENDLOCAL
