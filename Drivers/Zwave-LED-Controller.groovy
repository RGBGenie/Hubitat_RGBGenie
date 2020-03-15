/* 
*	LED Controller Driver
*	Code written for RGBGenie by Bryan Copeland with major contributions from Adam Kempenich
*
*    - Added clamp(value, lowerBound (default 0), upperBound (default 100)) method
*    - Updated color method for unincluded parameters and removed extra parameters from setHue/setSaturation
*    - Added hue 360º option for users with 0-100 default hues and hueMode setting
*    - Added deviceType 1 settings section
*    - Updated logLevel defaults and removing log.debugs
*    - Updated colorPrestage method for setColor
*    - Adding comments to methods (left off here)
*
*   Updated 2020-02-26 Added importUrl and optional gamma correction on setColor events
*
*/

import hubitat.helper.ColorUtils
import groovy.transform.Field

metadata {
	definition (name: "RGBGenie LED Controller ZW", namespace: "rgbgenie", author: "RGBGenie", importUrl: "https://raw.githubusercontent.com/RGBGenie/Hubitat_RGBGenie/master/Drivers/Zwave-LED-Controller.groovy" ) {
		capability "Actuator"
		capability "ChangeLevel"
		capability "ColorControl"
		capability "ColorMode"
		capability "ColorTemperature"
		capability "Configuration"
		capability "HealthCheck"
		capability "LightEffects"
		capability "Refresh"
		capability "Sensor"
		capability "Switch"
		capability "SwitchLevel"

		attribute "colorMode", "string"
		attribute "lightEffects", "JSON_OBJECT"

		command "testRed"
        command "testGreen"
        command "testBlue"
        command "testWW"
		command "testCW"

		fingerprint mfr: "0330", prod: "0200", deviceId: "D002", inClusters:"0x5E,0x72,0x86,0x26,0x33,0x2B,0x2C,0x71,0x70,0x85,0x59,0x73,0x5A,0x55,0x98,0x9F,0x6C,0x7A", deviceJoinName: "RGBGenie LED Controller" // EU
		fingerprint mfr: "0330", prod: "0201", deviceId: "D002", inClusters:"0x5E,0x72,0x86,0x26,0x33,0x2B,0x2C,0x71,0x70,0x85,0x59,0x73,0x5A,0x55,0x98,0x9F,0x6C,0x7A", deviceJoinName: "RGBGenie LED Controller" // US
		fingerprint mfr: "0330", prod: "0202", deviceId: "D002", inClusters:"0x5E,0x72,0x86,0x26,0x33,0x2B,0x2C,0x71,0x70,0x85,0x59,0x73,0x5A,0x55,0x98,0x9F,0x6C,0x7A", deviceJoinName: "RGBGenie LED Controller" // ANZ
		fingerprint mfr: "0330", prod: "021A", deviceId: "D002", inClusters:"0x5E,0x72,0x86,0x26,0x33,0x2B,0x2C,0x71,0x70,0x85,0x59,0x73,0x5A,0x55,0x98,0x9F,0x6C,0x7A", deviceJoinName: "RGBGenie LED Controller" // RU

	}
	preferences {
		
		input "logLevel", "enum", title: "Logging Level", options: [1: "Error", 2: "Warn", 3: "Info", 4: "Debug", 5: "Trace"], required: false

		if (getDataValue("deviceModel")=="" || getDataValue("deviceModel")==null) {
			input description: "The device type has not been detected.. Please press the configure button", title: "Device Type Detection", displayDuringSetup: false, type: "paragraph", element: "paragraph"
		} else {
			input name: "dimmerSpeed", type: "number", description: "", title: "Dimmer Ramp Rate 0-255", defaultValue: 0, required: true
			input name: "loadStateSave", type: "enum", description: "", title: "Power fail load state restore", defaultValue: 0, required: true, options: [0: "Shut Off Load", 1: "Turn On Load", 2: "Restore Last State"]
			input name: "deviceType", type: "enum", description: "", title: "Change Device Type", defaultValue: getDataValue("deviceModel"), required: false, options: [0: "Single Color", 1: "CCT", 2: "RGBW"]
			
			if (getDataValue("deviceModel") == "1" || getDataValue("deviceModel")=="2") {
				// Color, or Color Temperature Model-specific settings
				input name: "colorPrestage", type: "bool", description: "", title: "Enable Color Prestaging", defaultValue: false, required: true
				input name: "colorDuration", type: "number", description: "", title: "Color Transition Duration", defaultValue: 3, required: true			
			}
			if (getDataValue("deviceModel")=="2") {
				// Color Model-specific settings
				input name: "wwComponent", type: "bool", description: "", title: "Enable Warm White Component", defaultValue: true, required: true
				input name: "wwKelvin", type: "number", description: "", title: "Warm White Temperature", defaultValue: 2700, required: true
				input name: "hueMode", type: "bool", description: "", title: "Send hue in 0-100 (off) or 0-360 (on)", defaultValue: false, required: true
				input name: "enableGammaCorrect", type: "bool", description: "May cause a slight difference in reported color", title: "Enable gamma correction on setColor", defaultValue: false, required: true
			}
			input name: "stageModeSpeed", type: "number", description: "", title: "Light Effect Speed 0-255 (default 243)", defaultValue: 243, required: true
			input name: "stageModeHue", type: "number", description: "", title: "Hue Of Fixed Color Light Effects 0-360", defaultValue: 0, required: true
			if (getDataValue("deviceModel") == "2" && wwComponent) {
				input description: "<table><tr><th>Number</th><th>Color Component</th></tr><tr><td>1</td><td>Red</td></tr><tr><td>2</td><td>Green</td></tr><tr><td>3</td><td>Blue</td></tr><td>4</td><td>WarmWhite</td></tr></table>", title: "Output Descriptions", displayDuringSetup: false, type: "paragraph", element: "paragraph"
			} else if (getDataValue("deviceModel") == "2" && !wwComponent) {
				input description: "<table><tr><th>Number</th><th>Color Component</th></tr><tr><td>1</td><td>Red</td></tr><tr><td>2</td><td>Green</td></tr><tr><td>3</td><td>Blue</td></tr><td>4</td><td>Empty</td></tr></table>", title: "Output Descriptions", displayDuringSetup: false, type: "paragraph", element: "paragraph"
			} else if (getDataValue("deviceModel") == "1") {
				input description: "<table><tr><th>Number</th><th>Color Component</th></tr><tr><td>1</td><td>WarmWhite</td></tr><tr><td>2</td><td>ColdWhite</td></tr><tr><td>3</td><td>WarmWhite</td></tr><td>4</td><td>ColdWhite</td></tr></table>", title: "Output Descriptions", displayDuringSetup: false, type: "paragraph", element: "paragraph"
			}
		}
	}
}

