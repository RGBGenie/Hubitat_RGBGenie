// Child driver for effect activation
// user created effect child 

metadata 
	definition (name: "RGBGenie CCT Child", namespace: "rgbgenie", author: "Bryan Copeland") {
		capability "Switch"
		capability "Actuator"
	}
	preferences {
		input name: "effectNum", type: "enum", description: "", title: "Effect Type", options: [0:"Inactive", 1:"Fade in / out Fixed Hue", 2:"Flash Mode Fixed Hue", 3:"Rainbow Mode", 4:"Faid in / out Random Hue", 5:"Flash Mode Random Hue", 6:"Rainbow mode, color change randomly", 7:"Random mode colors switch randomly"], defaultValue: 0
		input name: "effectSpeed", type: "number", description: "", title: "Speed of Effect", defaultValue: 243
		input name: "effectRepeat", type: "number", description: "", title: "Repeat 0=unlimited", defaultValue: 0
		input name: "effectHue", type: "number", description: "", title: "Hue of effect", defaultValue: 0
	}
}

void on() {
	// effectNum = Param 5 
	// effectSpeed = Param 6
	// effectRepeat = Param 7
	// effectHue = Param 8
	parent.effectOn(effectNum, effectSpeed, effectRepeat, effectHue, device.deviceNetworkId)
}

void off() {
	parent.effectOff(device.deviceNetworkId)
}

void parentEvent(evt) {
	send.event(evt)
}
