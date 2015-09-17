javac *.java
if %ERRORLEVEL% GTR 0 goto end

java Game
del /s /q /f *.class

:end
@echo off