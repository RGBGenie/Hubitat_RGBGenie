// CCT 2color Child
// 2 per controller
metadata (name: "RGBGenie CCT Child", namespace: "rgbgenie", author: "Bryan Copeland") {
	capability "Switch"
	capability "Actuator"
	capability "ColorTemperature"
	capability "SwitchLevel"
}


void on() {
	parent.childOn(device.deviceNetworkId)
}

void off() {
	parent.childOff(device.deviceNetworkId)
}

void setLevel() {

}

void setColorTemperature() {

}

void parentEvent(evt) {
	send.event(evt)
}
