# Millimeterwave_onos_app
A millimeterwave application based on onos

<img src="https://wiki.onosproject.org/download/attachments/9307113/HOW%20TO_%20Provider%2C%20protocol%2C%20driver%20-%20Outline-2.png?version=1&modificationDate=1461960381812&api=v2" width="50%" height="50%" />  

# Prerequisites
- Java 8 JDK (Oracle Java recommended; OpenJDK is not as thoroughly tested)
- Apache Maven 3.3.9
- git
- bash (for packaging & testing)
- Apache Karaf 3.0.5
- ONOS (git clone https://gerrit.onosproject.org/onos)  
----->More information you can find [here](https://wiki.onosproject.org/display/ONOS/Installing+and+Running+ONOS)


# Installation 
    git clone https://github.com/DINGDAMU/millimeterwave-onos-app.git
    cd millimeterwave-onos-app
    mvn clean install
    cd cli
    mvn clean install
    onos-app localhost reinstall! target/*.oar
    cd ../millimeterwavelink
    mvn clean install 
    onos-app localhost reinstall! target/*.oar
    cd ../millimeterwaveport
    mvn clean install 
    onos-app localhost reinstall! target/*.oar

    
 
# Usage 
## This application can acquire the mininet's topology from different subsystems via northbound APIs, such as HostService, LinkService and DeviceService.  
### Show all components by default
    onos>showcomponets  
### Show only devices
    onos>showcomponets -d  
### Show only links
    onos>showcomponets -l  
### Show only hosts
    onos>showcomponets -h  
    
## This application can also add addtional annotations on devices, links and ports by commands.


### Add additional annotations on devices
    onos>annotate-devices <deviceID> <key> <value>  
   
### Add additional annotations on links
    onos>annotate-links <source-connectPoint> <destination-connectPoint> <key> <value>
    
### Add additional annotations on ports
    onos>annotate-ports <deviceID> <Port number> <Port state> <key> <value>
    
## Find the shortest path with own custmized link weight
    onos>mmwave-devices-paths <source DeviceId> <destination DeviceId>
    onos>mmwave-hosts-path <source hostID> <source hostID>
In our case, the cost depends from the annotation value "probablity of success".  
total cost = fixed cost + dynamic cost  
In Ethernet case, total cost = 100 + 1; (ps = 100)  
In mm-wave case, total cost = 1 + 1/(ps/100);  

## Add mm-wave intents  
    onos>mmwave-add-intents <hostId 1> <hostId 2>  
Add the intent between host1 and host2, the path will be the shortest path which calculated by own cost instead of the default cost by **add-host-intent** command.  



## Use JSON files to annotate millimeterwave links and port  
### A JSON example  
    {
     "apps" : {
    "org.onosproject.millimeterwavelink" : {
      "links" : [{
        "src":"of:000000000000000e/5",
        "dst":"of:000000000000000f/3",
        "length": "100",
        "capacity":"100",
        "technology":"mmwave",
        "ps":"86"
      }]
    },
    "org.onosproject.millimeterwaveport" : {
      "ports" : [{
        "technology":"mmwave",
        "deviceID": "of:000000000000000a",
        "portnumber":"1",
        "isEnabled":"true"
      }]
     }
    }
### Configuration  
     onos>onos-netcfg <ONOS's address> <path to JSON>

# License
Copyright -present Open Networking Laboratory

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
