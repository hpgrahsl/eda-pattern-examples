CREATE DATABASE IF NOT EXISTS iot_db;

CREATE TABLE IF NOT EXISTS iot_db.devices (
  id INT PRIMARY KEY,
  active BOOLEAN,
  latitude DOUBLE,
  longitude DOUBLE,
  brand varchar(64));

INSERT INTO iot_db.devices (id, active, latitude, longitude, brand) VALUES
(1001,true,48.20499918,16.370498518,"Samsara"),
(1002,false,59.91273,10.74609,"Arm"),
(1003,true,45.46427,9.18951,"PTC"),
(1004,true,41.38879,2.15899,"GE Digital"),
(1005,false,48.85341,2.3488,"Verizon"),
(1006,false,51.50853,-0.12574,"Verizon"),
(1007,true,35.7721,-78.63861,"SoluLab"),
(1008,false,37.77493,-122.41942,"Telit"),
(1009,true,40.7166638,-74.0,"Cisco"),
(1010,false,42.35843,-71.05977,"Telit");
