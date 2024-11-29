# docker-actusrf20
Source + dockerfiles  to build docker images for actus-server and actus-riskserver with rfAPI 2.0
## Overview
This repository provides materials to build and configure a network of docker containers which can simulate the cash flows of financial contracts defined with the ACTUS standard using different risk scenarios - risk modelling environments. The risk scenarios are defined in a risk service which is a separate docker container from the ACTUS server providing the contract type sensitive cashflow simulation logic.  
## Recommended QUICKSTART ACTUS service configuration 
For new ACTUS users wanting to deploy and ACTUS contract simulation service quickly and start experimenting with it, it is recommended for an initial network to include:
*  actus-server-rf20 docker container - with a contract simulation API and contract type specific logic to simulate future cashflows of any contract
*  actus-riskserver-ce docker container - providing an external community-edition riskservice with a risk entity store api and the risk factor 2.0 interface to provide the actus-server-rf20 risk model observation results
*  a mongodb docker container providing persistent storage for risk entities created using the risk entity store api to the actus-riskserver-ce
*  an actus-rshiny-demo docker container providing a reactive demo showing how an actus service can be used to simulate futurecashflows of ACTUS contracts and portfolios under different interest rate risk scenarios 

In this configuration, installing the dockerized mongodb database container to listen on port 27018 will reduce the risk of port collisions with any existing installed mongodb service.

## QUICK START Deployment of an initial ACTUS service using Docker Compose 
### Steps
#### Prerequisites
*   You must have docker (a docker daemon) installed in your environment. Installing Docker Desktop is a convenient way to do this. Documentation on how to install Docker Desktop can be found under: https://docs.docker.com/desktop/ 
*   Ports 8082 8083 3000 and 27018 in your work environment must be available ( i.e. not in use by some other application )
    *  If this quickstart ACTUS deployment is being run a second time - you may need to stop and exit previous processes using these ports      

#### Installation
1.  Navigate to a folder where you want to install your ACTUS service - we will refer to this folder as ACTUS_BASE
2.  Clone this git repository to a local copy in your ACTUS_BASE folder using the command: > git clone https://github.com/fnparr/docker-actus-rf20.git
3.  Navigate to the ACTUS_BASE/docker-actus-rf20 folder
4.  Issue the command: > docker compose -f quickstart-docker-actus-rf20.yml -p quickstart-docker-actus-rf20 up

The teminal where you issued this command will start displaying console scripts of the started containers and network. 

At this point if you have docker desktop installed - you should be able to see in its dashboard:
*    In the images panel, locally copied and saved images for:
     * actus-server-rf20:27018
     *   actus-riskserver-ce:27018
     *   mongodb
*    in the containers panel
     *   a docker-compose network named quickstart-docker-actus-rf20
     *   if you click to expand this - running containers: 
         *    actus-server-rf20:nodb
         *    actus-riskserver-ce:mdb27018
         *    actus-rshiny-demo:b03
         *    mongodb

#### Experiment with the actus-rshiny-demo to see how ACTUS can simulate contract cashflows 
 *(MORE GUIDANCE NEEDED HERE)*

#### Experiment with curl commands requesting ACTUS services from the command line 
The following steps can be used to validate that you have a working QuickStart ACTUS installation. 
If you want to build an application using an ACTUS service to simulate future cashflows of an ACTUS contract defined by you,
using interest rate and market risk scnarios defined by you, the flow through your application is likely to match some part  or subset  
of the of the sequence of validation tests described below. 

1.  Start a new terminal window and navigate to ACTUS_BASE/docker-actus-rf20/actus-riskserver-ce/actus-riskservice/testB
2.  Run the (linux/MacOS) command:   > source putUst5Y_falling.txt
    * this will the actus-riskserver-ce risk data api to insert a new reference index into the persistent mongodb risk store
    * the response from the command should end with the line > Reference Index successfuly created
3.  Run the command:   > curl -v http://localhost:8082/findAllReferenceIndexes
    * this will read back the newly created reference index data from the mongo risk Store and display it
4.  Run the command:   > source l3PAMwRF.txt
    * this will return and display the cashflows generate by the actus service for simulating a contract
    * early in the displayed cashflow the name value pair:  > "status":"Success"
      indicates that contract simulation was successful.
      
