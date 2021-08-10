package com.mqgateway.core.device.mysensors

import com.mqgateway.core.device.Device
import com.mqgateway.core.gatewayconfig.DevicePropertyType
import com.mqgateway.core.gatewayconfig.DeviceType
import com.mqgateway.mysensors.Message
import com.mqgateway.mysensors.MySensorsSerialConnection
import com.mqgateway.mysensors.MySensorsSerialListener
import mu.KotlinLogging
import java.time.LocalDateTime

private val LOGGER = KotlinLogging.logger {}

// TODO add possibility to specify list of childSensorIds
class Bme280MySensorsInputDevice(
  id: String,
  private val mySensorsNodeId: Int,
  private val serialConnection: MySensorsSerialConnection
) : Device(id, DeviceType.BME280), MySensorsSerialListener {

  override fun initDevice() {
    super.initDevice()
    serialConnection.registerDeviceListener(mySensorsNodeId, this)
  }

  override fun onMessageReceived(message: Message) {
    LOGGER.info { "MySensor message received for device '$id': $message" }
    val sensorType = SensorType.byId(message.childSensorId)
    when (sensorType) {
      SensorType.HUMIDITY -> notify(DevicePropertyType.HUMIDITY, message.payload)
      SensorType.TEMPERATURE -> notify(DevicePropertyType.TEMPERATURE, message.payload)
      SensorType.PRESSURE -> notify(DevicePropertyType.PRESSURE, message.payload)
      SensorType.DEBUG -> LOGGER.error { "Error message from device '$id': ${message.payload}" }
      null -> LOGGER.trace { "Message with unknown childSensorId: ${message.childSensorId}. Probably another device on the same node." }
    }
    if (sensorType == SensorType.DEBUG && message.payload.contains("error", true)) {
      notify(DevicePropertyType.STATE, AVAILABILITY_OFFLINE_STATE)
    } else {
      notify(DevicePropertyType.STATE, AVAILABILITY_ONLINE_STATE)
    }
    notify(DevicePropertyType.LAST_PING, LocalDateTime.now().toString())
  }

  enum class SensorType(val id: Int) {
    HUMIDITY(0), TEMPERATURE(1), PRESSURE(2), DEBUG(3);

    companion object {
      fun byId(id: Int): SensorType? = values().find { it.id == id }
    }
  }

  companion object {
    const val CONFIG_MY_SENSORS_NODE_ID = "mySensorsNodeId"
    const val AVAILABILITY_ONLINE_STATE = "ONLINE"
    const val AVAILABILITY_OFFLINE_STATE = "OFFLINE"
  }
}
