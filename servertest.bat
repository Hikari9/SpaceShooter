javac *.java

if %ERRORLEVEL% GTR 0 goto end
if "%1" == "" goto skip

@echo on
FOR /L %%G IN (2,1,%1) DO @start /b java ClientGame
java ServerGame /c %1
goto dele

:skip
@start /b java ClientGame
java ServerGame /c 2

:dele
REM del /s /q /f *.class

:end