private getRGBW_NAMES() { [RED, GREEN, BLUE, WARM_WHITE] }
private getRGB_NAMES() { [RED, GREEN, BLUE] }
private getCCT_NAMES() { [WARM_WHITE, COLD_WHITE] }
private getRED() { "red" }
private getGREEN() { "green" }
private getBLUE() { "blue" }
private getWARM_WHITE() { "warmWhite" }
private getCOLD_WHITE() { "coldWhite" }
private getCOLOR_TEMP_MIN() { 2700 }
private getCOLOR_TEMP_MAX() { 6500 }
private getCOLOR_TEMP_DIFF_RGBW() { COLOR_TEMP_MAX - wwKelvin }
private getCOLOR_TEMP_DIFF() { COLOR_TEMP_MAX - COLOR_TEMP_MIN }
private getCMD_CLASS_VERS() { [0x33:3,0x26:3,0x85:2,0x71:8,0x20:1] }
private getZWAVE_COLOR_COMPONENT_ID() { [warmWhite: 0, coldWhite: 1, red: 2, green: 3, blue: 4] }

def testRed() {
	def value=255
    commands ([zwave.switchColorV3.switchColorSet(red: value, green: 0, blue: 0, warmWhite:0, coldWhite:0)])
}

def testGreen(){
	def value=255
    commands ([zwave.switchColorV3.switchColorSet(red: 0, green: value, blue: 0, warmWhite:0, coldWhite:0)])
}

