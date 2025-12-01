@echo off
echo ============================================================
echo STARTING 5 DISTRIBUTED NODES (Ricart-Agrawala demo)...
echo ============================================================

REM ==== 1. START 5 NODES IN 5 TERMINALS ====
start "NODE 8080" cmd /k "java -jar target/agrawala-1.0-SNAPSHOT-jar-with-dependencies.jar 8080 "" "
timeout /t 1 >nul

start "NODE 8081" cmd /k "java -jar target/agrawala-1.0-SNAPSHOT-jar-with-dependencies.jar 8081 http://localhost:8080"
start "NODE 8082" cmd /k "java -jar target/agrawala-1.0-SNAPSHOT-jar-with-dependencies.jar 8082 http://localhost:8080"
start "NODE 8083" cmd /k "java -jar target/agrawala-1.0-SNAPSHOT-jar-with-dependencies.jar 8083 http://localhost:8080"
start "NODE 8084" cmd /k "java -jar target/agrawala-1.0-SNAPSHOT-jar-with-dependencies.jar 8084 http://localhost:8080"

echo Waiting for nodes to boot...
timeout /t 3 >nul


REM ==== 2. FULL MESH TOPOLOGY ====
echo Creating complete mesh topology...

curl -s -X POST http://localhost:8080/join -d "http://localhost:8081"
curl -s -X POST http://localhost:8080/join -d "http://localhost:8082"
curl -s -X POST http://localhost:8080/join -d "http://localhost:8083"
curl -s -X POST http://localhost:8080/join -d "http://localhost:8084"

curl -s -X POST http://localhost:8081/join -d "http://localhost:8082"
curl -s -X POST http://localhost:8081/join -d "http://localhost:8083"
curl -s -X POST http://localhost:8081/join -d "http://localhost:8084"

curl -s -X POST http://localhost:8082/join -d "http://localhost:8083"
curl -s -X POST http://localhost:8082/join -d "http://localhost:8084"

curl -s -X POST http://localhost:8083/join -d "http://localhost:8084"


echo Topology OK.
timeout /t 2 >nul

exit /b 0
