
import hubitat.helper.ColorUtils

metadata {
	definition (name: "RGBGenie LED Controller", namespace: "rgbgenie", author: "Bryan Copeland") {
		capability "SwitchLevel"
		capability "ColorControl"
		capability "ChangeLevel"
		capability "ColorTemperature"
		capability "ColorMode"
		capability "Configuration"
		capability "Switch"
		capability "Refresh"
		capability "Actuator"
		capability "Sensor"
		capability "HealthCheck"
		capability "LightEffects"

		attribute "colorMode", "string"
		attribute "lightEffects", "JSON_OBJECT"

		command "testRed"
        command "testGreen"
        command "testBlue"
        command "testWW"
		command "testCW"

		fingerprint mfr: "0330", prod: "0200", model: "D002", deviceJoinName: "RGBGenie LED Controller" // EU
		fingerprint mfr: "0330", prod: "0201", model: "D002", deviceJoinName: "RGBGenie LED Controller" // US
		fingerprint mfr: "0330", prod: "0202", model: "D002", deviceJoinName: "RGBGenie LED Controller" // ANZ
		fingerprint mfr: "0330", prod: "021A", model: "D002", deviceJoinName: "RGBGenie LED Controller" // RU
		inClusters:"0x5E,0x72,0x86,0x26,0x33,0x2B,0x2C,0x71,0x70,0x85,0x59,0x73,0x5A,0x55,0x98,0x9F,0x6C,0x7A" 
	}
	preferences {
		input name: "logEnable", type: "bool", description: "", title: "Enable Debug Log", defaultValue: false, required: true
		if (getDataValue("deviceModel")=="" || getDataValue("deviceModel")==null) {
			input description: "The device type has not been detected.. Please press the configure button", title: "Device Type Detection", displayDuringSetup: false, type: "paragraph", element: "paragraph"
		} else {
			input name: "loadStateSave", type: "enum", description: "", title: "Power fail load state restore", defaultValue: 0, required: true, options: [0: "Shut Off Load", 1: "Turn On Load", 2: "Restore Last State"]
			input name: "deviceType", type: "enum", description: "", title: "Change Device Type", defaultValue: getDataValue("deviceModel"), required: false, options: [0: "Single Color", 1: "CCT", 2: "RGBW"]
			if (getDataValue("deviceModel") == "1" || getDataValue("deviceModel")=="2") {
				input name: "colorPrestage", type: "bool", description: "", title: "Enable Color Prestaging", defaultValue: false, required: true
				input name: "colorDuration", type: "number", description: "", title: "Color Transition Duration", defaultValue: 3, required: true			
			}
			if (getDataValue("deviceModel")=="2") {
				input name: "wwComponent", type: "bool", description: "", title: "Enable Warm White Component", defaultValue: true, required: true
				input name: "wwKelvin", type: "number", description: "", title: "Warm White Temperature", defaultValue: 2700, required: true
			}
			input name: "stageModeSpeed", type: "number", description: "", title: "Light Effect Speed 0-255", defaultValue: 0, required: true
			input name: "stageModeHue", type: "number", description: "", title: "Hue Of Fixed Color Light Effects 0-360", defaultValue: 0, required: true
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
private getCMD_CLASS_VERS() { [0x33:3,0x26:3,0x85:2] }
private getZWAVE_COLOR_COMPONENT_ID() { [warmWhite: 0, coldWhite: 1, red: 2, green: 3, blue: 4] }

void getColorSupported() {
	commands([zwave.switchColorV3.switchColorSupportedGet()])
}


void testColorComponent(id) {
	

}
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
    log.warn "${device.label?device.label:device.name}: Disabling logging after timeout"
    device.updateSetting("logEnable",[value:"false",type:"bool"])
}

def interrogate() {
	log.debug "Querying for device type"
	def cmds = []
	cmds << zwave.configurationV2.configurationGet([parameterNumber: 4])
	cmds << zwave.associationV2.associationGet(groupingIdentifier:1)
	commands(cmds)
}

def updated() {
	log.debug "updated().."
	def cmds = [] 
    log.debug "deviceModel: "+getDataValue("deviceModel") + " Updated setting: ${deviceType}"
	if (getDataValue("deviceModel") != deviceType.toString()) {
		cmds << zwave.configurationV2.configurationSet([parameterNumber: 4, size: 1, scaledConfigurationValue: deviceType.toInteger()])
		cmds << zwave.configurationV2.configurationGet([parameterNumber: 4])
	}
	cmds << zwave.configurationV2.configurationSet([parameterNumber: 2, size: 1, scaledConfigurationValue: loadStateSave])
	cmds << zwave.configurationV2.configurationSet([parameterNumber: 6, size: 1, scaledConfigurationValue: stageModeSpeed])
	cmds << zwave.configurationV2.configurationSet([parameterNumber: 8, size: 1, scaledConfigurationValue: hueToHueByte(stageModeHue)])
    log.debug "commands: ${cmds}"
	commands(cmds)
}

private hueToHueByte(hueValue) {
	// hue as 0-360 return hue as 0-255
	return Math.Round(hueValue / (360/255))
}


private initializeVars() {
	state.colorReceived = ["red": null, "green": null, "blue": null, "warmWhite": null, "coldWhite": null]
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
	log.debug "installed()..."
	def cmds = []
    cmds << zwave.associationV2.associationGet(groupingIdentifier:1)
    cmds << zwave.configurationV2.configurationGet([parameterNumber: 4])
    commands(cmds)
}

def parse(description) {
	if (description != "updated") {
		def cmd = zwave.parse(description, cmdClassVers)
		if (cmd) {
			result = zwaveEvent(cmd)
			if (logEnable) log.debug("${description} parsed to $result")
		} else {
			log.warn("unable to parse: ${description}")
		}
	}
}


def zwaveEvent(hubitat.zwave.commands.associationv2.AssociationGroupingsReport cmd) {
    log.debug "Supported association groups: ${cmd.supportedGroupings}"
}

def zwaveEvent(hubitat.zwave.commands.associationgrpinfov3.AssociationGroupCommandListReport cmd) {
	log.debug "association group command list report: ${cmd}"
}

def zwaveEvent(hubitat.zwave.commands.associationgrpinfov3.AssociationGroupInfoReport cmd) {
	log.debug "association group info report"
}

def zwaveEvent(hubitat.zwave.commands.associationcommandconfigurationv1.CommandRecordsSupportedReport cmd) {
	log.debug "association command config supported: ${cmd}"
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
	log.debug "got ConfigurationReport: $cmd"
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
	log.debug "Got setEffect " + effectNumber
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
	log.debug "got SwitchColorReport: $cmd"
	if (!state.colorReceived) state.colorReceived = ["red": null, "green": null, "blue": null, "warmWhite": null, "coldWhite": null]
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
						//sendEvent(name: "colorTemperature", value: rgbToCt(state.colorReceived['red'] as Float, state.colorReceived['green'] as Float, state.colorReceived['blue'] as Float))
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
	def result = [createEvent(name: "switch", value: value, descriptionText: "$device.displayName was turned $value")]
	if (cmd.value) {
		result << createEvent(name: "level", value: cmd.value == 99 ? 100 : cmd.value , unit: "%")
	}
	return result
}

def zwaveEvent(hubitat.zwave.commands.hailv1.Hail cmd) {
	response(command(zwave.switchMultilevelV3.switchMultilevelGet()))
}

def zwaveEvent(hubitat.zwave.commands.securityv1.SecurityMessageEncapsulation cmd) {
	def encapsulatedCommand = cmd.encapsulatedCommand(cmdClassVers)
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

def zwaveEvent(hubitat.zwave.Command cmd) {
    log.debug "skip:${cmd}"
}

def zwaveEvent(hubitat.zwave.commands.switchcolorv3.SwitchColorSupportedReport cmd) {
	log.debug cmd
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

def refresh() {
	def cmds=[]
	cmds << zwave.switchMultilevelV3.switchMultilevelGet()
	cmds << zwave.configurationV2.configurationGet([parameterNumber: 5])
	commands(cmds + queryAllColors())
}

def ping() {
	log.debug "ping().."
	refresh()
}

def startLevelChange(direction) {
    def upDownVal = direction == "down" ? true : false
	if (logEnable) log.debug "got startLevelChange(${direction})"
    commands([zwave.switchMultilevelV3.switchMultilevelStartLevelChange(ignoreStartLevel: true, startLevel: device.currentValue("level"), upDown: upDownVal)])
}

def stopLevelChange() {
    commands([zwave.switchMultilevelV3.switchMultilevelStopLevelChange()])
}

def setLevel(level) {
	setLevel(level, 1)
}

def setLevel(level, duration) {
	log.debug "setLevel($level, $duration)"
	if(level > 99) level = 99
	commands([
		zwave.switchMultilevelV3.switchMultilevelSet(value: level, dimmingDuration: duration),
		zwave.switchMultilevelV3.switchMultilevelGet(),
	], (duration && duration < 12) ? (duration * 1000) : 3500)
}

def setSaturation(percent) {
	log.debug "setSaturation($percent)"
	setColor(hue: device.currentValue("hue"), saturation: percent)
}

def setHue(value) {
	log.debug "setHue($value)"
	setColor(hue: value, saturation: device.currentValue("saturation"))
}

def setColor(value) {
	state.colorReceived = ["red": null, "green": null, "blue": null, "warmWhite": null, "coldWhite": null]
	def duration=colorDuration?colorDuration:3
	log.debug "setColor($value)"
	if (state.deviceType==2) {
		if (value.hue == null || value.saturation == null) return
		if (value.level == null) value.level=100
		if (logEnable) log.debug "setColor($value)"
		def result = []
		def rgb = ColorUtils.hsvToRGB([value.hue, value.saturation, value.level])
		log.debug "r:" + rgb[0] + ", g: " + rgb[1] +", b: " + rgb[2]
		result << zwave.switchColorV3.switchColorSet(red: rgb[0], green: rgb[1], blue: rgb[2], warmWhite:0, dimmingDuration: duration)
		//if ((device.currentValue("switch") != "on") && (!colorStaging)){
		if (logEnable) log.debug "Bulb is off. Turning on"
 			result << zwave.basicV1.basicSet(value: 0xFF)
			result << zwave.switchMultilevelV3.switchMultilevelGet()
		//}
		result+=queryAllColors()
		log.debug "commands: ${result}"
		sendEvent(name: "colorMode", value: "RGB")
		commands(result)
	} else {
		log.trace "setColor not supported on this device type"
	}
}


def setColorTemperature(temp) {
	state.colorReceived = ["red": null, "green": null, "blue": null, "warmWhite": null, "coldWhite": null]
	def duration=colorDuration?colorDuration:3
	def warmWhite=0
	def coldWhite=0
	if(temp > COLOR_TEMP_MAX) temp = COLOR_TEMP_MAX
	def result = []
	log.debug "setColorTemperature($temp)"
	switch (getDataValue("deviceModel")) {
		case "0": 
			// Single Color Device Type
			log.trace "setColorTemperature not supported on this device type"
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
				log.debug "r: " + rgbTemp["r"] + " g: " + rgbTemp["g"] + " b: "+ rgbTemp["b"]
				log.debug "r: " + gammaCorrect(rgbTemp["r"]) + " g: " + gammaCorrect(rgbTemp["g"]) + " b: " + gammaCorrect(rgbTemp["b"])
				result << zwave.switchColorV3.switchColorSet(red: gammaCorrect(rgbTemp["r"]), green: gammaCorrect(rgbTemp["g"]), blue: gammaCorrect(rgbTemp["b"]), warmWhite: 0, dimmingDuration: duration)
			}
		break
	}
	if ((device.currentValue("switch") != "on") && !colorPrestage) {
		result << zwave.basicV1.basicSet(value: 0xFF)
		result << zwave.switchMultilevelV3.switchMultilevelGet()
	}
	result+=queryAllColors()
	log.debug result
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
//	if (zwaveInfo.zw.contains("s") || state.sec == 1) {
//		secEncap(cmd)
//	} else if (zwaveInfo.cc.contains("56")){
//		crcEncap(cmd)
//	} else {
		cmd.format()
//	}
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


def rgbToCt(Float r, Float g, Float b) {
	r=r/255
	g=g/255
	b=b/255
	//def R=(r>0.4045) ? Math.pow((r+0.055)/ 1.055, 2.4) : ( r / 12.92)
	//def G=(g>0.4045) ? Math.pow((g+0.055)/ 1.055, 2.4) : ( g / 12.92)
	//def B=(b>0.4045) ? Math.pow((b+0.055)/ 1.055, 2.4) : ( b / 12.92)

	X = (r * 0.664511) + (g * 0.154324) + (b * 0.162028)
	Y = (r * 0.283881) + (g * 0.668433) + (b * 0.047685)
	Z = (r * 0.000088) + (g * 0.072310) + (b * 0.986039)
	x = (X/(X+Y+Z))
	y = (Y/(X+Y+Z))
	return cieToCt(x,y)
}

def cieToCt(x,y) {
	n = ((x-0.3320)/(0.1858-y))
	CCT = (437*Math.pow(n,3))+(3601*Math.pow(n,2))+(6861*n)+5517
	return Math.round(CCT)
}