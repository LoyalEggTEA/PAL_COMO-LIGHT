@echo off
set "JAVA_EXE=C:\Program Files\Java\jdk-17\bin\java.exe"
if "%SAMBANOVA_API_KEY%"=="" (
    echo SAMBANOVA_API_KEY is not set, so Palcom will use fallback canned replies.
    echo Run setup_sambanova_key.bat if you want AI-powered replies.
    echo.
)
"%JAVA_EXE%" -jar PalcomAssistant.jar
pause
