@echo off
echo ============================================================
echo STARTING 5 DISTRIBUTED NODES (Ricart-Agrawala demo)
echo ============================================================

REM === 1. START NODES ===

set WIN_IP=192.168.56.1

start "NODE 8080" cmd /k "java -jar target/agrawala.jar 8080 %WIN_IP% """
timeout /t 1 >nul
start "NODE 8081" cmd /k "java -jar target/agrawala.jar 8081 %WIN_IP% """
start "NODE 8082" cmd /k "java -jar target/agrawala.jar 8082 %WIN_IP% """

echo Waiting for nodes to boot...
timeout /t 3 >nul

REM === 2. FULL TOPOLOGY ===

curl -X POST http://%WIN_IP%:8080/join -d "http://%WIN_IP%:8081"
curl -X POST http://%WIN_IP%:8080/join -d "http://%WIN_IP%:8082"
@REM curl -X POST http://%WIN_IP%:8080/join -d "http://192.168.56.102:8084"
@REM curl -X POST http://%WIN_IP%:8080/join -d "http://192.168.56.103:8083"

curl -X POST http://%WIN_IP%:8081/join -d "http://%WIN_IP%:8082"
@REM curl -X POST http://%WIN_IP%:8081/join -d "http://192.168.56.102:8084"
@REM curl -X POST http://%WIN_IP%:8081/join -d "http://192.168.56.103:8083"

@REM curl -X POST http://%WIN_IP%:8082/join -d "http://192.168.56.102:8084"
@REM curl -X POST http://%WIN_IP%:8082/join -d "http://192.168.56.103:8083"

echo Topology OK.
pause

