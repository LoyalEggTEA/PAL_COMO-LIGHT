@echo off
echo This saves your SambaNova API key for Palcom.
echo You only need to do this once on this Windows user account.
echo.
set /p SAMBANOVA_API_KEY=Paste your SambaNova API key here: 
if "%SAMBANOVA_API_KEY%"=="" (
    echo No key entered. Nothing was changed.
    pause
    exit /b
)
setx SAMBANOVA_API_KEY "%SAMBANOVA_API_KEY%"
echo.
echo Saved. Close and reopen any Command Prompt windows before rebuilding/running Palcom.
pause
