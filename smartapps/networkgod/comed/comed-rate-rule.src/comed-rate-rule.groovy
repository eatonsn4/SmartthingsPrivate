/**
 *  ComEd Peak Rate
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
definition(
    name: "ComEd Rate Rule",
    namespace: "NetworkGod/ComEd",
    author: "Sid Eaton",
    description: "Links into ComEd's peak rate software and allows you to perform actions based upon electricity pricing.",
    category: "Green Living",
    parent: "NetworkGod/ComEd:ComEd Rate",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    page(name: "startPage", title: "ComEd Rule Definition", nextPage: "conditionsPage", install: false, uninstall: true) {
      section("Global Options:") {
        input name: "activate", type: "bool", title: "Enabled?", description: "Enables or disables this rule.", required: true, defaultValue: "true"
        input(name: "ruleType", type: "enum", title: "Pricing Rule Type?", options: ["Greater Than": "Greater Than", "Less Than": "Less Than", "Between": "Between"], required: true, description: "With respect to the price crossing the threshold, when should this rule activate?")
        input "cents_per_kwh", "decimal", required: true, title: "Cents Per KWH Threshold?", defaultValue: 4.5, range: "-10..30"
      }
    }
    page(name: "conditionsPage", title: "ComEd Rule Conditions", install: true, uninstall: true)

    
}

def processPriceChange(newPrice, timeString, previousPrice) {
     // Called by parent to push down new price quotes
     log.debug "Parent sent new price of ${newPrice} for ${timeString} and the previous price was ${previousPrice}"
     
     // Attempt to see if reactivation thresholds were met for either low or high policies.
      resetPricePolicyThreshold(newPrice)
      
     // Main Condition Check
     checkConditions(newPrice, "${timeString}", previousPrice)
}

def installed() {
	log.debug "Installed with settings: ${settings}"
    state.currentPrice = 0.0
    state.lastChangeDate = now()
    state.thresholdStatus = "RESET"
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
	// TODO: subscribe to attributes, devices, locations, etc.
    //runEvery1Minute(testCode)
    // if the user did not override the label, set the label to the default
    if (!overrideLabel) {
        app.updateLabel(defaultLabel())
    }
}

//def testCode() {
//     processPriceChange(5.1, "2018-01-01", 2.0)
//}

// page for allowing the user to give the automation a custom name
def conditionsPage() {
    if (!overrideLabel) {
        // if the user selects to not change the label, give a default label
        def l = defaultLabel()
        log.debug "will set default label of $l"
        app.updateLabel(l)
    }
    dynamicPage(name: "conditionsPage", install: true) { //, title: "Rule Actions", install: true, uninstall: true) {
        section("Rule Options") {
               //def cents_per_kwh_title = "Cents Per KWH Threshold?"
               if (ruleType.toLowerCase() == "between") {
                    //cents_per_kwh_title = "High Cents/KWH Threshold?"
                    input "high_cents_per_kwh", "decimal", required: true, title: "Do no trigger above this Cents/KWH threshold?", defaultValue: 6.0, range: "-10..30"
               } else {
                    input "cents_per_kwh_reset", "decimal", required: true, title: "Do not execute again until price falls below this cents/kwh threshold is met (0.0 = disabled)?", defaultValue: 0.0, range: "0..30"
               }
               //input "cents_per_kwh", "decimal", required: true, title: cents_per_kwh_title, defaultValue: 4.5, range: "0..30", submitOnChange: true
               input "time_delay", "number", required: true, title: "Select how many minutes to wait before this condition is elgible to trigger again (0 = disabled)?", defaultValue: 0
               input "elgibleModes", "mode", title: "Select elgible modes", multiple: true, required: false
               input "afterTime", "time", title: "Only if time is after...", required: false
               input "beforeTime", "time", title: "and before...", required: false
               input "daysOfWeek", "enum", title: "Select Days of the Week", required: true, multiple: true, options: ["Sunday": "Sunday", "Monday": "Monday", "Tuesday": "Tuesday", "Wednesday": "Wednesday", "Thursday": "Thursday", "Friday": "Friday", "Saturday": "Saturday"]
        }
  
        section("Which Actions To Excute?") {
             input "switch_on", "capability.switch", title: "Select the switches to turn on", required: false, multiple:true, hideWhenEmpty: true
             input "switch_off", "capability.switch", title: "Select the switches to turn off", required: false, multiple:true, hideWhenEmpty: true
             input "dimmers","capability.switchLevel", title: "Select the devices to dim", required: false, multiple:true, hideWhenEmpty: true
             input "thermostats", "capability.thermostat", title: "Select the thermostats to change", required: false, multiple:true, hideWhenEmpty: false,submitOnChange: true
        }
        if (dimmers != null) {
             section("Dimmer Options") {
                  input "dimmerLevel", "number", title: "What do you want to set dimmer levels to?", defaultValue: "50", required: "false", submitOnChange: false
             }
        }
        if (thermostats != null) {
             section("Thermostat Override Options") {
                  input "heating_overrideLabel", "bool", title: "Overide Heating Temperature?", defaultValue: "false", required: "false", submitOnChange: true
                  if (heating_overrideLabel) {
                       input "thermostat_heatingSetpoint", "number", title: "Set heating to? (Degrees)", required: false, defaultValue: 73, hideWhenEmpty: false, range: "50..90"
                  }
     
                  input "cooling_overrideLabel", "bool", title: "Override Cooling Temperature?", defaultValue: "false", required: "false", submitOnChange: true
                  if (cooling_overrideLabel) {
                       input "thermostat_coolingSetpoint", "number", title: "Set cooling to? (Degrees)", required: false, defaultValue: 73, hideWhenEmpty: false, range: "50..90"
                  }
             }
        }
        section("Notifications") {
            input("recipients", "contact", title: "Send notifications to") {
                 input(name: "sms", type: "phone", title: "Send A Text To", description: null, required: false)
                 input(name: "pushNotification", type: "bool", title: "Send a push notification", description: null, defaultValue: false)
            }
        }
        if (overrideLabel) {
            section("Automation name") {
                label title: "Enter custom name", defaultValue: app.label, required: false
            }
        } else {
            section("Automation name") {
                paragraph app.label
            }
        }
        section {
            input "overrideLabel", "bool", title: "Edit automation name", defaultValue: "false", required: "false", submitOnChange: true
        }
    }
}

// a method that will set the default label of the automation.
// It uses the lights selected and action to create the automation label
def defaultLabel() {
     if (ruleType.toLowerCase() == "between") {
          return "Trigger when price hits range starting at ${cents_per_kwh}"
     } else { return "Trigger when price is ${ruleType.toLowerCase()} ${cents_per_kwh}" }
}

// This method checks to make sure that the current mode is allowed in the rule list
def checkModes() {
     if (elgibleModes != null) {
          // Modes has been defined check to see if we are in one of them
          def modeResult = elgibleModes.any{it == location.mode}
          log.debug("Mode ${location.mode} within parameters: ${modeResult}")
          return modeResult
     } else {
          // Modes is null therefore we'll return true because any mode should match
          log.debug("No mode conditions found, returning true to skip this check.")
          return true
     }
}

// Perform a time check and return the boolean values.  This allows us to modularize the conditionals making it easier to read and maintain code.
def checkTime() {
     // To allow maximum flexibility, any values not specified (i.e. afterTime and beforeTime) will be substiuted with midnight to enable simple time
     // calculations.  If both are specified then make sure the time is between those values.
     if (afterTime != null & beforeTime == null) {
          def timeResult = timeOfDayIsBetween(afterTime, "00:00", new Date(), location.timeZone)
          log.debug("Checking to make sure time is after ${afterTime} returning ${timeResult}")
          return timeResult
     } else if (afterTime == null & beforeTime != null) {
          def timeResult = timeOfDayIsBetween("00:00", beforeTime, new Date(), location.timeZone)
          log.debug("Checking to make sure time is before ${beforeTime} returning ${timeResult}")
          return timeResult
     } else if (afterTime != null & beforeTime != null) {
          def timeResult = timeOfDayIsBetween(afterTime, beforeTime, new Date(), location.timeZone)
          log.debug("Checking to make sure time is between ${afterTime} and ${beforeTime} returning ${timeResult}")
          return timeResult
     } else {
          log.debug("No time conditions found, returning true to skip this check.")
          // If there is an error or neither time periods are specified this check should always return true.
          return true
     }
}

// This method checks the day of the week and returns a boolean value.
def checkDayOfWeek() {
     if (daysOfWeek != null) {
          def df = new java.text.SimpleDateFormat("EEEE")
          df.setTimeZone(location.timeZone)
          def day = df.format(new Date())
          def dayResult = daysOfWeek.contains(day)
          log.debug("Today is ${day} returning ${dayResult}")
          return dayResult
     } else {
          log.debug("No day of the week conditions found, returning true to skip this check.")
          return true
     }
}

// TODO: implement event handlers

// Reset pricing thresholds if set.  To save Samsung some performance, don't reset if the end value will be the same in atomicState.
def resetPricePolicyThreshold(newPrice) {
   // If threshold is reset skip resetting again.
   if (state.thresholdStatus != "RESET") {
        if (cents_per_kwh_reset == null || cents_per_kwh_reset <= 0.0) {
             resetPricePolicyThresholdAction()
        } else if (ruleType.toLowerCase() == "greater than" && cents_per_kwh_reset >= newPrice) {
             resetPricePolicyThresholdAction()
        } else if (ruleType.toLowerCase() == "less than" && cents_per_kwh_reset <= newPrice) {
             resetPricePolicyThresholdAction()
        }
   }
}

// Helper/action method for when resetPricePolicyThreshold is true.
def resetPricePolicyThresholdAction() {
     log.debug("Resetting threshold status.")
     state.thresholdStatus = "RESET"
}

// This method is a generic method to check to see if the required number of minutes have passed since the last change was made.
def checkTimeCondition(lastChangeDate) {
     def duration = (now() - lastChangeDate)/60000
     log.debug "checkTimeCondition - DATE: ${duration} minutes ago, THRESHOLD: ${time_delay}"
     if (duration >= time_delay) { return true }
     else { return false }
}

// Simple helper/action method
def condition_changer() {
     log.debug ("Executing actions!")
     if (switch_on != null) { switch_on.on() } else { log.debug "ON: ${switch_on}" }
     if (switch_off != null) { switch_off.off() } else { log.debug "OFF: ${switch_off}" }
     if (thermostats != null) {
          log.debug("Attempting to set ${thermostats} to heat: ${thermostat_heatingSetpoint} and cool: ${thermostat_coolingSetpoint}")
          if (heating_overrideLabel) {  thermostats.setHeatingSetpoint(thermostat_heatingSetpoint)  }
          if (cooling_overrideLabel) {  thermostats.setCoolingSetpoint(thermostat_coolingSetpoint)  }
	      thermostats.poll()
     if (dimmers != null) { dimmers.setLevel(dimmerLevel) }
     } else { log.debug "THERMOSTAT: ${thermostats}" }
}

// This is a simple helper to make some of the if statements more readible and predictable.
def condition_helper(enabled, thresholdStatus) {
     log.debug("Rule enabled: ${enabled}, status: ${thresholdStatus}")
     if (enabled && checkModes() && checkTime() && checkDayOfWeek() && thresholdStatus == "RESET") { return true }
     else { return false }
}


def checkConditions(newPrice, timestamp, previousPrice) {
     if (condition_helper(activate, state.thresholdStatus) && checkTimeCondition(state.lastChangeDate)) {
          log.debug "Passed main conditional checks...."
          log.debug "RuleType: ${ruleType.toLowerCase()}, Price: ${newPrice}, Threshold: ${cents_per_kwh}"
          //log.debug "${ruleType.indexOf(ruleType)}"
          def sub_conditions_met = false
          if (ruleType.toLowerCase() == "greater than" && newPrice >= cents_per_kwh) { sub_conditions_met = true }
          else if (ruleType.toLowerCase() == "less than" && newPrice <= cents_per_kwh) { sub_conditions_met = true }
          else if (ruleType.toLowerCase() == "between" && newPrice >= cents_per_kwh && newPrice <= high_cents_per_kwh) { sub_conditions_met = true }
          
          if (sub_conditions_met) {
               log.debug("ComEd rate is ${ruleType.toLowerCase()} threshold of ${cents_per_kwh} on ${timestamp} with price ${newPrice}!")
               sendMessage("ComEd rate is ${ruleType.toLowerCase()} threshold of ${cents_per_kwh} on ${timestamp} with price ${newPrice}!")
               condition_changer()
               state.lastChangeDate = now()
               state.thresholdStatus = "REACHED"
          }
     }
}

def sendMessage(msg) {
    if (location.contactBookEnabled) {
        sendNotificationToContacts(msg, recipients)
    }
    else {
        if (sms) {
            sendSms(sms, msg)
        }
        if (pushNotification) {
            sendPush(msg)
        }
    }
}
