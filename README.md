# Scientific-Hub-Jpeg-File-addon

This program is an add-on for Serco ESA Scientific Data Hub software. 

It allows the system to ingest Jpeg image files performing the necessary meta-data information extraction from a provided metadata file.
Once the products have been ingested users can log into the platform to access, view the relative metadata and download them.
The format of the metadata file should follow the one provided in the example folder.

You can find the original code for the Scientific Data Hub at the address: https://github.com/SentinelDataHub/DataHubSystem


### Set Up ###

The setup is simple:

* make sure to stop the current execution of the dhus.
```
sh stop.sh
```

* build each of the two modules using:
```
mvn package
```
* move the resulting jar files from the target/ folders into the lib/ folder of your dhus installation
* make sure the user of the dhus is the owner of the jar files
* start the dhus
```
sh start.sh
```
