javac *.java
if %ERRORLEVEL% GTR 0 goto end

java LaunchGame
del /s /q /f *.class
