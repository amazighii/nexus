#!/bin/bash

echo "Stopping all services..."

pkill -f spring-boot:run

sleep 1

echo "-----------------------------------"

echo "Stopping MongoDB container..."

# Stop MongoDB container

docker compose down 
# chmod +x ./scripts/db/db-stop.sh
# ./scripts/db/db-stop.sh

# Stop Kafka and Zookeeper
# chmod +x ./scripts/kafka/kafka_stop.sh
# ./scripts/kafka/kafka_stop.sh


sleep 1
echo "docker stop command executed."
echo "-----------------------------------"


echo "Cleaning up logs..."

if [ -d "./logs" ] && [ -n "$(ls -A ./logs)" ]; then
    echo "Logs directory exists. Removing log files..."
    rm ./logs/*.log
else
    echo "Logs directory does not exist. Creating logs directory..."
    mkdir -p ./logs
fi

echo "-----------------------------------"

echo "All services stopped."