def testBlue(){
	def value=255
	commands ([zwave.switchColorV3.switchColorSet(red: 0, green: 0, blue: value, warmWhite:0, coldWhite:0)])
}

def testWW(){
	def value=255
    commands ([zwave.switchColorV3.switchColorSet(red: 0, green: 0, blue: 0, warmWhite: value, coldWhite:0)])
}

def testCW(){
	def value=255
    commands ([zwave.switchColorV3.switchColorSet(red: 0, green: 0, blue: 0, warmWhite: 0, coldWhite:255)])
}

def configure() {
	initializeVars()
	interrogate()
}

def logsOff(){
    logWarn "${device.label?device.label:device.name}: Disabling logging after timeout"
    device.updateSetting("logLevel",[value:"1",type:"number"])
}

def interrogate() {
	logDebug "Querying for device type"
	def cmds = []
	cmds << zwave.configurationV2.configurationGet([parameterNumber: 4])
	cmds << zwave.associationV2.associationGet(groupingIdentifier:1)
	commands(cmds)
}

def updated() {
	logDebug "updated().."
	log.info "Logging level is ${logLevel}"
	def cmds = [] 
    logDebug "deviceModel: "+getDataValue("deviceModel") + " Updated setting: ${deviceType}"
	if (getDataValue("deviceModel") != deviceType.toString()) {
		cmds << zwave.configurationV2.configurationSet([parameterNumber: 4, size: 1, scaledConfigurationValue: deviceType.toInteger()])
		cmds << zwave.configurationV2.configurationGet([parameterNumber: 4])
	}
	cmds << zwave.configurationV2.configurationSet([parameterNumber: 2, size: 1, scaledConfigurationValue: loadStateSave])
	cmds << zwave.configurationV2.configurationSet([parameterNumber: 6, size: 1, configurationValue: [stageModeSpeed as Integer]])
	cmds << zwave.configurationV2.configurationSet([parameterNumber: 8, size: 1, scaledConfigurationValue: hueToHueByte(stageModeHue)])
    logDebug "commands: ${cmds}"
	commands(cmds)
}

private hueToHueByte(hueValue) {
	// hue as 0-360 return hue as 0-255
	return Math.round(hueValue / (360/255))
}


private initializeVars() {
	state.lightEffects = [
		"0":"None",
		"1":"Fade in/out mode, fixed color", 
		"2":"Flash mode fixed color",
		"3":"Rainbow Mode, fixed change effect",
		"4":"Fade in/out mode, color changes randomly",
		"5":"Flash Mode, color changes randomly",
		"6":"Rainbow Mode, color changes randomly",
		"7":"Random Mode"
	]
}

