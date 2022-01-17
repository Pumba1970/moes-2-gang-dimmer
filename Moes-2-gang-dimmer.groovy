/*
 *  Moes ZigBee Switch
 *  
 *  Copyright 2020 SmartThings
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *  use this file except in compliance with the License. You may obtain a copy
 *  of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  License for the specific language governing permissions and limitations
 *  under the License.
 *
 */
public static String version() { return "v0.0.1.20210727" }


private getMODEL_MAP() { 
    [
        'TS0601' : 3
    ]
}

metadata {
    definition(name: "Moes ZigBee Switch", namespace: "Moes", author: "Kotsos", ocfDeviceType: "oic.d.light", vid: "Light") {
        capability "Actuator"
        capability "Configuration"
        capability "Refresh"
        capability "Health Check"
        capability "Switch"
        capability "Switch Level"

        command "childOn", ["string"]
        command "childOff", ["string"]
		command "childSetLevel", ["string", "string"]

        //Moeshouse Switch
        // 1 gang
		fingerprint profileId: "0104", model: "TS0601", manufacturer: "_TZE200_amp6tsvy", endpointId: "01", inClusters: "0000,0004,0005,EF00", outClusters: "0019,000A", application: "42", deviceJoinName: "Moes Multi Switch 1"
		// 2 gang dimmer
        fingerprint profileId: "0104", model: "TS0601", manufacturer: "_TZE200_e3oitdyu", endpointId: "01", inClusters: "0000,0004,0005,EF00", outClusters: "0019,000A", application: "44", deviceJoinName: "Moes Multi Switch 1"
        // 3 gang
        fingerprint profileId: "0104", model: "TS0601", manufacturer: "_TZE200_tz32mtza", endpointId: "01", inClusters: "0000,0004,0005,EF00", outClusters: "0019,000A", application: "42", deviceJoinName: "Moes Multi Switch 1"
      }
 }
// Tile
    tiles(scale: 2) {
		multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: true){
			tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
				attributeState "on", label:'${name}', action:"switch.off", icon:"st.switches.light.on", backgroundColor:"#00A0DC", nextState:"turningOff"
				attributeState "off", label:'${name}', action:"switch.on", icon:"st.switches.light.off", backgroundColor:"#ffffff", nextState:"turningOn"
				attributeState "turningOn", label:'${name}', action:"switch.off", icon:"st.switches.light.on", backgroundColor:"#00A0DC", nextState:"turningOff"
				attributeState "turningOff", label:'${name}', action:"switch.on", icon:"st.switches.light.off", backgroundColor:"#ffffff", nextState:"turningOn"
			}
			tileAttribute ("device.level", key: "SLIDER_CONTROL") {
				attributeState "level", action:"switch level.setLevel"
			}
		}
		standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
		}
		main "switch"
//		details(["switch", "refresh"])
        details(["switch", "refresh", "level"])
	}

def installed() {
	//createChildDevices()
	updateDataValue("onOff", "catchall")
	refresh()
}

// Parse incoming device messages to generate events
def parse(String description) {
    Map map = [:]
    //def event = zigbee.getEvent(description)

    if (description?.startsWith('catchall:')) {
        log.debug description
        // call parseCatchAllMessage to parse the catchall message received
        map = parseCatchAllMessage(description)
        if (map != [:]) {
            log.debug "ok send event: $map.name $map.value"
            sendEvent(name: map.name, value: map.value)
        }
    }
    else {
        log.warn "DID NOT PARSE MESSAGE for description : $description"
    }
}

private getChildEndpoint(String dni) {
	dni.split(":")[-1] as Integer
}

def off() {
    log.debug "called off"
    zigbee.command(0xEF00, 0x0, "00010101000100")  //kanaal 1
}

def on() {
    log.debug "called on" 
    zigbee.command(0xEF00, 0x0, "00010101000101")  //kanaal 1
}

def setLevel(value) {
    log.debug "called setLevel with value $value"
    if (value >= 0 && value <= 100) {
        Map commandParams = [:]
        String commandPayload = "0001020200040000" + zigbee.convertToHexString((value * 10) as Integer, 4)  //kanaal 1
        zigbee.command(0xEF00, 0x0, commandPayload)
    }
}

def refresh() {
    log.debug "called refresh"
    zigbee.command(0xEF00, 0x0, "00020100")
    //pauseExecution(1000)
    //zigbee.command(0xEF00, 0x0, "0002020200")
   
}

def childOn(String dni) {
	def childEndpoint = getChildEndpoint(dni)
    	def name = dni.split("-")[-1]
        def cmd = zigbee.smartShield(text: "${name} on").format()
        sendHubCommand(new physicalgraph.device.HubAction(cmd))
    log.debug(" child on ${dni} ${childEndpoint} ")
	zigbee.command(0xEF00, 0x0, "00000701000101") //kanaal 2
    }

