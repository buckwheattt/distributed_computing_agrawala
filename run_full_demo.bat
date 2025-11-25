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


REM ==== 3. ENTER CS FROM ALL NODES (SEQUENTIAL REQUESTS) ====
echo Testing Ricart-Agrawala ordering with sequential CS requests...

echo [STEP] Node 8080 enterCS
curl -s -X POST http://localhost:8080/enterCS
timeout /t 2 >nul

echo [STEP] Node 8081 enterCS
curl -s -X POST http://localhost:8081/enterCS
timeout /t 2 >nul

echo [STEP] Node 8082 enterCS
curl -s -X POST http://localhost:8082/enterCS
timeout /t 2 >nul

echo [STEP] Node 8083 enterCS
curl -s -X POST http://localhost:8083/enterCS
timeout /t 2 >nul

echo [STEP] Node 8084 enterCS
curl -s -X POST http://localhost:8084/enterCS
timeout /t 2 >nul

echo Waiting for all nodes to finish their CS work...
timeout /t 5 >nul


REM ==== 4. SIMULATE SLOW COMMUNICATION ON 8082 ====
echo Simulating slow network on node 8082...

curl -s -X POST http://localhost:8082/setDelay -d "2000"

echo [STEP] Node 8080 enterCS (normal)
curl -s -X POST http://localhost:8080/enterCS
timeout /t 1 >nul

echo [STEP] Node 8081 enterCS (normal)
curl -s -X POST http://localhost:8081/enterCS
timeout /t 1 >nul

echo [STEP] Node 8082 enterCS (SLOW)
curl -s -X POST http://localhost:8082/enterCS

echo Waiting while slow node 8082 finishes...
timeout /t 12 >nul

curl -s -X POST http://localhost:8082/setDelay -d "0"
echo Delay on node 8082 reset to 0 ms.
timeout /t 2 >nul


REM ==== 5. KILL NODE 8083 (ISOLATE IT) ====
echo Killing node 8083 (mark as dead on others)...

curl -s -X POST http://localhost:8081/kill -d "http://localhost:8083"
curl -s -X POST http://localhost:8082/kill -d "http://localhost:8083"
curl -s -X POST http://localhost:8084/kill -d "http://localhost:8083"

echo Node 8083 isolated (others will not send messages to it).
timeout /t 2 >nul

echo [STEP] Node 8080 enterCS after kill(8083)
curl -s -X POST http://localhost:8080/enterCS
timeout /t 5 >nul


REM ==== 6. REVIVE NODE 8083 ====
echo Reviving node 8083...

curl -s -X POST http://localhost:8081/revive -d "http://localhost:8083"
curl -s -X POST http://localhost:8082/revive -d "http://localhost:8083"
curl -s -X POST http://localhost:8084/revive -d "http://localhost:8083"

timeout /t 2 >nul

curl -s -X POST http://localhost:8083/join -d "http://localhost:8080"
curl -s -X POST http://localhost:8083/join -d "http://localhost:8081"
curl -s -X POST http://localhost:8083/join -d "http://localhost:8082"
curl -s -X POST http://localhost:8083/join -d "http://localhost:8084"

echo [STEP] Node 8083 enterCS after revive()
curl -s -X POST http://localhost:8083/enterCS
timeout /t 5 >nul


REM ==== 7. DONE ====
echo ============================================================
echo TEST FINISHED — CHECK LOG FILES logs_node-*.txt
echo ============================================================

exit /b 0
