echo "waiting for kafka connect to start listening..."
while [ $(curl -s -o /dev/null -w %{http_code} http://connect:8083/connectors) -eq 000 ] ; do 
    echo -e $(date) " kafka connect listener HTTP state: " $(curl -s -o /dev/null -w %{http_code} http://connect:8083/connectors) " (waiting for 200)"
    sleep 2
done
echo "kafka connect is up!"
sleep 5
curl -X POST -H "Content-Type: application/json" -d @/home/create_mysql_dbz_source_connector.json http://connect:8083/connectors
echo "done registering connectors"
