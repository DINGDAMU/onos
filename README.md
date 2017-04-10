ONOS : Open Network Operating System
====================================

## New Features:  
    onos>mmwave-add-intent <TAB><source hostID> <TAB><destination hostID>  

According the annotated "probability of success", build the mm-wave intent with the shortest cost between two hosts.  
The related path will be shown in yellow color on WEB GUI.  

## New Application:
    onos>app activate org.onosproject.millimeterwave  
For more details, please read the README.md in $ONOS/apps/millimeterwave. 

## Modifications:

- The default map of ONOS GUI is changed into 'Italy'.  

## How to add a new intent:  

### MMwaveIntent:  

-  New **MMWaveIntent** class in core-api-net
-  New **MMWaveIntentCompiler** class in net-net-intent-impl-compiler
-  Register **MMWaveIntent** class in store-serializers-KryoNamespaces
-  Add EdgeLink's support for mm-wave intent type in **Traffic monitor.java** 
-  Add isIntentRelevantToMMWaveHosts() method for mm-wave intent's highlight on Web GUI in **TopoIntentFilter.java** 
-  Add Related method for mm-wave intents in **topoTraffic.js** and **topoSelect.js** so that the mm-wave intent can be created on WEB GUI directly.