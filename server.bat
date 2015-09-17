javac *.java

@echo off
if %ERRORLEVEL% GTR 0 goto end
if "%1%" == "" goto skip

@echo on
java ServerGame /c %1%
goto dele

:skip
java ServerGame /c 2

:dele
del /s /q /f *.class

:end
@echo off