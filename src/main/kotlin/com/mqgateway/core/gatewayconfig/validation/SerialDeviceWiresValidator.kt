package com.mqgateway.core.gatewayconfig.validation

import com.mqgateway.configuration.GatewaySystemProperties
import com.mqgateway.core.gatewayconfig.DeviceConfig
import com.mqgateway.core.gatewayconfig.DeviceType
import com.mqgateway.core.gatewayconfig.Gateway
import javax.inject.Singleton

@Singleton
class SerialDeviceWiresValidator : GatewayValidator {
  override fun validate(gateway: Gateway, systemProperties: GatewaySystemProperties): List<ValidationFailureReason> {
    val devices: List<DeviceConfig> = gateway.rooms
      .flatMap { room -> room.points }
      .flatMap { point -> point.devices }
      .filter { device -> device.type in listOf(DeviceType.BME280, DeviceType.DHT22) }

    return devices
      .filter { device -> device.wires.size != 2 || device.wires[0] == device.wires[1] }
      .map { WrongWiresConfigurationForSerialDevice(it) }
  }

  class WrongWiresConfigurationForSerialDevice(val device: DeviceConfig) : ValidationFailureReason() {

    override fun getDescription(): String {
      return "Following serial-based device should have two different wires configured: ${device.name}"
    }
  }
}
