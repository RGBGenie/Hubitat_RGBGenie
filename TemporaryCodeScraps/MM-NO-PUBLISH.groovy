/*

	Generic Z-Wave RGBWW Controller
	Copyright 2016, 2017, 2018, 2019 Hubitat Inc. All Rights Reserved
	2019-12-23 2.1.8 maxwell
		-initial pub

*/

import java.math.RoundingMode
import hubitat.helper.ColorUtils
import groovy.transform.Field

@Field static Map commandClassVersions = [
        0x20: 1	    //Basic
        ,0x33: 1	//switchcolor
        ,0x60: 3	//multichannel
]

metadata {

    definition (name: "Generic Z-Wave RGBWW Controller", namespace: "hubitat", author: "Mike Maxwell") {
        capability "Switch Level"
        capability "Actuator"
        capability "Switch"
        capability "Refresh"
        capability "Sensor"
        capability "Configuration"
        capability "Color Control"

        command "setRedLevel", ["NUMBER"]
        command "setGreenLevel", ["NUMBER"]
        command "setBlueLevel", ["NUMBER"]
        command "setWarmWhiteLevel", ["NUMBER"]
        command "setColdWhiteLevel", ["NUMBER"]

        fingerprint  mfr:"0330", prod:"0201", deviceId:"D002", inClusters:"0x5E,0x72,0x86,0x26,0x33,0x2B,0x2C,0x71,0x70,0x85,0x59,0x73,0x5A,0x55,0x98,0x9F,0x6C,0x7A", deviceJoinName: "RGBGenie RGBW controller"
    }
    preferences {
        input name: "transitionTime", type: "enum", title: "Transition time (Default 1 second)", options: [[0:"ASAP"],[1:"1 Second"],[2:"2 Seconds"],[5:"5 Seconds"]], defaultValue: 1
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
        input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
    }
}

def installed() {

}

void logsOff() {
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable",[value:"false",type:"bool"])
}

void parse(String description) {
    hubitat.zwave.Command cmd = zwave.parse(description, commandClassVersions)
    if (cmd) zwaveEvent(cmd)
    else log.warn "unable to parse:${description}"
}

void zwaveEvent(hubitat.zwave.commands.basicv1.BasicReport cmd) {
    sendLevelEvent(cmd.value)
}

void zwaveEvent(hubitat.zwave.commands.switchcolorv1.SwitchColorSupportedReport cmd) {
    List<Short> supported = []
    if (cmd.warmWhite) supported.add(0)
    if (cmd.coldWhite) supported.add(1)
    if (cmd.red) supported.add(2)
    if (cmd.green) supported.add(3)
    if (cmd.blue) supported.add(4)
    state.supported = supported
}

void zwaveEvent(hubitat.zwave.commands.switchcolorv1.SwitchColorReport cmd) {
    state."cc${cmd.colorComponentId}" = cmd.value
    if (cmd.colorComponentId == state.supported.last()) {
        if (state.supported.containsAll(2,3,4)) {
            updateColorValues()
        }
    }
}

