/**
 *  Homeseer Switch Child Device
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
	definition (name: "Homeseer Switch Child Device", namespace: "NetworkGod/Homeseer Integration", author: "Sidney Eaton") {
		capability "Light"
		capability "Polling"
        capability "Switch"
        parent: "eatonsn4:Local Homeseer"

	}

        //command getDeviceInfo

	simulator {
		// TODO: define status and reply messages here
	}

	tiles(scale: 2) {
        multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: true){
            tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
                attributeState "on", label:'${name}', action:"switch.off", icon:"st.switches.light.on", backgroundColor:"#00a0dc", nextState:"turningOff"
                attributeState "off", label:'${name}', action:"switch.on", icon:"st.switches.light.off", backgroundColor:"#ffffff", nextState:"turningOn"
                attributeState "turningOn", label:'${name}', action:"switch.off", icon:"st.switches.light.on", backgroundColor:"#00a0dc", nextState:"turningOff"
                attributeState "turningOff", label:'${name}', action:"switch.on", icon:"st.switches.light.off", backgroundColor:"#ffffff", nextState:"turningOn"
            }
            //tileAttribute ("device.level", key: "SLIDER_CONTROL") {
            //    attributeState "level", action:"switch level.setLevel"
            //}
        }
        standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "default", label:"", action:"poll", icon:"st.secondary.refresh"
            //state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
            
        }
        main "switch"
        details(["switch", "refresh"])
    }
}

// parse events into attributes
def parse(String description) {
	log.debug "Parsing '${description}'"
	// TODO: handle 'switch' attribute

}

// handle commands
def off() {
	log.debug "Executing 'off'"
    //parent.cmdByLabel("5","Off")
    parent.cmdByLabel(parent.getChildDeviceNetworkId(device.deviceNetworkId), "Off")
    //return cmdByLabel("5","Off")
	// TODO: handle 'off' command
}

def on() {
	log.debug "Executing 'on'"
    //parent.cmdByLabel("5","On")
    parent.cmdByLabel(parent.getChildDeviceNetworkId(device.deviceNetworkId), "On")
    //return cmdByLabel("5","On")
	// TODO: handle 'on' command
//    def host = "192.168.5.61"
//    def port = 81
//    def hosthex = convertIPtoHex(host).toUpperCase() //thanks to @foxxyben for catching this
//    def porthex = convertPortToHex(port).toUpperCase()
//    device.deviceNetworkId = "$hosthex:$porthex" 
    
//    log.debug "The device id configured is: $device.deviceNetworkId"
    
//    def path = "/JSON?request=controldevicebylabel&ref=5&label=On"
//    log.debug "path is: $path"
    
//    def headers = [:] 
//    headers.put("HOST", "$host:$port")
//   	if (CameraAuth) {
//        headers.put("Authorization", userpass)
//    }
    
//    log.debug "The Header is $headers"
    
//    def method = "GET"
        
//    log.debug "The method is $method"
    
//    try {
//    def hubAction = new physicalgraph.device.HubAction(
//    	method: method,
//    	path: path,
//    	headers: headers
//        )
        	
//    log.debug hubAction
//    hubAction
//    }
//    catch (Exception e) {
//    	log.debug "Hit Exception $e on $hubAction"
//    }
}

def poll() {
	log.debug "Executing 'poll'"
    def data = parent.pollChild()
    log.debug "MY DATA: ${data}"
	// TODO: handle 'poll' command
}

def generateEventInformation(data) {
  log.debug "Received event information from parent: " + data
  sendEvent(name: "switch", value: data.valueString.toLowerCase(), descriptionText: "${data.name} is now ${data.valueString.toLowerCase()}!")
}

//def cmdByLabel(String childRefID, String label) {
//  log.debug "Executing lable ${label} on Homeseer ref id ${childRefID}"
//  return new physicalgraph.device.HubAction("""GET /JSON?request=controldevicebylabel&ref=5&label=Off HTTP/1.1\r\nHOST: 192.168.5.61:81\r\n\r\n""", physicalgraph.device.Protocol.LAN, "5", [callback: cmdCallBackHandler])
//}









private String convertIPtoHex(ipAddress) { 
    String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join()
    log.debug "IP address entered is $ipAddress and the converted hex code is $hex"
    return hex

}

private String convertPortToHex(port) {
	String hexport = port.toString().format( '%04x', port.toInteger() )
    log.debug hexport
    return hexport
}

private Integer convertHexToInt(hex) {
	Integer.parseInt(hex,16)
}


private String convertHexToIP(hex) {
	log.debug("Convert hex to ip: $hex") 
	[convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".")
}

private getHostAddress() {
	def parts = device.deviceNetworkId.split(":")
    log.debug device.deviceNetworkId
	def ip = convertHexToIP(parts[0])
	def port = convertHexToInt(parts[1])
	return ip + ":" + port
}