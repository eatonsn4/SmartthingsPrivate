/**
 *  ComEd 5 minute average
 *
 *  Copyright 2018 Sid Eaton
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
metadata {
	definition (name: "ComEd Pricing", namespace: "NetworkGod/ComEd", author: "Sid Eaton") {
		capability "Sensor"
        attribute "PricePerKWH", "number"
        attribute "DateTime", "string"
        command "refresh"
	}


	simulator {
		// TODO: define status and reply messages here
	}

	tiles(scale: 1) {
		// TODO: define your main and details tiles here
        valueTile("Price", "device.PricePerKWH", decoration: "flat", width: 3, height: 2) {
            state "val", label:'${currentValue} Cents', backgroundColor: "#00a0dc"
        }
        
        valueTile("Poll Time", "device.DateTime", decoration: "flat", width: 3, height: 2) {
            state "val", label:'${currentValue}', backgroundColor: "#00a0dc"
        }
        
        //Detail Screen
        details(["Price", "Poll Time"])
	}
    
}



// parse events into attributes
def parse(String description) {
	log.debug "Parsing '${description}'"

}

def installed() {
    refresh()
    runEvery5Minutes(getData)
}

// handle commands
def refresh() {
    getData()
}

def getData() {
    log.debug "Now in method getData()"
    def params = [
      uri: "https://hourlypricing.comed.com/api?type=currenthouraverage",
      contentType: "application/json"
    ]
    
    try {
      httpGet(params) { resp ->
        resp.headers.each {
        log.debug "${it.name} : ${it.value}"
        }
      
      log.debug "Raw data received: ${resp.data}"
      def currentPrice = Double.parseDouble(resp.data[0].price)
      def timeString = new Date("${resp.data[0].millisUTC}".toLong()).format("MM-dd-yy h:mm:ss a", location.timeZone)
      log.debug "Received ContentType: ${resp.contentType} response containing ${currentPrice} cents @ ${timeString}"
      sendEvent(name: "PricePerKWH", value: currentPrice)
      sendEvent(name: "DateTime", value: timeString)
      }
    } catch (e) {
    log.error "something went wrong: $e"
    }

}
