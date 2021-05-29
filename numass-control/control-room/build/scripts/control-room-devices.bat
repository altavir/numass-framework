@if "%DEBUG%" == "" @echo off
@rem ##########################################################################
@rem
@rem  control-room-devices startup script for Windows
@rem
@rem ##########################################################################

@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%..

@rem Add default JVM options here. You can also use JAVA_OPTS and CONTROL_ROOM_DEVICES_OPTS to pass JVM options to this script.
set DEFAULT_JVM_OPTS=

@rem Find java.exe
if defined JAVA_HOME goto findJavaFromJavaHome

set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if "%ERRORLEVEL%" == "0" goto init

echo.
echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%/bin/java.exe

if exist "%JAVA_EXE%" goto init

echo.
echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME%
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:init
@rem Get command-line arguments, handling Windows variants

if not "%OS%" == "Windows_NT" goto win9xME_args

:win9xME_args
@rem Slurp the command line arguments.
set CMD_LINE_ARGS=
set _SKIP=2

:win9xME_args_slurp
if "x%~1" == "x" goto execute

set CMD_LINE_ARGS=%*

:execute
@rem Setup the command line

set CLASSPATH=%APP_HOME%\lib\control-room-0.3.0.jar;%APP_HOME%\lib\cryotemp-0.2.0.jar;%APP_HOME%\lib\msp-0.4.0.jar;%APP_HOME%\lib\vac-0.5.0.jar;%APP_HOME%\lib\numass-control-1.0.0.jar;%APP_HOME%\lib\numass-server-1.0.0.jar;%APP_HOME%\lib\numass-client-1.0.0.jar;%APP_HOME%\lib\plots-jfc-0.4.0 - SNAPSHOT.jar;%APP_HOME%\lib\dataforge-control-0.4.0 - SNAPSHOT.jar;%APP_HOME%\lib\dataforge-gui-0.4.0 - SNAPSHOT.jar;%APP_HOME%\lib\numass-core-1.0.0.jar;%APP_HOME%\lib\storage-server-0.4.0 - SNAPSHOT.jar;%APP_HOME%\lib\dataforge-messages-0.4.0 - SNAPSHOT.jar;%APP_HOME%\lib\dataforge-plots-0.4.0 - SNAPSHOT.jar;%APP_HOME%\lib\dataforge-storage-0.4.0 - SNAPSHOT.jar;%APP_HOME%\lib\dataforge-json-0.4.0 - SNAPSHOT.jar;%APP_HOME%\lib\dataforge-core-0.4.0 - SNAPSHOT.jar;%APP_HOME%\lib\tornadofx-controlsfx-0.1.jar;%APP_HOME%\lib\tornadofx-1.7.15.jar;%APP_HOME%\lib\kotlin-stdlib-jdk8-1.2.50.jar;%APP_HOME%\lib\kotlin-reflect-1.2.50.jar;%APP_HOME%\lib\kotlin-stdlib-jdk7-1.2.50.jar;%APP_HOME%\lib\kotlinx-coroutines-jdk8-0.22.jar;%APP_HOME%\lib\kotlinx-coroutines-core-0.22.jar;%APP_HOME%\lib\kotlin-stdlib-1.2.50.jar;%APP_HOME%\lib\ratpack-core-1.4.6.jar;%APP_HOME%\lib\commons-daemon-1.1.0.jar;%APP_HOME%\lib\kotlin-stdlib-common-1.2.50.jar;%APP_HOME%\lib\annotations-15.0.jar;%APP_HOME%\lib\commons-cli-1.4.jar;%APP_HOME%\lib\zt-zip-1.13.jar;%APP_HOME%\lib\jfreesvg-3.3.jar;%APP_HOME%\lib\jfreechart-fx-1.0.1.jar;%APP_HOME%\lib\jssc-2.8.0.jar;%APP_HOME%\lib\controlsfx-8.40.14.jar;%APP_HOME%\lib\richtextfx-0.9.0.jar;%APP_HOME%\lib\netty-codec-http-4.1.6.Final.jar;%APP_HOME%\lib\netty-handler-4.1.6.Final.jar;%APP_HOME%\lib\netty-transport-native-epoll-4.1.6.Final-linux-x86_64.jar;%APP_HOME%\lib\guava-19.0.jar;%APP_HOME%\lib\logback-classic-1.2.3.jar;%APP_HOME%\lib\jcl-over-slf4j-1.7.25.jar;%APP_HOME%\lib\slf4j-api-1.7.25.jar;%APP_HOME%\lib\reactive-streams-1.0.0.final.jar;%APP_HOME%\lib\caffeine-2.3.1.jar;%APP_HOME%\lib\javassist-3.19.0-GA.jar;%APP_HOME%\lib\jackson-datatype-guava-2.7.5.jar;%APP_HOME%\lib\jackson-datatype-jdk8-2.7.5.jar;%APP_HOME%\lib\jackson-datatype-jsr310-2.7.5.jar;%APP_HOME%\lib\jackson-databind-2.7.5.jar;%APP_HOME%\lib\jackson-dataformat-yaml-2.7.5.jar;%APP_HOME%\lib\snakeyaml-1.15.jar;%APP_HOME%\lib\protobuf-java-3.5.0.jar;%APP_HOME%\lib\sftp-fs-1.1.3.jar;%APP_HOME%\lib\freemarker-2.3.26-incubating.jar;%APP_HOME%\lib\jfreechart-1.5.0.jar;%APP_HOME%\lib\fxgraphics2d-1.6.jar;%APP_HOME%\lib\cache-api-1.0.0.jar;%APP_HOME%\lib\commons-io-2.5.jar;%APP_HOME%\lib\javax.json-1.1.2.jar;%APP_HOME%\lib\undofx-2.0.0.jar;%APP_HOME%\lib\flowless-0.6.jar;%APP_HOME%\lib\reactfx-2.0-M5.jar;%APP_HOME%\lib\wellbehavedfx-0.3.3.jar;%APP_HOME%\lib\netty-codec-4.1.6.Final.jar;%APP_HOME%\lib\netty-transport-4.1.6.Final.jar;%APP_HOME%\lib\netty-buffer-4.1.6.Final.jar;%APP_HOME%\lib\netty-resolver-4.1.6.Final.jar;%APP_HOME%\lib\netty-common-4.1.6.Final.jar;%APP_HOME%\lib\jackson-annotations-2.7.0.jar;%APP_HOME%\lib\jackson-core-2.7.5.jar;%APP_HOME%\lib\fs-core-1.2.jar;%APP_HOME%\lib\jsch-0.1.54.jar;%APP_HOME%\lib\logback-core-1.2.3.jar;%APP_HOME%\lib\json-simple-3.0.2.jar;%APP_HOME%\lib\javax.json-api-1.1.2.jar

@rem Execute control-room-devices
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %CONTROL_ROOM_DEVICES_OPTS%  -classpath "%CLASSPATH%" inr.numass.control.ServerApp %CMD_LINE_ARGS%

:end
@rem End local scope for the variables with windows NT shell
if "%ERRORLEVEL%"=="0" goto mainEnd

:fail
rem Set variable CONTROL_ROOM_DEVICES_EXIT_CONSOLE if you need the _script_ return code instead of
rem the _cmd.exe /c_ return code!
if  not "" == "%CONTROL_ROOM_DEVICES_EXIT_CONSOLE%" exit 1
exit /b 1

:mainEnd
if "%OS%"=="Windows_NT" endlocal

:omega
