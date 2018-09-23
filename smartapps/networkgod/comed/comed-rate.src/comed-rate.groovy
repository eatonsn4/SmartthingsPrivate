/**
 *  ComEd Rate
 *
 *  Copyright 2018 Sid Eaton
 *
 */
definition(
    name: "ComEd Rate",
    namespace: "NetworkGod/ComEd",
    author: "Sid Eaton",
    description: "Links into ComEd's peak rate software and allows you to perform actions based upon electricity pricing.",
    category: "Green Living",
    //singleInstance: true,
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    // The parent app preferences are pretty simple: just use the app input for the child app.
    page(name: "mainPage", title: "ComEd Rules", install: true, uninstall: true,submitOnChange: true) {
        section {
            app(name: "ComEdChildApp", appName: "ComEd Rate Rule", namespace: "NetworkGod/ComEd", title: "Create New Automation", multiple: true)
            }
            
    }
    //page(name: "statusPage")
}

//def statusPage() {
//     dynamicPage(name: "statusPage",install: true, uninstall: true) {
//          section("Status") {
//               paragraph "Current rate is ${state.previousPrice}"
//          }
//     }
//}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
	// TODO: subscribe to attributes, devices, locations, etc.
    runEvery5Minutes(getData)
    //runEvery1Minute(getData)
    state.previousPrice = 0.0
}

// TODO: implement event handlers

def getData() {
    log.debug "Checking ComEd Rates!"
    def params = [
      uri: "https://hourlypricing.comed.com/api?type=currenthouraverage",
      contentType: "application/json"
    ]
    
    try {
      httpGet(params) { resp ->
        resp.headers.each {
        log.debug "${it.name} : ${it.value}"
        }
      // Create Timestamp string
      def timeString = new Date("${resp.data[0].millisUTC}".toLong()).format("MM-dd-yy h:mm:ss a", location.timeZone)
      def newPrice = resp.data[0].price
      
      log.debug "ComEd returned price ${newPrice} for ${timeString}"
      
      // Send all children the price
      def children = getChildApps()
      log.debug "$children.size() child apps installed"
      children.each { child ->
           log.debug "Sending price ${newPrice} to child app id ${child.id}"
           child.processPriceChange(Double.parseDouble(newPrice), timeString, state.previousPrice)
      }
      
      //Set Atomic State after event creation
      state.previousPrice = Double.parseDouble(newPrice)
        
      
      }
    } catch (e) {
    log.error "something went wrong: $e"
    }

}