def zwaveEvent(hubitat.zwave.commands.associationv2.AssociationReport cmd) {
    if (cmd.nodeId.any { it == zwaveHubNodeId }) {
        sendEvent(descriptionText: "$device.displayName is associated in group ${cmd.groupingIdentifier}")
    } else if (cmd.groupingIdentifier == 1) {
        sendEvent(descriptionText: "Associating $device.displayName in group ${cmd.groupingIdentifier}")
        commands(zwave.associationV1.associationSet(groupingIdentifier:cmd.groupingIdentifier, nodeId:zwaveHubNodeId))
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

void zwaveEvent(hubitat.zwave.Command cmd) {
    if (logEnable) log.debug "skip:${cmd}"
}

void sendLevelEvent(rawValue) {
    if (logEnable) log.debug "dimmerEvents value:${rawValue}"

    Integer value = rawValue.toInteger()
    if (value > 99) value = 99
    String switchValue = value == 0 ? "off" : "on"

    Boolean isSwitchChange = isStateChange(device,"switch",switchValue)
    Boolean isLevelChange = isStateChange(device,"level",rawValue.toString())
    //override level change on 0, device is being turned off
    if (switchValue == "off") isLevelChange = false

    String descriptionText
    if (switchValue == "off"){
        if (isSwitchChange) {
            descriptionText = "${device.displayName} was turned off"
            sendEvent(name: "switch", value: switchValue, descriptionText: descriptionText)
        } else {
            descriptionText = "${device.displayName} is off"
            sendEvent(name: "switch", value: switchValue, descriptionText: descriptionText)
        }
        if (txtEnable) log.info "${descriptionText}"
    } else {
        if (isSwitchChange){
            descriptionText = "${device.displayName} was turned on"
            if (txtEnable) log.info "${descriptionText}"
            sendEvent(name: "switch", value: "on", descriptionText: descriptionText)
        } else {
            descriptionText = "${device.displayName} is on"
            if (txtEnable) log.info "${descriptionText}"
            sendEvent(name: "switch", value: "on", descriptionText: descriptionText)
        }
        if (isLevelChange) {
            descriptionText = "${device.displayName} level was set to ${value}%"
            sendEvent(name: "level", value: value, descriptionText: descriptionText, unit:"%")
        } else {
            descriptionText = "${device.displayName} level is ${value}%"
            sendEvent(name: "level", value: value, descriptionText: descriptionText, unit:"%")
        }
        if (txtEnable) log.info "${descriptionText}"
    }
}

void updateColorValues() {
    List<Float> hsv = ColorUtils.rgbToHSV([state.cc2, state.cc3, state.cc4])
    Integer hue = hsv[0].toInteger()
    Integer saturation = hsv[1].toInteger()
    Integer level = hsv[2].toInteger()
    if (level == 0) return
    String descriptionText
    if (hue != device.currentValue("hue")) descriptionText = "${device.displayName} hue was set to ${hue}%"
    else descriptionText = "${device.displayName} hue is ${hue}%"
    if (txtEnable) log.info descriptionText
    sendEvent(name:"hue", value:hue, unit:"%", descriptionText:descriptionText)

    if (saturation != device.currentValue("saturation")) descriptionText = "${device.displayName} saturation was set to ${saturation}%"
    else descriptionText = "${device.displayName} saturation is ${saturation}%"
    if (txtEnable) log.info descriptionText
    sendEvent(name:"saturation", value:saturation, unit:"%",descriptionText:descriptionText)
    setGenericName(hue, saturation)
}

List<String>  on() {
    return [zwave.basicV1.basicSet(value:99).format()]
}

List<String> off() {
    return [zwave.basicV1.basicSet(value:0).format()]
}

List<String> setRedLevel(level){
    if (2 in state.supported) {
        if (level > 99) level = 99
        level = roundValue((level * 255 / 99),0).toInteger()
        Integer tt = defaultTransitionTime()
        runIn(tt + 1,refresh)
        return [zwave.switchColorV2.switchColorSet(red:level, dimmingDuration:tt).format()]
    } else log.trace "red is not supported on this device."
}

List<String> setGreenLevel(level){
    if (3 in state.supported) {
        if (level > 99) level = 99
        level = roundValue((level * 255 / 99),0).toInteger()
        Integer tt = defaultTransitionTime()
        runIn(tt + 1,refresh)
        return [zwave.switchColorV2.switchColorSet(green:level, dimmingDuration:tt).format()]
    } else log.trace "green is not supported on this device."
}

List<String> setBlueLevel(level){
    if (4 in state.supported) {
        if (level > 99) level = 99
        level = roundValue((level * 255 / 99),0).toInteger()
        Integer tt = defaultTransitionTime()
        runIn(tt + 1,refresh)
        return [zwave.switchColorV2.switchColorSet(blue:level, dimmingDuration:tt).format()]
    } else log.trace "blue is not supported on this device."
}

List<String> setWarmWhiteLevel(level){
    if (0 in state.supported) {
        if (level > 99) level = 99
        level = roundValue((level * 255 / 99),0).toInteger()
        Integer tt = defaultTransitionTime()
        runIn(tt + 1,refresh)
        return [zwave.switchColorV2.switchColorSet(warmWhite:level, dimmingDuration:tt).format()]
    } else log.trace "warmWhite is not supported on this device."
}

List<String> setColdWhiteLevel(level){
    if (1 in state.supported) {
        if (level > 99) level = 99
        level = roundValue((level * 255 / 99),0).toInteger()
        Integer tt = defaultTransitionTime()
        runIn(tt + 1,refresh)
        return [zwave.switchColorV2.switchColorSet(coldWhite:level, dimmingDuration:tt).format()]
    } else log.trace "coldWhite is not supported on this device."
}

List<String> setHue(value) {
    return setColor([hue:value,saturation:100, level:100])
}

List<String> setSaturation(value) {
    return setColor([saturation:value,hue:(device.currentValue("hue") ?: 100).toInteger()])
}

List<String> setColor(colorMap) {
    if (colorMap.hue == null || colorMap.saturation == null) return
    if (state.supported.containsAll(2,3,4)) {
        Integer tt = defaultTransitionTime()
        runIn(tt + 1,refresh)
        Integer level = (colorMap?.level ?: device.currentValue("level") ?: 100).toInteger()
        if (level > 99) level = 99
        List<Float> rgb = ColorUtils.hsvToRGB([colorMap.hue,colorMap.saturation,level])
        Integer red = rgb[0].toInteger()
        Integer green = rgb[1].toInteger()
        Integer blue = rgb[2].toInteger()
        return delayBetween([
                zwave.switchColorV2.switchColorSet(red:red,green:green,blue:blue, dimmingDuration:tt).format()
                ,zwave.switchMultilevelV2.switchMultilevelSet(dimmingDuration:tt, value:level).format()
        ], 200)
    } else log.trace "RGB colors are not supported by this device."
}

List<String> setLevel(level) {
    return setLevel(level,defaultTransitionTime())
}

List<String> setLevel(level, transitionTime) {
    if (level > 99) level = 99
    Integer tt = (transitionTime < 128 ? transitionTime : 128 + Math.round(transitionTime / 60)).toInteger()
    return [zwave.switchMultilevelV2.switchMultilevelSet(dimmingDuration:tt, value:level).format()]
}

List<String> refresh() {
    List<String> cmds = []
    state.supported.each {
        cmds.add(zwave.switchColorV1.switchColorGet(colorComponentId:it).format())
    }
    sendHubCommand(new hubitat.device.HubMultiAction(delayBetween(cmds,300), hubitat.device.Protocol.ZWAVE))
}

Integer defaultTransitionTime() {
    return (transitionTime ?: 1).toInteger()
}

List<String> configure() {
    log.info "configure..."
    //fetch capabilities report
    List<String> cmds = [zwave.basicV1.basicGet().format(),"delay 300",zwave.switchColorV1.switchColorSupportedGet().format()]
    runIn(3,refresh)
    return cmds
}

void updated() {
    log.info "updated..."
    log.warn "debug logging is: ${logEnable == true}"
    log.warn "description logging is: ${txtEnable == true}"
    if (logEnable) runIn(1800,logsOff)
}

void setGenericName(hue, saturation){
    String colorName = "white"
    if (saturation > 0) {
        switch ((hue * 3.6).toInteger()){
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
    }
    String descriptionText = "${device.displayName} color is ${colorName}"
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "colorName", value: colorName ,descriptionText: descriptionText)
}

private roundValue(val, scale) {
    return val.setScale(scale,RoundingMode.HALF_UP)
}