If all of the above tests run as expected, you have a working actus-riskserver-ce container, properly configured to use 
persistent storage in a mongodb container and an actus-riskserver. capable of simulating contract. At this point the 
more complete sequence of tests in ACTUS_BASE/docker-actus-rf20/actus-riskserver-ce/actus-riskservice/testB/TestB_script.txt can be run.

## Alternate ACTUS Service configurations
### Using an already installed MongoDb service 
If you have a mongodb database service already installed in your work environment, you may want to use that to persistently save created risk entities 
(i.e. scenarios, reference Indexes, and behavior models). The default is that the mongodb service is installed to listen on port 27017. 

In that case you will want to bring up config1-docker-actus-rf20.yml as your docker compose network.
The config1 network will pull required images which are needed but not available locally from a public docker hub repository and save them in local docker environment.
It is also possible to explicitly download from the public fnparr/ dockerhub registry or build locally the images:
*  fnparr/actus-server-rf20:mdb27017
*  fnparr/actus-riskserver-ce:mdb27017

### The QUICK START Configuration as described above 
If you want to install a dockerized mongodb database to persistently save risk entities created for your actus rf20 environment, it will reduce the risk of port collisions with any existing installed mongodb service to have this project specific mongodb listen on port 27018. 

In that case you will want to bring up config2-docker-actus-rf20 as your docker compose network. 

The config2 network requires that you download from the public fnparr/ dockerhub registry or build locally the images:
*  fnparr/actus-server-rf20:mdb27018
*  fnparr/actus-riskserver-ce:mdb27018

### Downloading required images 
A Prerequisite ( for ALL steps in using docker-actus-rf20) is that you must have docker (a docker daemon) installed in your environment. 
Installing Docker Desktop is a convenient way to do this.

The terminal command:
  > docker pull fnparr/actus-server-rf20:mdb27018
will download and install the specified image.

Alternatively use the search bar in Docker Desktop to locate the image and request pull. 
 
### Creating and starting Docker Compose configurations of docker-actus-rf20 
Steps.
1. You must have Docker Desktop or some othe docker daemon installed in your environment
2. Clone this git repository to your local environment.
3. Download or build the required images - as listed above- to make them available locally
   * downloading using docker pull comands is simpler
   * building in your local environment is described below
4. Go into  the docker-actus-rf20 folder
   * For config1:  > docker compose -f config1-docker-actus-rf20.yml -p config1-docker-actus-rf20 up
   * For config2:  > docker compose -f config2-docker-actus-rf20.yml -p config2-docker-actus-rf20 up

### View, test,  or stop your docker compose network
Use Docker Desktop Dashboard to view the docker compose network you have started.

The sequence of commands in docker-actus-rf20/actus-riskserver-ce/actus-riskservice/tests/TestB_script.txt
can be run from a terminal to validate that the installation is working 

To stop the network use the stop button in Docker Desktop Dashboard , or CNTROL C in the terminal 
window where you issued > docker compose ... up will do it. 

### Building images locally using the provided Dockerfiles
For example - to build the image fnparr/actus-server-rf20:mdb27017 
* go to the folder docker-actus-rf20/actus-server-rf20
* edit file docker-actus-rf20/actus-server-rf20/actus-service/src/main/resource/application.properties to make sure spring.data.mongodb.port=27017
* change it to this value if it was set to 27018
* you need to populate docker-actus-rf20/actus-server-rf20/actus-core/src/main with java source code from https://github.com/actus-core/src/main/java branch rf2dev
  * this can be done with a recursive copy cp -r on linux or MacOS
* Issue the command: > docker build . -t fnparr/actus-server-rf20:mdb27017 --no-cache
* the image will be visible as a local image in your Docker Desktop Dashboard.

Setting the spring.data.mongodb.port=27018instead of 27107 and issuing the same docker build command will generate the :mdb27018 image.

A similar process of:
* copying in actus-core source java code into docker-actus-rf20/actus-riskserver-ce/actus-core/src/main/
* setting the appropriate value of spring.data.mongodb.port in docker-actus-rf20/actus-riskserver-ce/actus-riskservice/src/main/resources/application. properties 
*  Issue the command: > docker build . -t fnparr/actus-riskserver-ce:mdb27017 --no-cache   
 will build that image. ( We have shown the 27017 case )      