def installed() {
	log.info "installed()..."
	def cmds = []
    cmds << zwave.associationV2.associationGet(groupingIdentifier:1)
    cmds << zwave.configurationV2.configurationGet([parameterNumber: 4])
    commands(cmds)
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


def zwaveEvent(hubitat.zwave.commands.associationv2.AssociationGroupingsReport cmd) {
    logDebug "Supported association groups: ${cmd.supportedGroupings}"
}

def zwaveEvent(hubitat.zwave.commands.associationgrpinfov3.AssociationGroupCommandListReport cmd) {
	logDebug "association group command list report: ${cmd}"
}

def zwaveEvent(hubitat.zwave.commands.associationgrpinfov3.AssociationGroupInfoReport cmd) {
	logDebug "association group info report"
}

def zwaveEvent(hubitat.zwave.commands.associationcommandconfigurationv1.CommandRecordsSupportedReport cmd) {
	logDebug "association command config supported: ${cmd}"
}

def zwaveEvent(hubitat.zwave.commands.associationv2.AssociationReport cmd) {
    if (cmd.nodeId.any { it == zwaveHubNodeId }) {
        sendEvent(descriptionText: "$device.displayName is associated in group ${cmd.groupingIdentifier}")
    } else if (cmd.groupingIdentifier == 1) {
        sendEvent(descriptionText: "Associating $device.displayName in group ${cmd.groupingIdentifier}")
        commands(zwave.associationV1.associationSet(groupingIdentifier:cmd.groupingIdentifier, nodeId:zwaveHubNodeId))
    }
}

def zwaveEvent(hubitat.zwave.commands.configurationv2.ConfigurationReport cmd) {
	logDebug "got ConfigurationReport: $cmd"
	switch (cmd.parameterNumber) {
		case 4:
			device.updateDataValue("deviceModel", "${cmd.scaledConfigurationValue}")
			if (cmd.scaledConfigurationValue!=state.deviceType) {
				state.deviceType=(cmd.scaledConfigurationValue)
			}
			response(refresh())
		break
		case 5:
			def effectName = "None"
			switch (cmd.scaledConfigurationValue) {
				case 0:
					effectName="None"
				break
				case 1:
					effectName="Fade in/out mode, fixed color"
				break
				case 2:
					effectName="Flash mode fixed color"
				break
				case 3:
					effectName="Rainbow Mode, fixed change effect"
				break
				case 4:
					effectName="Fade in/out mode, color changes randomly"
				break
				case 5:
					effectName="Flash Mode, color changes randomly"
				break
				case 6:
					effectName="Rainbow Mode, color changes randomly"
				break
				case 7:
					effectName="Random Mode"
				break
			}
			if (device.currentValue("effectName")!=effectName) sendEvent(name: "effectName", value: effectName)
			state.effectNumber=cmd.scaledConfigurationValue
		break
	}
}

def setEffect(effectNumber) {
	logDebug "Got setEffect " + effectNumber
	def cmds=[]
	cmds << zwave.configurationV2.configurationSet([parameterNumber: 5, size: 1, scaledConfigurationValue: effectNumber])
	cmds << zwave.configurationV2.configurationGet([parameterNumber: 5])
	if (device.currentValue("switch") != "on") {
		cmds << zwave.basicV1.basicSet(value: 0xFF)
		cmds << zwave.switchMultilevelV3.switchMultilevelGet()
	}
	commands(cmds)
}

def setNextEffect() {
	if (state.effectNumber < 7) device.setEffect(state.effectNumber+1)
}

def setPreviousEffect() {
	if (state.effectNumber > 0) device.setEffect(state.effectNumber-1)
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

def zwaveEvent(hubitat.zwave.commands.switchcolorv3.SwitchColorReport cmd) {
	logDebug "got SwitchColorReport: $cmd"
	if (!state.colorReceived) state.colorReceived=["red": null, "green": null, "blue": null, "warmWhite": null, "coldWhite": null]
	state.colorReceived[cmd.colorComponent] = cmd.targetValue
	switch (getDataValue("deviceModel")) {
		case "1":
			// CCT Device Type
			if (CCT_NAMES.every { state.colorReceived[it] != null }) {
				// Got all CCT colors
				def warmWhite = state.colorReceived["warmWhite"]
				def coldWhite = state.colorReceived["coldWhite"]
				def colorTemp = COLOR_TEMP_MIN + (COLOR_TEMP_DIFF / 2)
				if (warmWhite != coldWhite) {
					colorTemp = (COLOR_TEMP_MAX - (COLOR_TEMP_DIFF * warmWhite) / 255) as Integer
				}
				sendEvent(name: "colorTemperature", value: colorTemp)
				// clear state values
				CCT_NAMES.collect { state.colorReceived[it] = null }
			}
		break
		case "2":
			// RGBW Device Type
			if (RGBW_NAMES.every { state.colorReceived[it] != null }) {
				if (device.currentValue("colorMode") == "RGB") {
					def hsv=ColorUtils.rgbToHSV([state.colorReceived["red"], state.colorReceived["green"], state.colorReceived["blue"]])
					def hue=hsv[0]
					def sat=hsv[1]
					def lvl=hsv[2]
					if (hue != device.currentValue("hue")) {
						sendEvent(name:"hue", value:Math.round(hue), unit:"%")
						setGenericName(hue)
					}
					if (sat != device.currentValue("saturation")) { 
						sendEvent(name:"saturation", value:Math.round(sat), unit:"%")
					}
					if (lvl != device.currentValue("level")) {
						sendEvent(name:"level", value:Math.round(lvl), unit:"%")
					}
				} else { 
					if (wwComponent) {
						def colorTemp = COLOR_TEMP_MIN + (COLOR_TEMP_DIFF_RGBW / 2)
						def warmWhite = state.colorReceived["warmWhite"]
						def coldWhite = state.colorReceived["red"]
						if (warmWhite != coldWhite) colorTemp = (COLOR_TEMP_MAX - (COLOR_TEMP_DIFF_RGBW * warmWhite) / 255) as Integer
						sendEvent(name: "colorTemperature", value: colorTemp)
					} else {
						// Math is hard
						sendEvent(name: "colorTemperature", value: state.ctTarget)
					}

				}
				// clear state values
				RGBW_NAMES.collect { state.colorReceived[it] = null }
			}
		break
	}
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

def zwaveEvent(hubitat.zwave.commands.notificationv8.NotificationReport cmd) {
	logDebug "Notification received: ${cmd}"
	if (cmd.notificationType == 9) {
		if (cmd.event == 7) {
			logWarn "Emergency shutoff load malfunction"
		}
	}
}


def zwaveEvent(hubitat.zwave.Command cmd) {
    logDebug "skip:${cmd}"
}

def zwaveEvent(hubitat.zwave.commands.switchcolorv3.SwitchColorSupportedReport cmd) {
	logDebug cmd
}

def buildOffOnEvent(cmd){
	[zwave.basicV1.basicSet(value: cmd), zwave.switchMultilevelV3.switchMultilevelGet()]
}

def on() {
	// Turns on a device

	commands(buildOffOnEvent(0xFF), 3500)
}

def off() {
	// Turns off a device 

	commands(buildOffOnEvent(0x00), 3500)
}

def refresh() {
	// Queries a device for changes 

	def cmds=[]
	cmds << zwave.switchMultilevelV3.switchMultilevelGet()
	cmds << zwave.configurationV2.configurationGet([parameterNumber: 5])
	commands(cmds + queryAllColors())
}

def ping() {
	// Calls refresh() to query changes

	logDebug "ping().."
	refresh()
}

def startLevelChange(direction) {
    def upDownVal = direction == "down" ? 1 : 0
	logDebug "got startLevelChange(${direction})"
    commands([zwave.switchMultilevelV2.switchMultilevelStartLevelChange(ignoreStartLevel: 1, startLevel: 1, upDown: upDownVal)])
}

def stopLevelChange() {
    commands([zwave.switchMultilevelV3.switchMultilevelStopLevelChange()])
}

def setLevel(level, duration = settings.dimmerSpeed) {
	// Sets the Level (brightness) of a device (0-99) over a duration (0-???) of (MS?)

	logDebug "setLevel($level, $duration)"
	
	level = clamp(level, 0, 99)

	commands([
		zwave.switchMultilevelV3.switchMultilevelSet(value: level, dimmingDuration: duration)
	])
}

def setSaturation(percent) {
	// Sets the Saturation of a device (0-100)

	percent = clamp(percent)
	logDebug "setSaturation($percent)"
	setColor(saturation: percent)
}

def setHue(value) {
	// Sets the Hue of a device (0-360) < Add setting for this

	value = clamp(value, 0, 360)
	logDebug "setHue($value)"
	setColor(hue: value)
}

def setColor(value) {
	// Sets the color of a device from HSL

	def setValue = [:]
	def duration=colorDuration?colorDuration:3

	logDebug "setColor($value)"

	if (state.deviceType==2) {
		setValue.hue = value.hue == null ? device.currentValue("hue") : clamp((settings.hueMode == true ? value.hue * 3.6 : value.hue), 0, 360)
		setValue.saturation = value.saturation == null ? device.currentValue("saturation") : clamp(value.saturation)
		setValue.level = value.level == null ? device.currentValue("level") : clamp(value.level)
		logDebug "setColor updated values to $setValue."
		
		// Device HSL values get updated with parse()

		def result = []
		def rgb = ColorUtils.hsvToRGB([setValue.hue, setValue.saturation, setValue.level])
		
		logDebug "HSL Converted to R:${rgb[0]} G:${rgb[1]} B:${rgb[2]}"
		if (enableGammaCorrect) {
			result << zwave.switchColorV3.switchColorSet(red: gammaCorrect(rgb[0]), green: gammaCorrect(rgb[1]), blue: gammaCorrect(rgb[2]), warmWhite:0, dimmingDuration: duration)
		} else {
			result << zwave.switchColorV3.switchColorSet(red: rgb[0], green: rgb[1], blue: rgb[2], warmWhite:0, dimmingDuration: duration)
		}
		if ((device.currentValue("switch") != "on") && !colorPrestage) {
			logDebug "Turning device on with pre-staging"
 			result << zwave.basicV1.basicSet(value: 0xFF)
			result << zwave.switchMultilevelV3.switchMultilevelGet()
		}

		result+=queryAllColors()

		logDebug "commands: ${result}"

		sendEvent(name: "colorMode", value: "RGB")
		commands(result)
	} else {
		logTrace "setColor not supported on this device type"
	}
}


def setColorTemperature(temp) {
	// Sets the colorTemperature of a device

	temp = clamp(temp, 1000, 12000)

	def duration=colorDuration?colorDuration:3
	def warmWhite=0
	def coldWhite=0
	if(temp > COLOR_TEMP_MAX) temp = COLOR_TEMP_MAX
	def result = []
	logDebug "setColorTemperature($temp)"
	switch (getDataValue("deviceModel")) {
		case "0": 
			// Single Color Device Type
			logTrace "setColorTemperature not supported on this device type"
			return
		break
		case "1":
			// Full CCT Devie Type
			if(temp < COLOR_TEMP_MIN) temp = COLOR_TEMP_MIN
			state.ctTarget=temp
			warmValue = ((COLOR_TEMP_MAX - temp) / COLOR_TEMP_DIFF * 255) as Integer
			coldValue = 255 - warmValue
			result << zwave.switchColorV3.switchColorSet(warmWhite: warmValue, coldWhite: coldValue, dimmingDuration: duration)
		break
		case "2":
			// RGBW Device type
			if (wwComponent) {
				// LED strip has warm white
				if(temp < wwKelvin) temp = wwKelvin
				state.ctTarget=temp
				def warmValue = ((COLOR_TEMP_MAX - temp) / COLOR_TEMP_DIFF_RGBW * 255) as Integer
				def coldValue = 255 - warmValue
				def rgbTemp = ctToRgb(6500)
				result << zwave.switchColorV3.switchColorSet(red: gammaCorrect(coldValue), green: gammaCorrect(Math.round(coldValue*0.9765)), blue: gammaCorrect(Math.round(coldValue*0.9922)), warmWhite: gammaCorrect(warmValue), dimmingDuration: duration)
			} else {
				// LED strip is RGB and has no white
				if(temp < COLOR_TEMP_MIN) temp = COLOR_TEMP_MIN
				def rgbTemp = ctToRgb(temp)
				state.ctTarget=temp
				logDebug "r: " + rgbTemp["r"] + " g: " + rgbTemp["g"] + " b: "+ rgbTemp["b"]
				logDebug "r: " + gammaCorrect(rgbTemp["r"]) + " g: " + gammaCorrect(rgbTemp["g"]) + " b: " + gammaCorrect(rgbTemp["b"])
				result << zwave.switchColorV3.switchColorSet(red: gammaCorrect(rgbTemp["r"]), green: gammaCorrect(rgbTemp["g"]), blue: gammaCorrect(rgbTemp["b"]), warmWhite: 0, dimmingDuration: duration)
			}
		break
	}
	if ((device.currentValue("switch") != "on") && !colorPrestage) {
			logDebug "Turning device on with pre-staging"
 			result << zwave.basicV1.basicSet(value: 0xFF)
			result << zwave.switchMultilevelV3.switchMultilevelGet()
	}
	result+=queryAllColors()
	logDebug result
	sendEvent(name: "colorMode", value: "CT")
	commands(result)
}

private queryAllColors() {
	def cmds=[]
	switch (getDataValue("deviceModel")) {
		case "1":
			// cct device type
			CCT_NAMES.collect { cmds << zwave.switchColorV3.switchColorGet(colorComponentId: ZWAVE_COLOR_COMPONENT_ID[it]) }
		break
		case "2":
			// rgbw device type
			RGBW_NAMES.collect { cmds << zwave.switchColorV3.switchColorGet(colorComponentId: ZWAVE_COLOR_COMPONENT_ID[it]) }
		break
	}
	return cmds
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

private ctToRgb(colorTemp) {
	// ct with rgb only
	float red=0
	float blue=0
	float green=0
	def temperature = colorTemp / 100 
	red = 255
	green=(99.4708025861 *  Math.log(temperature)) - 161.1195681661
	if (green < 0) green = 0
	if (green > 255) green = 255
	if (temperature >= 65) {
		blue=255
	} else if (temperature <= 19) {
		blue=0
	} else {
		blue = temperature - 10
		blue = (138.5177312231 * Math.log(blue)) - 305.0447927307
		if (blue < 0) blue = 0
		if (blue > 255) blue = 255
	}
	return ["r": Math.round(red), "g": Math.round(green), "b": Math.round(blue)]
}

private gammaCorrect(value) {
	def temp=value/255
	def correctedValue=(temp>0.4045) ? Math.pow((temp+0.055)/ 1.055, 2.4) : (temp / 12.92)
	return Math.round(correctedValue * 255) as Integer
}

def setGenericTempName(temp){
    if (!temp) return
    def genericName
    def value = temp.toInteger()
    if (value <= 2000) genericName = "Sodium"
    else if (value <= 2100) genericName = "Starlight"
    else if (value < 2400) genericName = "Sunrise"
    else if (value < 2800) genericName = "Incandescent"
    else if (value < 3300) genericName = "Soft White"
    else if (value < 3500) genericName = "Warm White"
    else if (value < 4150) genericName = "Moonlight"
    else if (value <= 5000) genericName = "Horizon"
    else if (value < 5500) genericName = "Daylight"
    else if (value < 6000) genericName = "Electronic"
    else if (value <= 6500) genericName = "Skylight"
    else if (value < 20000) genericName = "Polar"
    sendEvent(name: "colorName", value: genericName)
}

def setGenericName(hue){
    def colorName
    hue = Math.round(hue * 3.6) as Integer
    switch (hue){
        case 0..15: colorName = "Red"	
            break
        case 16..45: colorName = "Orange"
            break
        case 46..75: colorName = "Yellow"
            break
        case 76..105: colorName = "Chartreuse"
            break
        case 106..135: colorName = "Green"
            break
        case 136..165: colorName = "Spring"
            break
        case 166..195: colorName = "Cyan"
            break
        case 196..225: colorName = "Azure"
            break
        case 226..255: colorName = "Blue"
            break
        case 256..285: colorName = "Violet"
            break
        case 286..315: colorName = "Magenta"
            break
        case 316..345: colorName = "Rose"
            break
        case 346..360: colorName = "Red"
            break
    }
    if (device.currentValue("saturation") == 0) colorName = "White"
    sendEvent(name: "colorName", value: colorName)
}

def logError(msg) {
  if (logLevel?.toInteger() >= 1 || logLevel == null) { log.error msg }
}

def logWarn(msg) {
  if (logLevel?.toInteger() >= 2) { log.warn msg }
}

def logInfo(msg) {
  if (logLevel?.toInteger() >= 3) { log.info msg }
}

def logDebug(msg) {
  if (logLevel?.toInteger() >= 4) { log.debug msg }
}

def logTrace(msg) {
  if (logLevel?.toInteger() >= 5) { log.trace msg }
}

def clamp( value, lowerBound = 0, upperBound = 100 ){
    // Takes a value and ensures it's between two defined thresholds

    value == null ? value = upperBound : null

    if(lowerBound < upperBound){
        if(value < lowerBound ){ value = lowerBound}
        if(value > upperBound){ value = upperBound}
    }
    else if(upperBound < lowerBound){
        if(value < upperBound){ value = upperBound}
        if(value > lowerBound ){ value = lowerBound}
    }

    return value
}


