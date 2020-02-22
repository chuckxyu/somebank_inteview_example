# Coding Challenge - Authorizer

## Description

**OperationAuthorizer.zip** is the compressed file containing a maven project which at his resources presents the necessary artifacts to *dockerize* the build and the execution environment
to test with input from stdin.

![Java Project](https://i.imgur.com/ULUbBqJ.jpg)


## Instructions

 1. Download **OperationAuthorizer.zip**
 2. Extract files from **OperationAuthorizer.zip**
 3. Go to resources directory project and into Docker:
	 `cd src/main/resources/Docker`
 4. Run the script file **runDockerCompose.sh** and wait for it to be finish
	 `sh runDockerCompose.sh`
 5. test!

*see the video:
[https://youtu.be/Nyu9sXy65oo](https://youtu.be/Nyu9sXy65oo)


## Design
![First notes on the requirements and design decisions](https://i.imgur.com/qHqx6VX.jpg)

As I usually do with any task that involves order, results or learning, I wrote some hints to help me understand what I have to accomplish.

At first I wrote down statements about the solution as the InMemory structure required and the operations to be authorized and some details about inputs and outputs to imagine whats the behavior for data.

I sketched a DB (listed: redis) that represents the InMemory requirement and something that I have to build to get and set Operations from (listed: Java, spring-boot, scala) 

On later reading, I decided to go with full Java 8, in order to get a basic working program to read from stdin and process it, also to keep it simple.

The build tool I use for this is maven because its simplicity and the dependencies are easily managed.

I also decide to include as an option Hazelcast due to it scability: 

Hazelcast IMDG supports unlimited number of maps and caches per cluster.

I marked that last part in blue as Client, as that's the concept to be coded.

Also in blue put some lines marking the sketch with the different componentes needed:

 - InMemory db "server"
 - Authorizer: the use of a client to validate and persist operations at
   the InMemory DB.

As final part of the notes, I decide to make a list of models to serialize and manage the operations, but then decided to keep it to Strings and Json objects.

From this I started creating the following flow using docker-compose with hazelcast image as inmemory backend and a basic maven project to test client capabilities:


```mermaid
sequenceDiagram
Authorizer main() ->> Client: new Client()
Client-->>Client: INIT CLIENT CONFIG
Authorizer main()-->> Client: Clear()
Client-->>Hazelcast: CLEAR MAP
Authorizer main()-->>Authorizer main(): READS STDIN
Authorizer main()-->>Client:setOperation(stdin)
Client -->>Client:PROCESS
Client-->>Hazelcast: SET DATA
Client-->>Authorizer main(): RESULT
```
After testing with inputs from the original spec, I include the application to the compose definition creating a docker image that uses maven and java from public images to build and run inside the same docker env.
