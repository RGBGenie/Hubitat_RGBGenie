/*
*   Micro Dimmer Controller Driver
*	Code written for RGBGenie by Bryan Copeland
*
*
*
*
*/
import groovy.transform.Field


metadata {
    definition (name: "RGBGenie Micro Controller",namespace: "rgbgenie", author: "RGBGenie") {
        capability "Actuator"
        capability "Switch"
        capability "Switch Level"
        capability "ChangeLevel"

        fingerprint deviceId: "3034", inClusters: "0x5E,0x86,0x72,0x5A,0x85,0x59,0x73,0x26,0x27,0x70,0x2C,0x2B,0x5B,0x7A", outClusters: "0x5B", mfr: "0315", prod: "4447", deviceJoinName: "ZWP WD-100 Dimmer"
        fingerprint deviceId: "3034", inClusters: "0x5E,0x86,0x72,0x5A,0x85,0x59,0x55,0x73,0x26,0x70,0x2C,0x2B,0x5B,0x7A,0x9F,0x6C", outClusters: "0x5B", mfr: "0315", prod: "4447", deviceJoinName: "ZLINK ZL-WD-100"
        fingerprint deviceId: "0334", inClusters: "0x26,0x27,0x2B,0x2C,0x72,0x86,0x91,0x77,0x73", mfr: "001D", prod: "1B03", deviceJoinName: "Leviton DXMX1"
        fingerprint deviceId: "0209", inClusters: "0x26,0x27,0x2B,0x2C,0x85,0x72,0x86,0x91,0x77,0x73", outClusters: "0x82", mfr: "001D", prod: "0401", deviceJoinName: "Leviton VRI06-1LZ"
        fingerprint deviceId: "0209", inClusters: "0x25,0x27,0x2B,0x2C,0x85,0x72,0x86,0x91,0x77,0x73", outClusters: "0x82", mfr: "001D", prod: "0301", deviceJoinName: "Leviton VRI10-1LZ"
        fingerprint deviceId: "0209", inClusters: "0x26,0x27,0x2B,0x2C,0x85,0x72,0x86,0x91,0x77,0x73", outClusters: "0x82", mfr: "001D", prod: "0501", deviceJoinName: "Leviton ???"
        fingerprint deviceId: "0334", inClusters: "0x26,0x27,0x2B,0x2C,0x85,0x72,0x86,0x91,0x77,0x73", outClusters: "0x82", mfr: "001D", prod: "0602", deviceJoinName: "Leviton ???"
        fingerprint deviceId: "0001", inClusters: "0x5E,0x85,0x59,0x86,0x72,0x70,0x5A,0x73,0x26,0x20,0x27,0x2C,0x2B,0x7A", outClusters: "0x82", mfr: "001D", prod: "3501", deviceJoinName: "Leviton DZPD3-2BW"

    }
    preferences {
        input name: "logEnable", type: "bool", description: "", title: "Enable Debug Logging", defaultValue: false, required: true
		input name: "parameter2", type: "enum", description: "", title: "Power fail load state restore", defaultValue: 0, required: true, options: [0: "Shut Off Load", 1: "Turn On Load", 2: "Restore Last State"]
        input name: "parameter4", type: "number", description: "", title: "Default Fade Time 0-254", defaultValue: 1, required: true
        input name: "parameter5", type: "number", description: "", title: "Minimum Level", defaultValue: 0, required: true
        input name: "parameter6", type: "enum", description: "", title: "MOSFET Driving Type", defaultValue: 0, required: true, options: [0: "Trailing Edge", 1: "Leading Edge"]
    }
}

private getCMD_CLASS_VERS() { [0x26:3,0x85:2,0x71:8,0x20:1] }

def logsOff() {
	log.warn "debug logging disabled..."
	device.updateSetting("logEnable", [value: "false", type: "bool"])
	if (logEnable) runIn(1800,logsOff)
}

def updated() {
    log.info "updated..."
    log.warn "debug logging is: ${logEnable == true}"
    def cmds=[]
    if (parameter2) cmds << zwave.configurationV2.configurationSet([parameterNumber: 2, size: 1, scaledConfigurationValue: parameter2.toInteger()])
    if (parameter4) cmds << zwave.configurationV2.configurationSet([parameterNumber: 4, size: 1, scaledConfigurationValue: parameter2.toInteger()])
    if (parameter5) cmds << zwave.configurationV2.configurationSet([parameterNumber: 5, size: 1, scaledConfigurationValue: parameter2.toInteger()])
    if (parameter6) cmds << zwave.configurationV2.configurationSet([parameterNumber: 6, size: 1, scaledConfigurationValue: parameter2.toInteger()])
    if (logEnable) log.debug "updated cmds: ${cmds}"
   	if (logEnable) runIn(1800,logsOff)
    commands(cmds)
}

