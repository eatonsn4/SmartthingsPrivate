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
        capability "Refresh"
        attribute "PricePerKWH", "number"
        attribute "DateTime", "string"
        attribute "30MinutePricePerKWH", "number"
        attribute "60MinutePricePerKWH", "number"
        //command "refresh"
	}


	simulator {
		// TODO: define status and reply messages here
	}

	tiles(scale: 2) {
		// TODO: define your main and details tiles here
        valueTile("Price", "device.PricePerKWH", decoration: "flat", width: 6, height: 2) {
            state "val", label:'Current price is ${currentValue} Cents', defaultState:true
        }
             
        valueTile("30 Minute Price", "device.30MinutePricePerKWH", decoration: "flat", width: 6, height: 2) {
            state "val", label:'30 minute price is ${currentValue} Cents', defaultState:true
        }
        
        valueTile("60 Minute Price", "device.60MinutePricePerKWH", decoration: "flat", width: 6, height: 2) {
            state "val", label:'60 minute price is ${currentValue} Cents', defaultState:true
        }
        
        
        valueTile("Poll Time", "device.DateTime", decoration: "flat", width: 6, height: 2) {
            state "val", label:'${currentValue}', defaultState:true
        }
        
        standardTile("refresh", "device.switch", inactiveLabel: false, height: 3, width: 6, decoration: "flat") {
            state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
        }
        
        valueTile("PriceMain", "device.PricePerKWH", decoration: "flat", width: 6, height: 2) {
            state "val", label:'${currentValue} Cents', defaultState:true
        }
        
        main(["PriceMain"])
        
        //Detail Screen
        details(["Price", ,"30 Minute Price", "60 Minute Price", "Poll Time", "refresh"])
	}
    
}



// parse events into attributes
def parse(String description) {
	log.debug "Parsing '${description}'"

}

def installed() {
    refresh()
    runEvery5Minutes(ProcessHourlyPricingData)
    runEvery30Minutes(Last30MinuteHandler, [mins: 30])
    runEvery1Hour(Last60MinuteHandler, [mins: 60])
}

// handle commands
def refresh() {
    ProcessHourlyPricingData()
    Last30MinuteHandler([mins: 30])
    Last60MinuteHandler([mins: 60])
}

// Private helper method for scheduling handlers
private def LastXMinuteHandler(data) {
   //log.debug data.get("mins")
   return getAverage(getLastXMinuteAverage(data.get("mins")))
}

// Simple handler for scheduler and refresh button
private def Last30MinuteHandler(data) {
   sendEvent(name: "30MinutePricePerKWH", value: LastXMinuteHandler(data))
}

// Simple handler for scheduler and refresh button
private def Last60MinuteHandler(data) {
   sendEvent(name: "60MinutePricePerKWH", value: LastXMinuteHandler(data))
}


// Gets the last X minutes of 5 minute quotes and converts them to an average price.
def getLastXMinuteAverage(mins) {
     log.debug "Retrieving current hourly average"
     def endDate = new Date()
     def startDate = endDate
     use(groovy.time.TimeCategory) {
       startDate = endDate - mins.minutes
     }
     def resp = getHourlyAverage(startDate.format("yyyyMMddHHmm", location.timeZone), endDate.format("yyyyMMddHHmm", location.timeZone))
     return resp
}

// Generic method to take a price collection returned from the app and convert it into an average price.
def getAverage(priceCollection) {
     try {
          def sum = priceCollection?.sum { Double.parseDouble(it.price) }  // This sometimes seems to work in simulator.
          def count = priceCollection?.count { Double.parseDouble(it.price) }
          def price = (sum / count).round(1)
          log.debug "Computing average for price collection = ${price}, SUM: ${sum}, COUNT: ${count}"
          return price
     } catch (e) {
         log.error "something went wrong: $e"
         return 0.0
     }
          
}

// Returns an average between start and stop dates.
def getHourlyAverage(startDate, endDate) {
     log.debug "Returning data for start date: ${startDate} and end date: ${endDate}"
     def params = [
      uri: "https://hourlypricing.comed.com/api?type=5minutefeed&datestart=${startDate}&dateend=${endDate}",
      contentType: "application/json"
    ]
    try {
         httpGet(params) { resp ->
              resp.headers.each {
                   log.debug "${it.name} : ${it.value}"
              }
              log.debug "response contentType: ${resp.contentType}"
              log.debug "response data: ${resp.data}"
              return resp.data
         }
    } catch (e) {
         log.error "something went wrong: $e"
         return null
    }
}

def getConsolidatedHourlyAverage() {
     log.debug "Getting the consolidated hourly average."
     def params = [
      uri: "https://hourlypricing.comed.com/api?type=currenthouraverage",
      contentType: "application/json"
    ]
    try {
         httpGet(params) { resp ->
              resp.headers.each {
                   log.debug "${it.name} : ${it.value}"
              }
              log.debug "response contentType: ${resp.contentType}"
              log.debug "response data: ${resp.data}"
              return resp
         }
    } catch (e) {
         log.error "Error while trying to fetch url: $e"
         return null
    }
}

// This method is a parent method for the getConsolidatedHourlyAverage() method to allow for easier readibility and troubleshooting
def ProcessHourlyPricingData() {
     try {
          def resp = getConsolidatedHourlyAverage()
          def currentPrice = Double.parseDouble(resp.data[0].price)
          def timeString = new Date("${resp.data[0].millisUTC}".toLong()).format("MM-dd-yy h:mm:ss a", location.timeZone)
          sendEvent(name: "PricePerKWH", value: currentPrice)
          sendEvent(name: "DateTime", value: timeString)
     } catch (e) {
         log.error "Error trying to parse url results: $e"
    }
}