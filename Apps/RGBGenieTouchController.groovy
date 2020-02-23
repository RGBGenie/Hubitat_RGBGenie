definition(
	name: "RGBGenieTouchControlCenter",
	namespace: "rgbgenie",
	author: "Bryan Copeland",
	description: "RGB Genie Touch Control Center App",
	category: "Convenience",
	iconUrl: "",
	iconX2Url: "",
)


preferences {
	page(name: "mainPage" )
    section() {
        paragraph "<img src='https://rgbgenie.com/wp-content/uploads/2018/07/logo-2_171w_trans.png'></img>"
		paragraph "Touch Control Center"
    }
}

def mainPage() {
	dynamicPage(name: "mainPage", title: " ", install: true, uninstall: true) {
		section {
			paragraph "<img src='https://rgbgenie.com/wp-content/uploads/2018/07/logo-2_171w_trans.png'></img>"
			paragraph "Touch Control Center"
		}
		section {
			paragraph "<div style="background-image: url('https://rgbgenie.com/wp-content/uploads/2018/08/Control-pic.jpg');"><br><br><br><br>"
            app(name: "childPanels", appName: "RGBGenieTouchController-Child", namespace: "rgbgenie", title: "Add Touch Panel", multiple: true)
			paragraph "<br><br><br><br></div>"
		}
	}
}