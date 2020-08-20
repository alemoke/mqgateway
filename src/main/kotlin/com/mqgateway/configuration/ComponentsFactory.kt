package com.mqgateway.configuration

import com.mqgateway.core.device.DeviceFactory
import com.mqgateway.core.device.DeviceRegistry
import com.mqgateway.core.gatewayconfig.ConfigLoader
import com.mqgateway.core.gatewayconfig.Gateway
import com.mqgateway.core.mcpexpander.McpExpanders
import com.mqgateway.core.mcpexpander.McpExpandersFactory
import com.mqgateway.core.mcpexpander.Pi4JExpanderPinProvider
import com.mqgateway.core.pi4j.Pi4jConfigurer
import com.mqgateway.homie.HomieDevice
import com.mqgateway.homie.gateway.GatewayHomieReceiver
import com.mqgateway.homie.gateway.HomieDeviceFactory
import com.mqgateway.homie.mqtt.HiveMqttClientFactory
import com.mqgateway.homie.mqtt.MqttClientFactory
import com.pi4j.io.gpio.GpioFactory
import com.pi4j.io.i2c.I2CBus
import com.pi4j.io.serial.Baud
import com.pi4j.io.serial.Serial
import com.pi4j.io.serial.SerialConfig
import com.pi4j.io.serial.SerialFactory
import io.micronaut.context.annotation.Factory
import javax.inject.Singleton

@Factory
internal class ComponentsFactory {

  @Singleton
  fun gatewayConfiguration(gatewayApplicationProperties: GatewayApplicationProperties): Gateway {
    val gateway = ConfigLoader.load(gatewayApplicationProperties.configPath)
    return gateway
  }

  @Singleton
  fun mcpExpanders(gatewaySystemProperties: GatewaySystemProperties): McpExpanders {
    Pi4jConfigurer.setup(gatewaySystemProperties.platform)
    val mcpPorts: List<Int> = gatewaySystemProperties.components.mcp23017.ports.map { it.toInt(16) }
    return McpExpandersFactory.create(I2CBus.BUS_0, mcpPorts)
  }

  @Singleton
  fun deviceRegistry(mcpExpanders: McpExpanders, gateway: Gateway): DeviceRegistry {
    val gpioController = GpioFactory.getInstance()
    val deviceFactory = DeviceFactory(Pi4JExpanderPinProvider(gpioController, mcpExpanders))
    return DeviceRegistry(deviceFactory.createAll(gateway))
  }

  @Singleton
  fun mqttClientFactory(gateway: Gateway): MqttClientFactory = HiveMqttClientFactory(gateway.mqttHostname)

  @Singleton
  fun homeDevice(
    mqttClientFactory: MqttClientFactory,
    gatewayApplicationProperties: GatewayApplicationProperties,
    gatewaySystemProperties: GatewaySystemProperties,
    gateway: Gateway
  ): HomieDevice {

    return HomieDeviceFactory(mqttClientFactory, gatewayApplicationProperties.appVersion)
      .toHomieDevice(gateway, gatewaySystemProperties.networkAdapter)
  }

  @Singleton
  fun homieReceiver(deviceRegistry: DeviceRegistry) = GatewayHomieReceiver(deviceRegistry)

  @Singleton
  fun serial(gatewaySystemProperties: GatewaySystemProperties): Serial {
    val serial = SerialFactory.createInstance()
    serial.open(SerialConfig()
      .device(gatewaySystemProperties.components.serial.device)
      .baud(Baud.getInstance(gatewaySystemProperties.components.serial.baud))
    )
    return serial
  }
}
