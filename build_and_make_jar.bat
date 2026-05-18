@echo off
set "JDK_BIN=C:\Program Files\Java\jdk-17\bin"

echo Compiling Java file in Java 8-compatible mode...
"%JDK_BIN%\javac.exe" --release 8 PalcomAssistant.java
if %errorlevel% neq 0 (
    echo Compilation failed. Make sure the JDK is installed at %JDK_BIN%.
    pause
    exit /b
)

echo Creating manifest...
echo Main-Class: PalcomAssistant> manifest.txt

echo Building JAR...
"%JDK_BIN%\jar.exe" cfm PalcomAssistant.jar manifest.txt PalcomAssistant*.class
if %errorlevel% neq 0 (
    echo JAR build failed.
    pause
    exit /b
)

echo Done!
echo PalcomAssistant.jar has been rebuilt in Java 8-compatible mode.
echo To enable AI replies, set SAMBANOVA_API_KEY before running.
pause
