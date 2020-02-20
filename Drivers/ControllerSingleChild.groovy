// single color child 
// 4 childern per controller
// CCT 2color Child
// 2 per controller
metadata (name: "RGBGenie CCT Child", namespace: "rgbgenie", author: "Bryan Copeland") {
	capability "Switch"
	capability "Actuator"
	capability "SwitchLevel"
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

void parentEvent(evt) {
	send.event(evt)
}
