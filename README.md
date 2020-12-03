# wlan-opensync-wifi-controller  #TESTING
 
Opensync Wifi Controller - accepts connections from the access points, pushes configuration, reads metrics from the topics on MQTT broker.

Components in this repository depend on other wlan-cloud repositories. 

How to build components in this repository - checkout [wlan-cloud-workspace](https://github.com/Telecominfraproject/wlan-cloud-workspace) repository and follow its build instructions.

There are several variants of the Opensync Wifi Controller:
* statically configured by the json files - see the projects: opensync-ext-static, opensync-gateway-static-process, opensync-gateway-static-docker
* dynamically configured using REST APIs and provisioning workflows of the CloudSDK - see the projects: opensync-ext-cloud, opensync-gateway-cloud-process, opensync-gateway-cloud-docker

All the variants mentionined above are using common components defined in projects opensync-gateway and opensync-ext-interface
