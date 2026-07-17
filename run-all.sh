#!/bin/bash

echo "Starting microservices..."

# Capture the absolute path of the root directory where the parent ./mvnw lives
ROOT_DIR=$(pwd)

# Enforce Java 17 path for your environment
export JAVA_HOME="/usr/lib/jvm/java-17-openjdk-amd64"
export PATH="$JAVA_HOME/bin:$PATH"

# Load environment variables from .env file if it exists
if [ -f .env ]; then
    export $(cat .env | grep -v '#' | xargs)
fi

# Make sure the parent Maven wrapper is executable
chmod +x ./mvnw

echo "Starting databases and networks..."
# chmod +x ./scripts/db/db-start.sh
# chmod +x ./scripts/kafka/kafka_init.sh

# Start databases & custom networks defined in docker-compose
docker compose up -d
sleep 5

# Create logs directory
mkdir -p logs

# Function to run a service using the parent wrapper
run_service () {
    SERVICE_NAME=$1
    SERVICE_DIR=$2

    echo "-----------------------------------"
    echo "Starting $SERVICE_NAME..."

    # Run the parent wrapper from ROOT_DIR, targeting the specific module (-pl)
    # -B runs in batch mode to keep your log files clean from download bars
    nohup "$ROOT_DIR/mvnw" -pl "$SERVICE_DIR" clean spring-boot:run -B > "$ROOT_DIR/logs/$SERVICE_NAME.log" 2>&1 &

    echo "$SERVICE_NAME started with PID $!"
    echo "----------------------------------"
}

# 1. Start Eureka Server first
run_service "eureka-server" "eureka-server"
sleep 5

# 2. Start Gateway
run_service "gateway" "gateway"
sleep 5

# 3. Start User Service
run_service "user-service" "user-service"
sleep 5

# 4. Start Product Service
run_service "product-service" "product-service"
sleep 5

# 5. Start Media Service
run_service "media-service" "media-service"
sleep 5

run_service "order-service" "order-service"
sleep 5

echo "All services started!"
echo "Check logs in /logs folder"