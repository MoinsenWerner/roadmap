@if "%DEBUG%"=="" @echo off
@rem ##########################################################################
@rem
@rem  Gradle startup script for Windows
@rem
@rem ##########################################################################

@if "%DIRNAME%"=="" set DIRNAME=%~dp0
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%

@rem Resolve any .lnk link
:findJavaFromJavaHome
set JAVA_EXE=
if exist "%JAVA_HOME%\bin\java.exe" set JAVA_EXE=%JAVA_HOME%\bin\java.exe
if exist "%JAVA_EXE%" goto init

set JAVA_EXE=java.exe
where java.exe >NUL 2>&1
if %ERRORLEVEL% equ 0 goto init

echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
echo.
echo Please set the JAVA_HOME variable in your environment to match the
location of your Java installation.
goto fail

:init
set CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar

set DEFAULT_JVM_OPTS="-Xmx64m" "-Xms64m"

set CMD_LINE_ARGS=
set _SKIP=

:parseargs
if "%1"=="" goto execute
set CMD_LINE_ARGS=%CMD_LINE_ARGS% %1
shift
goto parseargs

:execute
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %GRADLE_OPTS% "-Dorg.gradle.appname=%APP_BASE_NAME%" -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %CMD_LINE_ARGS%

goto end

:fail
exit /b 1

:end
