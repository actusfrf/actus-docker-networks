# docker-actusrf20
Source + dockerfiles  to build docker images for actus-server and actus-riskserver with rfAPI 2.0
## Overview
This repo provides materials to build and configure a network of docker containers which can simulate th cash flows of financial contracts defined with the ACTUS standard using different risk scenarios - risk modelling environments. The risk scenarios are defined in a risk service wich is a separate docker container from the ACTUS server providin the contract type sensitive cashflow simulation.  
### Selecting a configuration
If you have a mongodb database service installed in you workenvironment and want to save crated risk entities, ( i.e. scenarios, reference Indexes ) persistently in that, It is likely that the mongodb service is installed to lsten on standard port 27017. 

In that case you will want to bring up config1-docker-actus-rf20 as your docker compose network.
The config1 network requires that you download from the public fnparr/ dockerhub registry or build locally the images:
*  fnparr/actus-server-rf20:mdb27017
*  fnparr/actus-riskserver-ce:mdb27017

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
4.  Go into  the docker-actus-rf20 folder
* For config1:  > docker compose -f config1-docker-actus-rf20 -p config1-docker-actus-rf20 up
* For config2:  > docker compose -f config2-docker-actus-rf20 -p config2-docker-actus-rf20 up


      

