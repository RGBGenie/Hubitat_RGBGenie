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
    def descriptionText = "${device.getDisplayName()} color is ${genericName}"
    if (txtEnable) log.info "${descriptionText}"
	sendEvent(name: "colorMode", value: "CT", descriptionText: "${device.getDisplayName()} color mode is CT")
    sendEvent(name: "colorName", value: genericName ,descriptionText: descriptionText)
}



def setGenericName(hue){
    def colorName
    hue = hue.toInteger()
    hue = (hue * 3.6)
    switch (hue.toInteger()){
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
    def descriptionText = "${device.getDisplayName()} color is ${colorName}"
    if (txtEnable) log.info "${descriptionText}"
	sendEvent(name: "colorMode", value: "RGB", descriptionText: "${device.getDisplayName()} color mode is RGB")
    sendEvent(name: "colorName", value: colorName ,descriptionText: descriptionText)
}

def cieToRgb (x, y, Y=254) {
	def z = 1.0 - x - y
	def Y=1.0
	def X=(Y/y) * x
    def Z=(Y/y) * z
	def R= (X * 1.656492) + (Y * -0.354851) + (Z * -0.255038)
	def G= (X * -0.707196) + (Y * 1.655397) + (Z * 0.036152)
	def B= (X * 0.051713) + (Y * -0.121364) + (Z * 1.011530)
	if (R > B &&  R > G && R > 1.0) {
		G = G / R
		B = B / R
		R = 1.0
	} else if (G > B && G > R && G > 1.0){
		R = R / G
		B = B / G
		G = 1.0
	} else if (B > R && B > G && B > 1.0) {
		R = R / B
		G = G / B
		B = 1.0
	}
	def r=(R<=0.0031308) ? 12.92 * R: (1.055 * Math.pow(R, 1.0 / 2.4)) - 0.055
	def g=(G<=0.0031308) ? 12.92 * G: (1.055 * Math.pow(G, 1.0 / 2.4)) - 0.055
	def b=(B<=0.0031308) ? 12.92 * B: (1.055 * Math.pow(B, 1.0 / 2.4)) - 0.055

	return ["r": Math.round(255*r), "g": Math.round(255*g), "b": Math.round(255*b))
}

def RgbToCie (r, g, b)
	r=r/255
	g=g/255
	b=b/255
	def R=(r>0.4045) ? Math.pow((r+0.055)/ 1.055, 2.4) : ( r / 12.92)
	def G=(g>0.4045) ? Math.pow((g+0.055)/ 1.055, 2.4) : ( g / 12.92)
	def B=(b>0.4045) ? Math.pow((b+0.055)/ 1.055, 2.4) : ( b / 12.92)

	X = (R * 0.664511) + (G * 0.154324) + (B * 0.162028)
	Y = (R * 0.283881) + (G * 0.668433) + (B * 0.047685)
	Z = (R * 0.000088) + (G * 0.072310) + (B * 0.986039)
	x = (X/(X+Y+Z))
	y = (Y/(X+Y+Z))
	return (["x": x, "y": y])
}

def cieToCt(x,y) {
	n = ((x-0.3320)/(0.1858-y))
	CCT = (437*Math.pow(n,3))+(3601*Math.pow(n,2))+(6861*n)+5517
	return Math.round(CCT)
}

def hueByteToHue(byteValue) {
	// hue as 0-255 return hue as 0-360
	return Math.Round(byteValue * (360/255))
}

def hueToHueByte(hueValue) {
	// hue as 0-360 return hue as 0-255
	return Math.Round(hueValue / (360/255))
}

def hueToHuePrecision(hueValue) {
	// hue as 0-100 return hue as 0-360
	return Math.Round(hueVlaue * (3.6))
}

def huePrecisionToHue(hueValue) {
	// hue as 0-360 return hue as 0-100
	return Math.Round(hueValue / (3.6))
}
