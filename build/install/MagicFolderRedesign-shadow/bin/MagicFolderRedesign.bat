@if "%DEBUG%" == "" @echo off
@rem ##########################################################################
@rem
@rem  MagicFolderRedesign startup script for Windows
@rem
@rem ##########################################################################

@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.

set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%..

@rem Add default JVM options here. You can also use JAVA_OPTS and MAGIC_FOLDER_REDESIGN_OPTS to pass JVM options to this script.
set DEFAULT_JVM_OPTS=

set JAVA_HOME="%APP_HOME%"
set JAVA_EXE=%JAVA_HOME%/bin/java.exe
set JAVA_EXE="%JAVA_EXE:"=%"

if exist %JAVA_EXE% goto init

echo.
echo ERROR: The directory %JAVA_HOME% does not contain a valid Java runtime for your platform.
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

set CLASSPATH="%JAVA_HOME:"=%/lib/*"



@rem Execute MagicFolderRedesign
%JAVA_EXE% %DEFAULT_JVM_OPTS% %CDS_JVM_OPTS% %JAVA_OPTS% %MAGIC_FOLDER_REDESIGN_OPTS%  -classpath %CLASSPATH% com.magicfolder.Main1 %CMD_LINE_ARGS%

:end
@rem End local scope for the variables with windows NT shell
if "%ERRORLEVEL%"=="0" goto mainEnd

:fail
rem Set variable MAGIC_FOLDER_REDESIGN_EXIT_CONSOLE if you need the _script_ return code instead of
rem the _cmd.exe /c_ return code!
if  not "" == "%MAGIC_FOLDER_REDESIGN_EXIT_CONSOLE%" exit 1
exit /b 1

:mainEnd
if "%OS%"=="Windows_NT" endlocal

:omega
