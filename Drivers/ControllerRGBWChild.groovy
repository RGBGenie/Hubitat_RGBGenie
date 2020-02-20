// RGBW color child
// 1 child per controller

// CCT 2color Child
// 2 per controller
metadata (name: "RGBGenie CCT Child", namespace: "rgbgenie", author: "Bryan Copeland") {
	capability "Switch"
	capability "Actuator"
	capability "ColorTemperature"
	capability "SwitchLevel"
	capability "ColorMode"
	capability "ColorControl"
}


void on() {
	parent.childOn(device.deviceNetworkId)
}

void off() {
	parent.childOff(device.deviceNetworkId)
}

void setLevel(level, duration=1) {
	parent.childSetLevel(level, duration, device.deviceNetworkId)
}

void setColorTemperature(temp) {
	parent.childSetColorTemperature(temp, device.deviceNetworkId)
}

void setColor(hue,saturation,level=100) {
	parent.childSetColor(hue, saturation, level, device.deviceNetworkId)
}

void parentEvent(evt) {
	send.event(evt)
}