def childOff(String dni) {
	def childEndpoint = getChildEndpoint(dni)
    	def name = dni.split("-")[-1]
        def cmd = zigbee.smartShield(text: "${name} off").format()
        sendHubCommand(new physicalgraph.device.HubAction(cmd))
    log.debug " child off ${dni} ${childEndpoint} "
	zigbee.command(0xEF00, 0x0, "00000701000100") //kanaal 2
    }
    

def childSetLevel (String dni, Integer value) {
	log.debug "called setLevel with value $value"
    if (value >= 0 && value <= 100) {
        Map commandParams = [:]
        String commandPayload = "0000080200040000" + zigbee.convertToHexString((value * 10) as Integer, 4)  //kanaal 2 ?
        zigbee.command(0xEF00, 0x0, commandPayload)
  }
}




def configure() {
    log.debug "Configuring Reporting and Bindings."
    zigbee.onOffConfig() + zigbee.levelConfig() + zigbee.onOffRefresh() + zigbee.levelRefresh()
}



private Map parseCatchAllMessage(String description) {
    // Create a map from the raw zigbee message to make parsing more intuitive
    def msg = zigbee.parse(description)
    Map result = [:]
    switch(msg.clusterId) {
        case 0xEF00: 
            def attribute = getAttribute(msg.data)
            def value = getAttributeValue(msg.data)
            log.debug "173 Atribute ${attribute} AttributeValue ${value}"
                switch (attribute) {
                case "switch": 
                    switch(value) {
                        case 0:
                            result = [
                                name: 'switch',
                                value: 'off',
                                data: [buttonNumber: 1],
                                descriptionText: "${device.displayName} button was pressed",
                                isStateChange: true
                            ]
                            log.debug "185 ${device.displayName} button was pressed"
                        break;

                        case 1:
                            result = [
                                name: 'switch',
                                value: 'on',
                                data: [buttonNumber: 1],
                                descriptionText: "${device.displayName} button was pressed",
                                isStateChange: true
                            ]
                            log.debug "${device.displayName} button was pressed"
                            log.debug "297 ${result}"
                        break;
                        
                    }
                
                break;
                
                case "switch2": 
                    switch(value) {
                        case 0:
                            result = [
                                name: 'switch2',
                                value: 'off',
                                data: [buttonNumber: 7],
                                descriptionText: "${device.displayName} button was pressed",
                                isStateChange: true
                            ]
                            log.debug "214 ${device.displayName} button was pressed"
                            
                        break;

                        case 1:
                            result = [
                                name: 'switch2',
                                value: 'on',
                                data: [buttonNumber: 7],
                                descriptionText: "${device.displayName} button was pressed",
                                isStateChange: true
                            ]
                            log.debug "${device.displayName} button was pressed"
                            log.debug "227 ${result}"
                        break;
                        
                    }
                
                case "level": 
                    int levelValue = value / 10
                    result = [
                        name: 'level',
                        value: levelValue,
                        data: [buttonNumber: 1],
                        descriptionText: "${device.displayName} level was modified",
                        isStateChange: true
                    ]
                    log.debug "241 result ${result}"
               
               break;
               
               case "level2": 
                    int levelValue = value / 10
                    result = [
                        name: 'level2',
                        value: levelValue,
                        data: [buttonNumber: 7],
                        descriptionText: "${device.displayName} level was modified",
                        isStateChange: true
                    ]
                    log.debug "254 result ${result}"
               
               break;
            }
        
        break;
    }
    
    return result
}



private String getAttribute(ArrayList _data) {
    log.debug "282 data:${_data}"
    String retValue = ""
      if (_data[1] >0) {  
     	if (_data[2] == 2 && _data[3] == 2 && _data[4] == 0) {
			retValue = "level" 
        }
       
      	if (_data[2] == 1 && _data[3] == 1 && _data[4] == 0) {
            retValue = "switch"
        }
        
   		if (_data[2] == 8 && _data[3] == 2 && _data[4] == 0) {
            retValue = "level2"
        }
        
        if (_data[2] == 7 && _data[3] == 1 && _data[4] == 0) {
            retValue = "switch2"
        }  
    }
    log.debug "301 return ${retValue}"
    return retValue
	
}



private int getAttributeValue(ArrayList _data) {
    int retValue = 0
    
    if (_data.size() >= 6) {
        int dataLength = _data[5] as Integer
        int power = 1;
        for (i in dataLength..1) {
            retValue = retValue + power * _data[i+5]
            power = power * 256
        }
    }
    
    return retValue
