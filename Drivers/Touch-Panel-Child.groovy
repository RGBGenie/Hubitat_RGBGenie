
import hubitat.helper.ColorUtils

metadata {
	definition (name: "RGBGenie Touch Panel Child", namespace: "rgbgenie", author: "Bryan Copeland") {
		capability "SwitchLevel"
		capability "PushableButton"
		capability "HoldableButton"
		capability "ColorControl"
		capability "ChangeLevel"
		capability "ColorMode"
		capability "ColorTemperature"
		capability "Switch"
		capability "Actuator"
		attribute "colorMode", "string"
	}

	preferences {
		if (getDataValue("deviceModel")!="1") {
			input name: "sceneCapture", type: "bool", description: "", title: "Enable scene capture and activate", defaultValue: false, required: true
		}
	}

}
private getCOLOR_TEMP_MIN() { 2700 }
private getCOLOR_TEMP_MAX() { 6500 }
private getCOLOR_TEMP_DIFF() { COLOR_TEMP_MAX - COLOR_TEMP_MIN }

def updated() {
	if (sceneCapture && getDataValue("deviceModel")!="1") { 
		sendEvent(name: "numberOfButtons", value: 0) 
	} else if (!sceneCapture && getDataValue("deviceModel")!="1") {
		sendEvent(name: "numberOfButtons", value: 3)
	}
}

def installed() {

}

def defineMe(value) {
	device.updateDataValue("deviceModel", "$value")
	if (value==1) { 
		sendEvent(name: "numberOfButtons", value: 0)
	} else {
		sendEvent(name: "numberOfButtons", value: 3)
	}
}

def parse(description) {
	log.debug "description"
//	zwaveEvent(cmd)
}

def zwaveEvent(hubitat.zwave.Command cmd) {
    log.debug "skip:${cmd}"
}

def zwaveEvent(hubitat.zwave.commands.basicv1.BasicReport cmd) {
	dimmerEvents(cmd)
}

def zwaveEvent(hubitat.zwave.commands.basicv1.BasicSet cmd) {
	log.debug "basic set: ${cmd}"
	dimmerEvents(cmd)
}

def zwaveEvent(hubitat.zwave.commands.switchmultilevelv3.SwitchMultilevelReport cmd) {
	dimmerEvents(cmd)
}




def zwaveEvent(hubitat.zwave.commands.switchcolorv3.SwitchColorSet cmd) {
	log.debug "got SwitchColorReport: $cmd"
	def colorComponents=cmd.colorComponents
	def warmWhite=null
	def coldWhite=null
	def red=0
	def green=0
	def blue=0
    colorComponents.each { k, v ->
        log.debug "color component: $k : $v"
		switch (k) {
			case 0:
				warmWhite=v
			break
			case 1:
				coldWhite=v
			break
			case 2:
				red=v
			break
			case 3:
				green=v
			break
			case 4:
				blue=v
			break
		}
    }
	if (red > 0 || green > 0 || blue > 0) {
		def hsv=ColorUtils.rgbToHSV([red, green, blue])
		def hue=hsv[0]
		def sat=hsv[1]
		def lvl=hsv[2]
		if (hue != device.currentValue("hue")) {
			sendEvent(name:"hue", value:Math.round(hue), unit:"%")
		}
		if (sat != device.currentValue("saturation")) { 
			sendEvent(name:"saturation", value:Math.round(sat), unit:"%")
		}
		sendEvent(name:"colorMode", value:"RGB")
	} else if (warmWhite != null && coldWhite != null) {
		sendEvent(name:"colorMode", value:"CT")
		def colorTemp = COLOR_TEMP_MIN + (COLOR_TEMP_DIFF / 2)
		if (warmWhite != coldWhite) {
			colorTemp = (COLOR_TEMP_MAX - (COLOR_TEMP_DIFF * warmWhite) / 255) as Integer
		}
		sendEvent(name:"colorTemperature", value: colorTemp)	
	} else if (warmWhite != null) {
		sendEvent(name:"colorMode", value:"CT")
		sendEvent(name:"colorTemperature", value: 2700)
	}
}

def levelChanging(options){
	def level=0
	if (options.upDown) {
		level=options.level-5
	} else {
		level=options.level+5
	}
	if (level>100) level=100
	if (level<0) level=0 

	sendEvent(name: "level", value: level == 99 ? 100 : level , unit: "%")
	if (level>0 && level<100) {
		if (device.currentValue("switch")=="off") sendEvent(name: "switch", value: "on")
		runInMillis(500, "levelChanging", [data: [upDown: options.upDown, level: level]])
	} else if (level==0) {
		if (device.currentValue("switch")=="on") sendEvent(name: "switch", value: "off")
	}
}

def zwaveEvent(hubitat.zwave.commands.switchmultilevelv3.SwitchMultilevelStartLevelChange cmd){
	runInMillis(500, "levelChanging", [data: [upDown: cmd.upDown, level: device.currentValue("level")]])
}

def zwaveEvent(hubitat.zwave.commands.switchmultilevelv3.SwitchMultilevelStopLevelChange cmd) {
	unschedule()
}

def zwaveEvent(hubitat.zwave.commands.switchmultilevelv3.SwitchMultilevelSet cmd) {
	sendEvent(name: "level", value: cmd.value)
}

private dimmerEvents(hubitat.zwave.Command cmd) {
	def value = (cmd.value ? "on" : "off")
	sendEvent(name: "switch", value: value, descriptionText: "$device.displayName was turned $value")
	if (cmd.value) {
		if (cmd.value>100) cmd.value=100
		sendEvent(name: "level", value: cmd.value == 99 ? 100 : cmd.value , unit: "%")
	}
}

def zwaveEvent(hubitat.zwave.commands.sceneactuatorconfv1.SceneActuatorConfSet cmd) {
	if (sceneCapture) {
		if (!state.scene) { state.scene=[:] }
		if(device.currentValue("colorMode")=="RGB") {
			state.scene["${cmd.sceneId}"]=["hue": device.currentValue("hue"), "saturation": device.currentValue("saturation"), "level": device.currentValue("level"), "colorMode": device.currentValue("colorMode"), "switch": device.currentValue("switch")]
		} else {
			state.scene["${cmd.sceneId}"]=["colorTemperature": device.currentValue("colorTemperature"), "level": device.currentValue("level"), "switch": device.currentValue("switch"), "colorMode": device.currentValue("colorMode")]
		}
	} else {
		sendEvent(name: "pushed", value: (cmd.sceneId/16))
	}
}

def zwaveEvent(hubitat.zwave.commands.sceneactivationv1.SceneActivationSet cmd) {
	if (sceneCapture) {
		if (!state.scene) { state.scene=[:] }
		def scene=state.scene["${cmd.sceneId}"] 
		scene.each { k, v ->
			sendEvent(name: k, value: v)
		}
	} else {
		sendEvent(name: "held", value: (cmd.sceneId/16))
	}
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

def setLevel(level) {
	setLevel(level, 1)
}

def setLevel(level, duration) {

}

def setSaturation(percent) {

}

def setHue(value) {

}

def setColor(value) {

}

def setColorTemperature(temp) {

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

