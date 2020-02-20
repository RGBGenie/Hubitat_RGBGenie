
metadata {
	definition (name: "RGBGenie LED Controller", namespace: "rgbgenie", author: "RGBGenie") {
		capability "SwitchLevel"
		capability "ColorControl"
		capability "ColorTemperature"
		capability "ColorMode"
		capability "Configuration"
		capability "Switch"
		capability "Refresh"
		capability "Actuator"
		capability "Sensor"
		capability "Health Check"
		command "testRed"
        command "testGreen"
        command "testBlue"
        command "testWW"
		command "getColorSupported"

		fingerprint mfr: "0330", prod: "0200", model: "D002", deviceJoinName: "RGBGenie LED Controller" // EU
		fingerprint mfr: "0330", prod: "0201", model: "D002", deviceJoinName: "RGBGenie LED Controller" // US
		fingerprint mfr: "0330", prod: "0202", model: "D002", deviceJoinName: "RGBGenie LED Controller" // ANZ
		fingerprint mfr: "0330", prod: "021A", model: "D002", deviceJoinName: "RGBGenie LED Controller" // RU
		inClusters:"0x5E,0x72,0x86,0x26,0x33,0x2B,0x2C,0x71,0x70,0x85,0x59,0x73,0x5A,0x55,0x98,0x9F,0x6C,0x7A" 
	}
	preferences {
		input name: "wwComponent", type: "bool", description: "", title: "Enable WW Component", defaultValue: false
		input name: "wwKelvin", type: "number", description: "", title: "WW Kelvin", defaultValue: 3200
	}
}

private getCOLOR_NAMES() { ["red", "green", "blue", "warmWhite"]}
private getRGB_NAMES() { ["red", "green", "blue"] }
private getCOLOR_TEMP_MIN() { 2700 }
private getCOLOR_TEMP_MAX() { 6500 }
private getCOLOR_TEMP_DIFF() { COLOR_TEMP_MAX - wwKelvin }
private getCmdClassVers() { [0x33:3,0x26:3] }
private getZWAVE_COLOR_COMPONENT_ID() { [warmWhite: 0, coldWhite: 1, red: 2, green: 3, blue: 4] }

