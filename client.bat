javac *.java

@echo off
if %ERRORLEVEL% GTR 0 end
if "%1" == "" goto skip
if "%2" == "" goto skip
if %2 LEQ 1 goto skip

@echo on
FOR /L %%G IN (2,1,%2) DO @start /b java ClientGame /c %1

:skip
@echo on
java ClientGame /c %1

:del
del /s /q /f *.class

:end
@echo off