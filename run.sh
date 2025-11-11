#!/bin/bash

# Heimdall Build and Run Script

echo "🛡️  Heimdall - Build and Run Script"
echo "===================================="

# Check if Java is installed
if ! command -v java &> /dev/null; then
    echo "❌ Java is not installed. Please install Java 17 or higher."
    exit 1
fi

# Check Java version
JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo "❌ Java version must be 17 or higher. Current version: $JAVA_VERSION"
    exit 1
fi

echo "✅ Java version: $(java -version 2>&1 | head -n 1)"

# Function to start infrastructure
start_infra() {
    echo ""
    echo "🚀 Starting infrastructure services..."
    cd docker
    docker-compose up -d postgres kafka zookeeper elasticsearch redis
    cd ..
    echo "✅ Infrastructure services started"
    echo ""
    echo "Waiting for services to be ready..."
    sleep 10
}

# Function to build application
build_app() {
    echo ""
    echo "🔨 Building application..."
    ./gradlew clean build -x test
    if [ $? -eq 0 ]; then
        echo "✅ Build successful"
    else
        echo "❌ Build failed"
        exit 1
    fi
}

# Function to run application
run_app() {
    echo ""
    echo "🚀 Starting Heimdall application..."
    ./gradlew bootRun --args='--spring.profiles.active=dev'
}

# Main menu
echo ""
echo "Select an option:"
echo "1) Start infrastructure only"
echo "2) Build application"
echo "3) Build and run application"
echo "4) Run application (already built)"
echo "5) Full setup (infra + build + run)"
echo ""
read -p "Enter option (1-5): " option

case $option in
    1)
        start_infra
        ;;
    2)
        build_app
        ;;
    3)
        build_app
        run_app
        ;;
    4)
        run_app
        ;;
    5)
        start_infra
        build_app
        run_app
        ;;
    *)
        echo "❌ Invalid option"
        exit 1
        ;;
esac
