# Hubitat RGBGenie Drivers

Thank you for choosing RGBGenie for your lighting solution! Here you will find the drivers needed to utilize the full functionality of your devices in the Hubitat platform.  We want you to be 100% satisified with your purchase and should you have any questions, please send us a message at info@RGBgenie.com or feel free to call us at 520-338-8849.  


# LED Controllers 

All RGBgenie Z-Wave Plus LED controllers work through the Z-Wave LED-Controller.groovy driver <br>
The LED controller driver allows you to change the type of device so you are not limited by the original model purchase. You can adjust between CCT, Single Color, and RGBW models, and it will change the device's functionality at a firmware level.<br><br>
RGBW Models:<br>
You can optionally enable / disable the WarmWhite component so if you are using RGB only LED strips you won't get signals sent to the WarmWhite output mistakenly. <br>
When using just RGB LEDs color temperature is simulated using just RGB color elemets. Between 2700K to 6500K <br>
When using RGBW LEDs, to get the best color temperature result, you can set the kelvin temperature of the white LED element, to match the specs of your LED strips, and the color temperature range will adjust to the capabilities of your LED strip. The Cold White color temperatures are created using the RGB elements. 



# Touch Panels

A child driver can be loaded for each zone on the multi zone models, the Scene only models are a single Zone by default.<br>
This child driver can be used with the Hubitat built-in mirror me application to syncronize the output of the touch panel to groups of devices that do not support Z-wave associations.  For example, Zigbee or Wifi devices.<br> 
Optionally the child drivers can utilize the Scene buttons as button controllers or scene capture / activate. With the button controller functionality on a 3 scene / 3 zone touch panel you would end up with 9 buttons that can be pushed or held to have actions mapped to any functionality in hubitat.<br><br>
All touch panels support up to 12 associations directly, including the lifeline (01) back to Hubitat.  These can be utilized in any combination per zone, including multiple occurances across each zone in the multizone units.  Endpoint devices bound via the child lifeline run independantly through hub commands and do not affect the direct association device limit of 12.

# Device / Driver List
<table>
<tr><th>Model</th><th>Description</th><th>Driver(s)</th></tr>
<tr><td>ZV-1008</td><td>RGBW LED Controller with built-in power supply</td>		<td>Zwave-LED-Controller.groovy</td></tr>
<tr><td>ZW-1000</td><td>CCT LED Controller</td>									<td>Zwave-LED-Controller.groovy</td><tr>
<tr><td>ZW-1001</td><td>Single Color LED Controller</td>						<td>Zwave-LED-Controller.groovy</td></tr>
<tr><td>ZW-1002</td><td>RGBW LED Controller</td>								<td>Zwave-LED-Controller.groovy</td></tr>
<tr><td>ZW-3001</td><td>3 Zone / 3 Scene Single Color Touch Panel White</td>	<td>Zwave-Touch-Panel.groovy /</td></tr>
<tr><td>ZW-3002</td><td>3 Scene Color Touch Panel White</td>					<td>Zwave-Touch-Panel.groovy</td></tr>
<tr><td>ZW-3003</td><td>3 Scene Color Touch Panel Black</td>					<td>Zwave-Touch-Panel.groovy</td></tr>
<tr><td>ZW-3004</td><td>3 Zone Color Touch Panel White</td>						<td>Zwave-Touch-Panel.groovy</td></tr>
<tr><td>ZW-3005</td><td>3 Zone Color Touch Panel Black</td>						<td>Zwave-Touch-Panel.groovy</td></tr>
<tr><td>ZW-3011</td><td>3 Zone / 3 Scene CCT Color Touch Panel White</td>		<td>Zwave-Touch-Panel.groovy</td></tr>
<tr><td>ZW-4001</td><td>Micro Controller and Lamp Module</td>					<td>Zwave-Micro-Controller.groovy</td></tr>
</table>
