echo "waiting for kafka connect to start listening..."
while [ $(curl -s -o /dev/null -w %{http_code} http://connect:8083/connectors) -eq 000 ] ; do 
    echo -e $(date) " kafka connect listener HTTP state: " $(curl -s -o /dev/null -w %{http_code} http://connect:8083/connectors) " (waiting for 200)"
    sleep 1
done
echo "kafka connect is up!"
sleep 5
kcctl config set-context default --cluster=http://connect:8083 && kcctl apply -f /home/create_csv_file_source_connector.json
sleep 10
kcctl config set-context default --cluster=http://connect:8083 && kcctl apply -f /home/create_http_sink_batched_connector.json
echo "done registering connectors"
