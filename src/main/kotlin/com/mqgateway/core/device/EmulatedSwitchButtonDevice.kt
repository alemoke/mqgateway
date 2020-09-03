package com.mqgateway.core.device

import com.mqgateway.core.gatewayconfig.DeviceType
import com.pi4j.io.gpio.GpioPinDigitalOutput
import com.pi4j.io.gpio.PinState
import mu.KotlinLogging
import kotlin.concurrent.thread

private val LOGGER = KotlinLogging.logger {}

class EmulatedSwitchButtonDevice(id: String, pin: GpioPinDigitalOutput) : DigitalOutputDevice(id, DeviceType.EMULATED_SWITCH, pin) {

  private fun changeState(newState: EmulatedSwitchState) {
    val newPinState = if (newState == EmulatedSwitchState.PRESSED) PRESSED_STATE else RELEASED_STATE
    pin.state = newPinState
  }

  override fun changeState(propertyId: String, newValue: String) {
    LOGGER.debug { "Changing state on emulated switch $id to $newValue" }
    if (newValue == "PRESSED") {
      changeState(EmulatedSwitchState.PRESSED)
      notify(propertyId, newValue)
      thread {
        Thread.sleep(TIME_BEFORE_RELEASE_IN_MS)
        changeState(EmulatedSwitchState.RELEASED)
        notify(propertyId, "RELEASED")
      }
    }
  }

  enum class EmulatedSwitchState {
    PRESSED, RELEASED
  }

  companion object {
    val PRESSED_STATE = PinState.LOW
    val RELEASED_STATE = PinState.getInverseState(PRESSED_STATE)!!
    const val TIME_BEFORE_RELEASE_IN_MS = 500L
  }
}