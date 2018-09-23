/**
 *  Nest Protect Test
 *
 *  Copyright 2017 Sidney Eaton
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
	definition (name: "Nest Protect", namespace: "NetworkGod/Nest", author: "Sidney Eaton") {
		capability "Carbon Monoxide Detector"
		capability "Sensor"
		capability "Smoke Detector"
        capability "Polling"
        capability "Battery"
        attribute "online", "enum", ["Online", "Offline"]
        parent: "NetworkGod:Nest (Connect)"
        command "test"

	}


	simulator {
		// TODO: define status and reply messages here
	}

	tiles {
		// TODO: define your main and details tiles here
        standardTile("OnlineTile", "device.online", width: 1, height: 1) {
			state("Online", label:"Online", icon:"st.Appliances.appliances17", backgroundColor:"#1f9604")
			state("Offline", label:"Offline", icon:"st.Appliances.appliances17", backgroundColor:"#f40000")
		}
        standardTile("SmokeTile", "device.smoke", width: 1, height: 1) {
			state("clear", icon:"https://raw.githubusercontent.com/eatonsn4/SmartThingsPublic/master/Images/Devices/smoke_clear.png", backgroundColor:"#ffffff")
			state("detected", icon:"https://raw.githubusercontent.com/eatonsn4/SmartThingsPublic/master/Images/Devices/smoke_emergency.png", backgroundColor:"#e86d13")
			state("tested", label:"Test", icon:"st.alarm.smoke.test", backgroundColor:"#e86d13")
		}
        standardTile("COTile", "device.carbonMonoxideDetector", width: 1, height: 1) {
			state("clear", icon:"https://raw.githubusercontent.com/eatonsn4/SmartThingsPublic/master/Images/Devices/co_clear.png", backgroundColor:"#ffffff")
			state("detected", icon:"https://raw.githubusercontent.com/eatonsn4/SmartThingsPublic/master/Images/Devices/co_emergency.png", backgroundColor:"#e86d13")
			state("tested", label:"Test", icon:"st.alarm.carbon-monoxide.test", backgroundColor:"#e86d13")
		}
        //valueTile("BatteryTile", "device.battery", inactiveLabel: false, decoration: "flat", width: 1, height: 1) {
        //    state "battery", label:'${currentValue}% battery', unit:"%"
        //}
        standardTile("BatteryTile", "device.battery", width: 1, height: 1) {
            state "100", icon:"https://raw.githubusercontent.com/eatonsn4/SmartThingsPublic/master/Images/Devices/battery_ok.png"
            state "0", icon:"https://raw.githubusercontent.com/eatonsn4/SmartThingsPublic/master/Images/Devices/battery_low.png"
        }
        standardTile("refresh", "device.smoke", inactiveLabel: false, decoration: "flat") {
          state "default", action:"polling.poll", icon:"st.secondary.refresh"
        }
        main("OnlineTile")
	}
}

void poll() {
	log.debug "Executing 'poll' using parent SmartApp"
	def data = parent.pollChild()
    log.debug "MY DATA: ${data}"
}

// parse events into attributes
def parse(String description) {
    log.debug "Parsing '${description}'"
	// TODO: handle 'carbonMonoxide' attribute
	// TODO: handle 'smoke' attribute
	// TODO: handle 'carbonMonoxide' attribute

}

def generateEvent(data) {
    //log.debug "Child device queried cached parent data and received: ${parent.getChildData(device.deviceNetworkId)}"
    log.debug "Child device queried cached parent data and received: ${data}"
    //def data = parent.getChildData(device.deviceNetworkId)
    online_alarm_parser(data.is_online)
    if (!data.is_manual_test_active) {
         smoke_alarm_parser(data.smoke_alarm_state)
         co_alarm_parser(data.co_alarm_state)
    } else {
         smoke_alarm_parser("testing")
         co_alarm_parser("testing")
    }
    battery_alarm_parser(data.battery_health)
}

def online_alarm_parser(val) {
  def result
  switch (val) {
    case 'true':
      result = "Online"
      break
    default:
      result = "Offline"
      break
  }
  sendEvent(name: "online", value: result, descriptionText: "$device.displayName is now " + result +"!")
  return result
}

def smoke_alarm_parser(val) {
  def result
  switch (val) {
    case 'ok':
      result = "clear"
      break
    case 'warning':
      result = "clear"
      break
    case 'emergency':
      result = "detected"
      break
    case 'testing':
      result = "tested"
      break
    default:
      result = "ERROR"
      break
  }
  sendEvent(name: "smoke", value: result, descriptionText: "$device.displayName smoke " + result +"!")
  return result
}

def co_alarm_parser(val) {
  def result
  switch (val) {
    case 'ok':
      result = "clear"
      break
    case 'warning':
      result = "clear"
      break
    case 'emergency':
      result = "detected"
      break
    case 'testing':
      result = "tested"
      break
    default:
      result = "ERROR"
      break
  }
  sendEvent(name: "carbonMonoxideDetector", value: result, descriptionText: "$device.displayName CO " + result +"!")
  return result
}

def battery_alarm_parser(val) {
  def result
  switch (val) {
    case 'replace':
      result = "0"
      break
    default:
      result = "100"
      break
  }
  log.debug "Battery results are $result"
  sendEvent(name: "battery", value: result, descriptionText: "$device.displayName battery level is " + val +"!")
  return result
}