def getColorSupported() {
	commands([zwave.switchColorV3.switchColorSupportedGet()])
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

def configure() {
	interrogate()
}

def interrogate() {
	def cmds = []
	cmds << zwave.configurationV2.configurationGet([parameterNumber: 4])
	commands(cmds)
}

def updated() {
	log.debug "updated().."
	response(refresh())
}

def installed() {
	log.debug "installed()..."
	state.colorReceived = ["red": null, "green": null, "blue": null, "warmWhite": null, "coldWhite": null]
//	sendEvent(name: "checkInterval", value: 1860, displayed: false, data: [protocol: "zwave", hubHardwareId: device.hub.hardwareID, offlinePingable: "0"])
	sendEvent(name: "level", value: 100, unit: "%")
	response(refresh())
}

def parse(description) {
	def result = null
	if (description.startsWith("Err 106")) {
		state.sec = 0
	} else if (description != "updated") {
		def cmd = zwave.parse(description, cmdClassVers)
		if (cmd) {
			result = zwaveEvent(cmd)
			log.debug("'$description' parsed to $result")
		} else {
			log.debug("Couldn't zwave.parse '$description'")
		}
	}
	result
}

def zwaveEvent(hubitat.zwave.commands.configurationv2.ConfigurationReport cmd) {
	log.debug "got ConfigurationReport: $cmd"
	if (cmd.parameterNumber==4) {
		state.deviceType=(cmd.scaledConfigurationValue)
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

def zwaveEvent(hubitat.zwave.commands.switchcolorv3.SwitchColorReport cmd) {
// This no longer makes sense .. 
//	log.debug "got SwitchColorReport: $cmd"
//	if (!state.colorReceived)
//		state.colorReceived = ["red": null, "green": null, "blue": null, "warmWhite": null, "coldWhite": null]
//	state.colorReceived[cmd.colorComponent] = cmd.value
//	def result = []
//	// Check if we got all the RGB color components
//	if (COLOR_NAMES.every { state.colorReceived[it] != null }) {
//		def colors = RGB_NAMES.collect { state.colorReceived[it] }
//		log.debug "colors: $colors"
//		// Send the color as hex format
//		def hexColor = "#" + colors.collect { Integer.toHexString(it).padLeft(2, "0") }.join("")
//		result << createEvent(name: "color", value: hexColor)
//		// Send the color as hue and saturation
//		def hsv = rgbToHSV(*colors)
//		result << createEvent(name: "hue", value: hsv.hue)
//		result << createEvent(name: "saturation", value: hsv.saturation)
//		
//		if ((state.colorReceived["red"] == state.colorReceived["blue"]) && (state.colorReceived["blue"]==state.colorReceived["green"])) {
//			def warmWhite=state.colorReceived["warmWhite"]
//			def coldWhite=state.colorReceived["red"]
//			log.debug "warmWhite: $warmWhite, coldWhite: $coldWhite"
//			def colorTemp = wwKelvin + (COLOR_TEMP_DIFF / 2)
//			if (warmWhite != coldWhite) {
//				colorTemp = (COLOR_TEMP_MAX - (COLOR_TEMP_DIFF * warmWhite) / 255) as Integer
//			}
//			result << createEvent(name: "colorTemperature", value: colorTemp)	
//		}		
//		COLOR_NAMES.collect { state.colorReceived[it] = null }
//	}
//	result
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
	response(command(zwave.switchMultilevelV4.switchMultilevelGet()))
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
	def linkText = device.label ?: device.name
	[linkText: linkText, descriptionText: "$linkText: $cmd", displayed: false]
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
	commands([zwave.switchMultilevelV3.switchMultilevelGet()] + queryAllColors())
}

def ping() {
	log.debug "ping().."
	refresh()
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
	setColor(saturation: percent)
}

def setHue(value) {
	log.debug "setHue($value)"
	setColor(hue: value)
}

def setColor(value) {
	/// this needs major work.. 
	log.debug "setColor($value)"
	if (value.hue == null || value.saturation == null) return
	if (value.level == null) value.level=100
	if (logEnable) log.debug "setColor($value)"
	def result = []
	def rgb = hubitat.helper.ColorUtils.hsvToRGB([value.hue, value.saturation, value.level])
    log.debug "r:" + rgb[0] + ", g: " + rgb[1] +", b: " + rgb[2]
	result << zwave.switchColorV3.switchColorSet(red: rgb[0], green: rgb[1], blue: rgb[2], warmWhite:0)
	//if ((device.currentValue("switch") != "on") && (!colorStaging)){
	if (logEnable) log.debug "Bulb is off. Turning on"
 		result << zwave.basicV1.basicSet(value: 0xFF)
        result << zwave.switchMultilevelV3.switchMultilevelGet()
	//}
    commands(result)
}


def setColorTemperature(temp, duration=1) {
	def result = []
	log.debug "setColorTemperature($temp)"
	if(wwComponent) {
		if(temp > COLOR_TEMP_MAX)
			temp = COLOR_TEMP_MAX
		else if(temp < wwKelvin)
			temp = wwKelvin
		// use CT MIN as 2700 but only mix down to the min K of strip
		def warmValue = ((COLOR_TEMP_MAX - temp) / COLOR_TEMP_DIFF * 255) as Integer
		def coldValue = 255 - warmValue
		def rgbTemp = ctToRgb(6500)
		// 6500k = 255, 249, 253 
		// r=value g=value * .9765 b= value * .9922
		result << zwave.switchColorV3.switchColorSet(red: gammaCorrect(coldValue), green: gammaCorrect(Math.round(coldValue*0.9765)), blue: gammaCorrect(Math.round(coldValue*0.9922)), warmWhite: gammaCorrect(warmValue), coldWhite: 0, duration: duration)
	} else {
		def rgbTemp = ctToRgb(temp)
		log.debug "r: " + rgbTemp["r"] + " g: " + rgbTemp["g"] + " b: "+ rgbTemp["b"]
		log.debug "r: " + gammaCorrect(rgbTemp["r"]) + " g: " + gammaCorrect(rgbTemp["g"]) + " b: " + gammaCorrect(rgbTemp["b"])
		result << zwave.switchColorV3.switchColorSet(red: gammaCorrect(rgbTemp["r"]), green: gammaCorrect(rgbTemp["g"]), blue: gammaCorrect(rgbTemp["b"]), warmWhite: 0, coldWhite: 0, duration: duration)
	}
	if (device.currentValue("switch") != "on") {
		result << zwave.basicV1.basicSet(value: 0xFF)
		result << zwave.switchMultilevelV3.switchMultilevelGet()
	}
	log.debug result
	commands(result+queryAllColors())
}

private queryAllColors() {
	COLOR_NAMES.collect { zwave.switchColorV3.switchColorGet(colorComponent: it, colorComponentId: ZWAVE_COLOR_COMPONENT_ID[it]) }
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