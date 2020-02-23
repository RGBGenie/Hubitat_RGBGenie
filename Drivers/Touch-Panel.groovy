// RGB 3Zone Touch Panel Driver


metadata {
	definition (name: "RGBGenie Touch Panel", namespace: "rgbgenie", author: "RGBGenie") {
        capability "Actuator"
        capability "Configuration"
        capability "Refresh"
        //command "setAssociationGroup", ["number", "enum", "number", "number"] // group number, nodes, action (0 - remove, 1 - add), multi-channel endpoint (optional)


		fingerprint mfr:"0330", prod:"021A", model:"D002", deviceJoinName: "RGBGenie LED Controller" // RU
		inClusters:"0x5E,0x72,0x86,0x26,0x33,0x2B,0x2C,0x71,0x70,0x85,0x59,0x73,0x5A,0x55,0x98,0x9F,0x6C,0x7A" 

    }
    preferences {
        input name: "associationsZ1", type: "string", description: "", title: "Zone 1 Associations", required: true
        input name: "associationsZ2", type: "string", description: "", title: "Zone 2 Associations", required: true
        input name: "associationsZ3", type: "string", description: "", title: "Zone 3 Associations", required: true
	}
}

private getDRIVER_VER() { "0.001" }
private getCOMMAND_CLASS_VERS() { [] }
private getZONE_MODEL() {
    // add some stuff here to check if model is a 3 zone 
    
    return true
}
private getNUMBER_OF_GROUPS() { 
    if (ZONE_MODEL) {
        return 4
	} else {
        return 1
	}
}



def initialize() {
    def cmds=[]
    if (parseInt(getDataValue("driverVer")) < 0.001) {
        updateDataValue("zwaveAssociationG1", "")
        updateDataValue("zwaveAssociationG2", "")
        updateDataValue("zwaveAssociationG3", "")
        updateDataValue("zwaveAssociationG4", "")
        //cmds << zwave.associationV2.associationSet(groupingIdentifier: 1, nodeId: zwaveHubNodeId)
	}
    cmds+=pollAssociations()
    updateDataValue("driverVer", DRIVER_VER)

    commands(cmds)
}

def updated() {
    def cmds=[]
    cmds+=processAssociations()
    cmds+=pollAssociations()
    log.debug "updated cmds: ${cmds}"
    commands(cmds)
}


def refresh() {
    def cmds=[]
    cmds+=pollAssociations()
    commands(cmds)
}

def installed() {
    initialize()
}


def pollAssociations() {
    def cmds=[]
    for(int i = 1;i<=NUMBER_OF_GROUPS;i++) {
        cmds << zwave.associationV2.associationGet(groupingIdentifier:i)
    }
    log.debug "pollAssociations cmds: ${cmds}"
    return cmds
}

def configure() {
    initialize()
}

def zwaveEvent(hubitat.zwave.commands.securityv1.SecurityMessageEncapsulation cmd) {
    def encapsulatedCommand = cmd.encapsulatedCommand(commandClassVersions)
    if (encapsulatedCommand) {
        state.sec = 1
        zwaveEvent(encapsulatedCommand)
    }
}

def zwaveEvent(hubitat.zwave.Command cmd) {
    log.debug "skip:${cmd}"
}

def parse(description) {
    def result = null
    if (description != "updated") {
        def cmd = zwave.parse(description, commandClassVersions)
        if (cmd) {
            result = zwaveEvent(cmd)
            //log.debug("'$cmd' parsed to $result")
        } else {
            log.debug "Couldn't zwave.parse '$description'" 
        }
    }
    def now
    if(location.timeZone)
    now = new Date().format("yyyy MMM dd EEE h:mm:ss a", location.timeZone)
    else
    now = new Date().format("yyyy MMM dd EEE h:mm:ss a")
    sendEvent(name: "lastActivity", value: now, displayed:false)
    result
}

private command(hubitat.zwave.Command cmd) {
    if (state.sec) {
        zwave.securityV1.securityMessageEncapsulation().encapsulate(cmd).format()
    } else {
        cmd.format()
    }
}

private commands(commands, delay=200) {
    delayBetween(commands.collect{ command(it) }, delay)
}

def setDefaultAssociation() {
    //def hubitatHubID = (zwaveHubNodeId.toString().format( '%02x', zwaveHubNodeId )).toUpperCase()
    def cmds=[]
    cmds << zwave.associationV2.associationSet(groupingIdentifier: 1, nodeId: zwaveHubNodeId)
    return cmds
}



def processAssociations(){
    def cmds = []
    cmds += setDefaultAssociation()
    if (ZONE_MODEL) { 
        def associationGroups = NUMBER_OF_GROUPS
        for (int i = 2 ; i <= associationGroups; i++) {
            def z=i-1
            log.debug "group: $i dataValue: " + getDataValue("zwaveAssociationG$i") + " parameterValue: " + settings."associationsZ$z"
            def parameterInput=settings."associationsZ$z"
            def newNodeList = []
            def oldNodeList = []
            if (getDataValue("zwaveAssociationG$i") != null) {
                getDataValue("zwaveAssociationG$i").minus("[").minus("]").split(",").each {
                    if (it!="") {
                        oldNodeList.add(it.minus(" "))
                    }
			    }
            }
            if (parameterInput!=null) {
                parameterInput.minus("[").minus("]").split(",").each {
                    if (it!="") {
                        newNodeList.add(it.minus(" "))
                    }
				}
            }
            if (oldNodeList.size > 0 || newNodeList.size > 0) {
                log.debug "${oldNodeList.size} - ${newNodeList.size}"
                oldNodeList.each {
                    if (!newNodeList.contains(it)) {
                        // user removed a node from the list
                        log.debug "removing node: $it, from group: $i"
                        cmds << zwave.associationV2.associationRemove(groupingIdentifier:i, nodeId:Integer.parseInt(it,16))
					}        
				}
                newNodeList.each {
                    cmds << zwave.associationV2.associationSet(groupingIdentifier:i, nodeId:Integer.parseInt(it,16))   
				}                            
			}
       }
    }
    log.debug "processAssociations cmds: ${cmds}"
    return cmds
}

def zwaveEvent(hubitat.zwave.commands.associationv2.AssociationReport cmd) {
    log.debug "${device.label?device.label:device.name}: ${cmd}"
    def temp = []
    if (cmd.nodeId != []) {
       cmd.nodeId.each {
          temp += it.toString().format( '%02x', it.toInteger() ).toUpperCase()
       }
    }
    def zone=cmd.groupingIdentifier-1
    log.debug "${cmd.groupingIdentifier} - $zone - $temp"
    if (zone > 0) {
        device.updateSetting("associationsZ$zone",[value: "${temp.toString().minus("[").minus("]")}", type: "string"])
    }
    updateDataValue("zwaveAssociationG${cmd.groupingIdentifier}", "$temp") 
}

def zwaveEvent(hubitat.zwave.commands.associationv2.AssociationGroupingsReport cmd) {
    if (debugEnable) log.debug "${device.label?device.label:device.name}: ${cmd}"
    sendEvent(name: "groups", value: cmd.supportedGroupings)
    if (infoEnable) log.info "${device.label?device.label:device.name}: Supported association groups: ${cmd.supportedGroupings}"
    state.associationGroups = cmd.supportedGroupings
}

