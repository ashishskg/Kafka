# Start Multiple Kafka Server Till Command (./bin/kafka-server-start.sh config/server-3.properties)
cd ~/Desktop/Tools/kafka
./bin/kafka-server-stop.sh
rm -rf /tmp/server-1 /tmp/server-2 /tmp/server-3
export CLUSTER_ID=$(./bin/kafka-storage.sh random-uuid)
echo $CLUSTER_ID
DIR1=$(./bin/kafka-storage.sh random-uuid)
DIR2=$(./bin/kafka-storage.sh random-uuid)
DIR3=$(./bin/kafka-storage.sh random-uuid)
echo $DIR1 $DIR2 $DIR3

 # Format the storage directories for each broker with the cluster ID and initial controller quorum
./bin/kafka-storage.sh format -t "$CLUSTER_ID" -c config/server-1.properties --initial-controllers "1@localhost:9093:$DIR1,2@localhost:9095:$DIR2,3@localhost:9097:$DIR3"
./bin/kafka-storage.sh format -t "$CLUSTER_ID" -c config/server-2.properties --initial-controllers "1@localhost:9093:$DIR1,2@localhost:9095:$DIR2,3@localhost:9097:$DIR3"
./bin/kafka-storage.sh format -t "$CLUSTER_ID" -c config/server-3.properties --initial-controllers "1@localhost:9093:$DIR1,2@localhost:9095:$DIR2,3@localhost:9097:$DIR3"
 