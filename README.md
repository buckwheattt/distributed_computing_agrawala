## Distributed Shared Variable Access (DSVA) – Ricart–Agrawala Algorithm
### Overview  
This project implements a distributed system with a shared variable and mutual exclusion using the Ricart–Agrawala algorithm. Each node runs as a separate Java process (JAR) and communicates with other nodes via REST API. 
The system demonstrates correct exclusive access to a critical section in a distributed environment using logical clocks (Lamport timestamps) and message-based coordination.

### Key Features
Distributed mutual exclusion (Ricart–Agrawala)  
Shared variable synchronization across nodes  
REST-based inter-node communication  
Dynamic topology management (join/leave/revive)  
Failure simulation (kill command)  
Lamport logical clocks  
Multiple nodes running on different machines/VMs

### System Architecture

Each node:  
Runs as an independent Java JAR process  
Has its own IP address and port  
Maintains a list of peers  
Communicates using REST endpoints:
```bash
/request

/reply

/join

/leave

/revive

/syncSharedValue
```
Nodes must receive replies from all peers before entering the critical section.

### Requirements  
Host OS: Windows (recommended)  
VirtualBox (2 virtual machines: Debian/Ubuntu Linux amd64)  
Java 17 installed on each node  
Application JAR file (e.g., node.jar)

### Network Setup
Each virtual machine should have:  
Adapter 1: NAT (Internet access)  
Adapter 2: Host-only Adapter (local node communication)   
Expected IP configuration:  
NAT interface: 10.0.2.x  
Host-only interface: 192.168.56.x

### Installation & Running
1. Install Java (if not installed)
```bash
sudo apt update
sudo apt install openjdk-17-jre -y
java -version
```
2. Transfer the JAR file to VM
```bash
scp node.jar user@192.168.56.x:/home/user/
```

3. Run the nodes
Example setup with 5 nodes:

On Debian VM:
```bash
java -jar node.jar 8084 192.168.56.x ""
```
On Ubuntu VM:
```bash
java -jar node.jar 8083 192.168.56.x ""
```
On Windows (3 nodes):
```bash
java -jar node.jar 8080 192.168.56.1 ""
java -jar node.jar 8081 192.168.56.1 ""
java -jar node.jar 8082 192.168.56.1 ""
```
You can also run:
```bash
run_full_demo.bat
```
to automatically connect the whole topology. 

### How Mutual Exclusion Works
When a node wants to access the shared variable:  
It sends /request to all peers  
Waits for /reply from every node  
Enters the critical section  
Updates the shared variable (+1 or custom write)  
Leaves the critical section and sends deferred replies  
If another node is already in the critical section, the requester must wait based on Lamport timestamps. 

### Console Commands
enter -	Requests access to the critical section (auto read + increment + write)  
enterCS -	Enter critical section manually  
leaveCS -	Leave the critical section  
read - Read the shared variable (without CS)  
write -	Write a custom value to the shared variable (inside CS)  
join - Join the network topology  
leave	- Gracefully disconnect from the system  
revive - Restore communication after leave  
kill - Simulate node crash

### Node State
Each node maintains:  
Its address (host:port)  
List of known peers  
Lamport logical clock  
Critical section state  
Deferred replies queue  
Local copy of the shared variable   

### Dynamic Topology

join — adds a node and synchronizes shared value

leave — graceful disconnect (node stays running but isolated)

revive — reconnects to previously known peers

kill — simulates sudden failure without notification

Other nodes continue functioning even if one node crashes. 

### Educational Purpose  
This project is designed to demonstrate:  
Distributed mutual exclusion  
Fault tolerance in distributed systems  
Logical clocks and ordering  
REST-based node coordination
