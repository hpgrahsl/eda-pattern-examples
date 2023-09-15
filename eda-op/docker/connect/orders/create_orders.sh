echo "waiting order service to start listening..."
while [ $(curl -s -o /dev/null -w %{http_code} http://eda-op-order-service:8080/actuator/health) -eq 000 ] ; do 
    echo -e $(date) " order service listener HTTP state: " $(curl -s -o /dev/null -w %{http_code} http://eda-op-order-service:8080/actuator/health) " (waiting for 200)"
    sleep 2
done
echo "order service is up!"
sleep 5
curl -X POST -H "Content-Type: application/json" -d @/home/order1.json http://eda-op-order-service:8080/api/orders
sleep 1
curl -X POST -H "Content-Type: application/json" -d @/home/order2.json http://eda-op-order-service:8080/api/orders
sleep 1
curl -X PUT -H "Content-Type: application/json" -d @/home/order1_update1.json http://eda-op-order-service:8080/api/orders/1/lines/1
sleep 1
curl -X PUT -H "Content-Type: application/json" -d @/home/order1_update2.json http://eda-op-order-service:8080/api/orders/1/lines/2
sleep 1
curl -X PUT -H "Content-Type: application/json" -d @/home/order2_update1.json http://eda-op-order-service:8080/api/orders/2/lines/3
sleep 1
curl -X PUT -H "Content-Type: application/json" -d @/home/order2_update2.json http://eda-op-order-service:8080/api/orders/2/lines/4
sleep 1
curl -X PUT -H "Content-Type: application/json" -d @/home/order2_update3.json http://eda-op-order-service:8080/api/orders/2/lines/5
echo "DONE with creating/updating orders!"
