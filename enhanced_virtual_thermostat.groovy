/**
*  Enhanced Virtual Thermostat
*
*  Author: An Nguyen
*/
definition(
    name: "Enhanced Virtual Thermostat",
    namespace: "amn0408",
    author: "An Nguyen",
    description: "Enhanced control of a space heater or window air conditioner in conjunction with any temperature sensor(s). It takes the average of temperature of the sensors and allows for different setpoints based on mode. Based on the original Virtual Thermostat and Green Thermostat apps.",
    category: "Green Living",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo-switch.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo-switch@2x.png"
)

preferences {
    section("Choose a temperature sensor..."){
        input "sensors", "capability.temperatureMeasurement", title: "Sensors", multiple: true
    }
    section("Enter degrees threshold (ex. 1)"){
        input "threshold", "decimal", title: "Threshold", required: true
    }
    section("Select the heater or air conditioner outlet(s)... "){
        input "outlets", "capability.switch", title: "Outlets", multiple: true
    }
    section("Select 'heat' for a heater and 'cool' for an air conditioner..."){
        input "mode", "enum", title: "Heating or cooling?", metadata: [values: ["heat","cool"]]
    }
    section("When home (day)") {
        input "homeHeat",  "decimal", title:"Heat (ex. 72)", required:true
        input "homeCool",  "decimal", title:"Cool (ex. 76)", required:true
    }
    section("When home (night)") {
        input "nightHeat", "decimal", title:"Heat (ex. 70)", required:true
        input "nightCool", "decimal", title:"Cool (ex. 78)", required:true
    }
    section("When away") {
        input "awayHeat",  "decimal", title:"Heat (ex. 50)", required:true
        input "awayCool",  "decimal", title:"Cool (ex. 85)", required:true
    }
}

def installed()
{
    log.debug "Installed with settings: ${settings}"
    initialize()
}

def updated()
{
    log.debug "Updated with settings: ${settings}"
    unsubscribe()
    initialize()
}

def initialize()
{
    for (sensor in sensors)
        subscribe(sensor, "temperature", temperatureHandler)
    def lastTemp = getTemperatures()
    log.debug "lastTemp($lastTemp)"
    subscribe(location, changedLocationMode)
    subscribe(app, appTouch)
    setSetpoint(lastTemp)
}

def double getTemperatures()
{
    def currentTemp = 0
    def numSensors = 0
    for (sensor in sensors)
    {
        if (sensor.latestValue("temperature") != null)
        {
            currentTemp = currentTemp + sensor.latestValue("temperature")
            numSensors = numSensors + 1
        }
    }
    if (currentTemp != 0 && numSensors != 0)
    {
        currentTemp = currentTemp / numSensors
    }
    return currentTemp
}

def setSetpoint(lastTemp)
{
    if (location.mode == "Home" ) {
        evaluate(lastTemp, homeHeat, homeCool)
    }
    if (location.mode == "Away" ) {
        evaluate(lastTemp, awayHeat, awayCool)
    }
    if (location.mode == "Night" ) {
        evaluate(lastTemp, nightHeat, nightCool)
    }
}

def temperatureHandler(evt)
{
    def lastTemp = getTemperatures()
    log.info "lastTemp: $lastTemp"
    setSetpoint(lastTemp)
}

def changedLocationMode(evt)
{
    log.info "changedLocationMode: $evt, $settings"
    def lastTemp = getTemperatures()
    log.info "lastTemp: $lastTemp"
    setSetpoint(lastTemp)
}

def appTouch(evt)
{
    log.info "appTouch: $evt, $lastTemp, $settings"
    def lastTemp = getTemperatures()
    setSetpoint(lastTemp)
}

private evaluate(currentTemp, desiredHeatTemp, desiredCoolTemp)
{
    log.debug "EVALUATE($currentTemp, $desiredHeatTemp, $desiredCoolTemp, $mode)"
    if (mode == "cool") {
        // air conditioner
        if (currentTemp - desiredCoolTemp > threshold) {
            outlets.on()
        }
        else if (desiredCoolTemp - currentTemp >= threshold) {
            outlets.off()
        }
    }
    else {
        // heater
        if (desiredHeatTemp - currentTemp > threshold) {
            outlets.on()
        }
        else if (currentTemp - desiredHeatTemp >= threshold) {
            outlets.off()
        }
    }
}

// catchall
def event(evt)
{
    log.info "value: $evt.value, event: $evt, settings: $settings, handlerName: ${evt.handlerName}"
}