def zwaveEvent(hubitat.zwave.commands.securityv1.SecurityMessageEncapsulation cmd) {
	def encapsulatedCommand = cmd.encapsulatedCommand(CMD_CLASS_VERS)
	if (encapsulatedCommand) {
		state.sec = 1
		def result = zwaveEvent(encapsulatedCommand)
		result = result.collect {
			if (it instanceof hubitat.device.HubAction && !it.toString().startsWith("9881")) {
				response(cmd.CMD + "00" + it.toString())
			} else {
				it
			}
		}
		result
	}
}

private secEncap(hubitat.zwave.Command cmd) {
	zwave.securityV1.securityMessageEncapsulation().encapsulate(cmd).format()
}

private crcEncap(hubitat.zwave.Command cmd) {
	zwave.crc16EncapV1.crc16Encap().encapsulate(cmd).format()
}

private command(hubitat.zwave.Command cmd) {
	if (getDataValue("zwaveSecurePairingComplete") == "true") {
		return zwave.securityV1.securityMessageEncapsulation().encapsulate(cmd).format()
    } else {
		return cmd.format()
    }	
}

private commands(commands, delay=200) {
	delayBetween(commands.collect{ command(it) }, delay)
}

def zwaveEvent(hubitat.zwave.commands.notificationv8.NotificationReport cmd) {
	logDebug "Notification received: ${cmd}"
	if (cmd.notificationType == 9) {
		if (cmd.event == 7) {
			log.warn "Emergency shutoff load malfunction"
		}
	}
}

def installed() {
	device.updateSetting("logEnable", [value: "true", type: "bool"])
	runIn(1800,logsOff)
}

def parse(description) {
	if (description != "updated") {
		def cmd = zwave.parse(description, CMD_CLASS_VERS)
		if (cmd) {
			result = zwaveEvent(cmd)
			logDebug("${description} parsed to $result")
		} else {
			logWarn("unable to parse: ${description}")
		}
	}
}

def zwaveEvent(hubitat.zwave.commands.basicv1.BasicReport cmd) {
	dimmerEvents(cmd)
}

def zwaveEvent(hubitat.zwave.commands.basicv1.BasicSet cmd) {
	dimmerEvents(cmd)
}

def zwaveEvent(hubitat.zwave.commands.switchmultilevelv3.SwitchMultilevelReport cmd) {
	dimmerEvents(cmd)
}

private dimmerEvents(hubitat.zwave.Command cmd) {
	def value = (cmd.value ? "on" : "off")
	sendEvent(name: "switch", value: value, descriptionText: "$device.displayName was turned $value")
	if (cmd.value) {
		sendEvent(name: "level", value: cmd.value == 99 ? 100 : cmd.value , unit: "%")
	}
}

def zwaveEvent(hubitat.zwave.commands.hailv1.Hail cmd) {
	response(command(zwave.switchMultilevelV3.switchMultilevelGet()))
}


def zwaveEvent(hubitat.zwave.Command cmd) {
    if (logEnable) log.debug "skip:${cmd}"
}

def refresh() {
	// Queries a device for changes 
	def cmds=[]
	cmds << zwave.switchMultilevelV3.switchMultilevelGet()
	cmds << zwave.configurationV2.configurationGet([parameterNumber: 5])
	commands(cmds)
}

def buildOffOnEvent(cmd){
	[zwave.basicV1.basicSet(value: cmd), zwave.switchMultilevelV3.switchMultilevelGet()]
}

def on() {
	commands(buildOffOnEvent(0xFF), 3500)
}

def off() {
	commands(buildOffOnEvent(0x00), 3500)
}

def startLevelChange(direction) {
    def upDownVal = direction == "down" ? 1 : 0
	if (logEnable) log.debug "got startLevelChange(${direction})"
    commands([zwave.switchMultilevelV3.switchMultilevelStartLevelChange(ignoreStartLevel: 1, startLevel: 1, upDown: upDownVal)])
}

def stopLevelChange() {
    commands([zwave.switchMultilevelV3.switchMultilevelStopLevelChange()])
}

def setLevel(level) {
    setLevel(level, 1)
}

def setLevel(level, duration) {
	if (logEnable) log.debug "setLevel($level, $duration)"
	commands([
		zwave.switchMultilevelV3.switchMultilevelSet(value: level, dimmingDuration: duration)
	])
}
