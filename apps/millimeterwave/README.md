# Millimeterwave_onos_app
A millimeterwave application based on ONOS

<img src="https://wiki.onosproject.org/download/attachments/9307113/HOW%20TO_%20Provider%2C%20protocol%2C%20driver%20-%20Outline-2.png?version=1&modificationDate=1461960381812&api=v2" width="50%" height="50%" />  




# Installation 
BUCK will help us to automatically install it in ONOS.

# Activation
    onos>app activate org.onosproject.millimeterwave

    
 
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
    
## Get the K shortest paths with own custmized link weight
    onos>mmwave-devices-paths <source DeviceId> <destination DeviceId>
    onos>mmwave-hosts-path <source hostID> <source hostID>
In our case, the cost depends from the annotation value "probablity of success".  
total cost = fixed cost + dynamic cost  
In Ethernet case, total cost = 100 + 1; (ps = 100)  
In mm-wave case, total cost = 1 + 1/(ps/100);  



### Conditions before choosing the shortest path  
All the switches are considered as indipendent, so the total packet loss = 1 - Ps1 * Ps2 * .... (All the switches in the path).  
The total packet loss should be less than the constraint which is given via RESTful API.

    onos>mmwave-hosts-path -f <source hostID> <source hostID>  
Filter the K shortest paths with the packet loss constraint.

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
     },
     "org.onosproject.millimeterwaveport" : {
       "hosts" : [{
         "hostid":"mmwave",
         "packetlossconstraint": 0.2,
         "maxpaths": 10
      }]
      }
    }
### Configuration  
     onos>onos-netcfg <ONOS's address> <path to JSON>  
     
### MM-wave Topo overlay on WEB GUI
- Start display mode: Highlight millimeter wave links in green color.
- Cancel display mode: No more highlight millimeter wave links.
- Show all related intents
- Show previous related intent
- Show next related intent
- Monitor traffic of selected intent

In addition, the number of mm-wave and Ethernet links will be shown on the Summary panel.

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
