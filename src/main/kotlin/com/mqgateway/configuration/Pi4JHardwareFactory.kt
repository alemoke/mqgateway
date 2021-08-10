package com.mqgateway.configuration

import com.mqgateway.core.hardware.MqExpanderPinProvider
import com.mqgateway.core.hardware.MqMcpExpanders
import com.mqgateway.core.hardware.MqSerial
import com.mqgateway.core.hardware.pi4j.Pi4JExpanderPinProvider
import com.mqgateway.core.hardware.pi4j.Pi4JGpioController
import com.mqgateway.core.hardware.pi4j.Pi4JMcpExpanders
import com.mqgateway.core.hardware.pi4j.Pi4JSerial
import com.mqgateway.core.pi4j.Pi4jConfigurer
import com.mqgateway.core.utils.Pi4JSystemInfoProvider
import com.mqgateway.core.utils.SystemInfoProvider
import com.pi4j.io.gpio.GpioFactory
import com.pi4j.io.i2c.I2CBus
import com.pi4j.io.serial.SerialFactory
import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Requirements
import io.micronaut.context.annotation.Requires
import javax.inject.Named
import javax.inject.Singleton

@Factory
@Requires(property = "gateway.system.platform", notEquals = "SIMULATED")
internal class Pi4JHardwareFactory {

  @Singleton
  fun mcpExpanders(gatewaySystemProperties: GatewaySystemProperties): MqMcpExpanders {
    val mcpPorts: List<Int> = gatewaySystemProperties.components.mcp23017.getPorts().map { it.toInt(16) }
    return Pi4JMcpExpanders(I2CBus.BUS_0, mcpPorts)
  }

  @Singleton
  fun expanderPinProvider(gatewaySystemProperties: GatewaySystemProperties, mcpExpanders: MqMcpExpanders): MqExpanderPinProvider {
    Pi4jConfigurer.setup(gatewaySystemProperties.platform)
    val gpio = Pi4JGpioController(GpioFactory.getInstance())
    return Pi4JExpanderPinProvider(gpio, mcpExpanders)
  }

  @Singleton
  fun systemInfoProvider(): SystemInfoProvider {
    return Pi4JSystemInfoProvider()
  }

  @Singleton
  @Requirements(
    Requires(property = "gateway.system.components.serial.enabled", value = "true"),
    Requires(property = "gateway.system.components.my-sensors.enabled", value = "false")
  )
  fun serial(gatewaySystemProperties: GatewaySystemProperties): MqSerial {
    val serial = Pi4JSerial(SerialFactory.createInstance())
    serial.open(gatewaySystemProperties.components.serial.device, gatewaySystemProperties.components.serial.baud)
    return serial
  }

  @Singleton
  @Named("mySensorsSerial")
  @Requirements(
    Requires(property = "gateway.system.components.serial.enabled", value = "false"),
    Requires(property = "gateway.system.components.my-sensors.enabled", value = "true")
  )
  fun mySensorsSerial(gatewaySystemProperties: GatewaySystemProperties): MqSerial {
    val serial = Pi4JSerial(SerialFactory.createInstance())
    serial.open(gatewaySystemProperties.components.mySensors.serialDevice, 115200)
    return serial
  }
}
