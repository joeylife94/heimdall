@echo off
REM Heimdall Build and Run Script for Windows

echo ========================================
echo Heimdall - Build and Run Script
echo ========================================

REM Check if Java is installed
java -version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Java is not installed. Please install Java 17 or higher.
    exit /b 1
)

echo [INFO] Java version check passed

echo.
echo Select an option:
echo 1) Start infrastructure only
echo 2) Build application
echo 3) Build and run application
echo 4) Run application (already built)
echo 5) Full setup (infra + build + run)
echo.

set /p option="Enter option (1-5): "

if "%option%"=="1" goto start_infra
if "%option%"=="2" goto build_app
if "%option%"=="3" goto build_and_run
if "%option%"=="4" goto run_app
if "%option%"=="5" goto full_setup
goto invalid_option

:start_infra
echo.
echo [INFO] Starting infrastructure services...
cd docker
docker-compose up -d postgres kafka zookeeper elasticsearch redis
cd ..
echo [SUCCESS] Infrastructure services started
goto end

:build_app
echo.
echo [INFO] Building application...
gradlew.bat clean build -x test
if errorlevel 1 (
    echo [ERROR] Build failed
    exit /b 1
)
echo [SUCCESS] Build successful
goto end

:build_and_run
call :build_app
if errorlevel 1 exit /b 1
call :run_app
goto end

:run_app
echo.
echo [INFO] Starting Heimdall application...
gradlew.bat bootRun --args="--spring.profiles.active=dev"
goto end

:full_setup
call :start_infra
timeout /t 10 /nobreak
call :build_app
if errorlevel 1 exit /b 1
call :run_app
goto end

:invalid_option
echo [ERROR] Invalid option
exit /b 1

:end
echo.
echo [INFO] Script completed
