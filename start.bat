@echo off
echo ========================================
echo  Inventory Management System Launcher
echo ========================================
echo.

REM Prompt for API key
set /p GEMINI_API_KEY="Enter your Gemini API Key: "

if "%GEMINI_API_KEY%"=="" (
    echo ERROR: API key cannot be empty!
    pause
    exit /b 1
)

echo.
echo API Key set successfully!
echo Starting application...
echo.

REM Set JAVA_HOME if not already set
if "%JAVA_HOME%"=="" (
    set "JAVA_HOME=C:\Program Files\Java\jdk-24"
    echo JAVA_HOME set to: %JAVA_HOME%
)

echo Starting MySQL check...
echo.

REM Check if MySQL is running (optional - comment out if not needed)
REM sc query MySQL80 | find "RUNNING" >nul
REM if errorlevel 1 (
REM     echo MySQL service is not running!
REM     echo Please start MySQL service first.
REM     pause
REM     exit /b 1
REM )

echo Building application...
echo.
call mvnw.cmd clean install -DskipTests

if errorlevel 1 (
    echo.
    echo Build failed! Please check the errors above.
    pause
    exit /b 1
)

echo.
echo ========================================
echo  Starting Application...
echo ========================================
echo.
echo Application will be available at:
echo   http://localhost:8086
echo.
echo Default credentials:
echo   Username: admin
echo   Password: admin123
echo.
echo Press Ctrl+C to stop the application
echo ========================================
echo.

REM Start the application
call mvnw.cmd spring-boot:run

pause
