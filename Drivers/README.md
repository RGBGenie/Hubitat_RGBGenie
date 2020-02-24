# Hubitat RGBGenie Drivers

All LED controllers work through the LED-Controller.groovy driver 


# Touch Panels
All touch panels work with the Touch-Panel.groovy and Touch-Panel-Child.groovy drivers

A child driver can be loaded for each zone on the multi zone models, the Scene only models are 1 Zone.
This child drivers can be used with the Hubitat built-in mirror application to syncronize the output of the touch panel to groups of devices of any type ex: Z-Wave Zigbee WiFi. 
Optionally the child drivers can utilize the Scene buttons as button controllers or scene capture / activate. With the button controller functionality on a 3 scene / 3 zone touch panel you would end up with 9 buttons that can be pushed or held to have actions mapped to any functionality in hubitat.

<table>
<tr><th>Model</th><th>Description</td><th>Driver</th></tr>
<tr><td>ZV-1008</td><td>RGBW LED Controller with built-in power supply</td><td>LED-Controller.groovy</td></tr>
<tr><td>ZW-1000</td><td>CCT LED Controller</td><td>LED-Controller.groovy</td><tr>
<tr><td>ZW-1001</td><td>Single Color LED Controller</td><td>LED-Controller.groovy</td></tr>
<tr><td>ZW-1002</td><td>RGBW LED Controller</td><td>LED-Controller.groovy</td></tr>
<tr><td>ZW-3001</td><td>3 Zone / 3 Scene Single Color Touch Panel White</td><td>Touch-Panel.groovy / Touch-Panel-Child.groovy</td></tr>
<tr><td>ZW-3002</td><td>3 Scene Color Touch Panel White</td><td>Touch-Panel.groovy / Touch-Panel-Child.groovy</td></tr>
<tr><td>ZW-3003</td><td>3 Scene Color Touch Panel Black</td><td>Touch-Panel.groovy / Touch-Panel-Child.groovy</td></tr>
<tr><td>ZW-3004</td><td>3 Zone Color Touch Panel White</td><td>Touch-Panel.groovy / Touch-Panel-Child.groovy</td></tr>
<tr><td>ZW-3005</td><td>3 Zone Color Touch Panel Black</td><td>Touch-Panel.groovy / Touch-Panel-Child.groovy</td></tr>
<tr><td>ZW-3011</td><td>3 Zone / 3 Scene CCT Color Touch Panel White</td><td>Touch-Panel.groovy / Touch-Panel-Child.groovy</td></tr>
<tr><td>ZW-4001</td><td>Micro Controller and Lamp Module</td><td>Micro-Controller.groovy</td></tr>
</table>

