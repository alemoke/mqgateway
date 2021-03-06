package com.mqgateway.core.device

import com.mqgateway.core.gatewayconfig.DevicePropertyType.STATE
import com.mqgateway.core.gatewayconfig.DeviceType
import mu.KotlinLogging

private val LOGGER = KotlinLogging.logger {}

class SingleButtonsGateDevice(
  id: String,
  private val actionButton: EmulatedSwitchButtonDevice,
  private val openReedSwitch: ReedSwitchDevice?,
  private val closedReedSwitch: ReedSwitchDevice?
) : Device(id, DeviceType.GATE) {

  private var state: State = State.UNKNOWN
    private set(value) {
      field = value
      notify(STATE, value.name)
    }

  override fun initProperty(propertyId: String, value: String) {
    if (propertyId != STATE.toString()) {
      LOGGER.warn { "Trying to initialize unsupported property '$id.$propertyId'" }
      return
    }
    state = State.valueOf(value)
  }

  override fun initDevice() {
    super.initDevice()
    actionButton.init(false)
    openReedSwitch?.init(false)
    closedReedSwitch?.init(false)
    if (closedReedSwitch?.isClosed() == true) {
      state = State.CLOSED
    } else if (openReedSwitch?.isClosed() == true) {
      state = State.OPEN
    }
    openReedSwitch?.addListener { _, _, newValue ->
      if (newValue == ReedSwitchDevice.CLOSED_STATE_VALUE) {
        state = State.OPEN
      } else if (hasClosedReedSwitch()) {
        state = State.CLOSING
      } else {
        state = State.CLOSED
      }
    }
    closedReedSwitch?.addListener { _, _, newValue ->
      if (newValue == ReedSwitchDevice.CLOSED_STATE_VALUE) {
        state = State.CLOSED
      } else if (hasOpenReedSwitch()) {
        state = State.OPENING
      } else {
        state = State.OPEN
      }
    }
  }

  override fun change(propertyId: String, newValue: String) {
    if (propertyId != STATE.toString()) {
      LOGGER.error { "Unexpected property change received for device '$id': $propertyId" }
      return
    }

    LOGGER.info { "Changing gate $id state to $newValue" }
    when (Command.valueOf(newValue.toUpperCase())) {
      Command.OPEN -> open()
      Command.CLOSE -> close()
      Command.STOP -> stop()
    }
  }

  fun close() {
    if (isClosedForSure()) {
      LOGGER.debug { "Nothing to be done - gate is closed already" }
    } else if (state == State.CLOSING) {
      LOGGER.debug { "Nothing to be done - gate is closing already" }
    } else if (state == State.OPENING) {
      LOGGER.debug { "Gate is opening - need to stop first" }
      stop()
      close()
    } else {
      LOGGER.debug { "Activating action button to close the gate" }
      actionButton.shortPress(true)
      state = if (hasClosedReedSwitch()) State.CLOSING else State.CLOSED
    }
  }

  fun open() {
    if (isOpenForSure()) {
      LOGGER.debug { "Nothing to be done - gate is open already" }
    } else if (state == State.OPENING) {
      LOGGER.debug { "Nothing to be done - gate is opening already" }
    } else if (state == State.CLOSING) {
      LOGGER.debug { "Gate is closing - need to stop first" }
      stop()
      open()
    } else {
      LOGGER.debug { "Activating action button to open the gate" }
      actionButton.shortPress(true)
      state = if (hasOpenReedSwitch()) State.OPENING else State.OPEN
    }
  }

  fun stop() {
    if (isOpenForSure() || isClosedForSure()) {
      LOGGER.debug { "Nothing to be done - gate is stopped already" }
    } else {
      LOGGER.debug { "Activating action button to stop the gate" }
      actionButton.shortPress(true)
      state = State.OPEN
    }
  }

  private fun hasClosedReedSwitch() = closedReedSwitch != null
  private fun hasOpenReedSwitch() = openReedSwitch != null
  private fun isOpenForSure() = openReedSwitch?.isClosed() == true
  private fun isClosedForSure() = closedReedSwitch?.isClosed() == true

  enum class Command {
    OPEN, CLOSE, STOP
  }

  enum class State {
    OPENING, CLOSING, OPEN, CLOSED, UNKNOWN
  }
}
