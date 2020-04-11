package com.mqgateway.core.device

import mu.KotlinLogging
import com.mqgateway.core.gatewayconfig.DeviceType

private val LOGGER = KotlinLogging.logger {}

/**
 * Add listeners before init
 */
abstract class Device(val id: String, val type: DeviceType) {

  private val updateListeners: MutableList<UpdateListener> = mutableListOf()
  private var initialized: Boolean = false

  /**
   * Add listeners before calling this method
   */
  fun init() {
    LOGGER.info { "Initializing device type='$type' id='$id'" }
    if (updateListeners.isEmpty()) {
      LOGGER.warn { "No update listener registered for device id='$id'" }
    }
    initDevice()
    initialized = true
    LOGGER.trace { "Initializing device(id=$id) finished" }
  }

  protected open fun initDevice() {
    // To be implemented by devices extending this class if needed
  }

  fun notify(propertyId: String, newValue: String) {
    LOGGER.trace { "Notifying listeners about property value change ($id.$propertyId = $newValue)" }
    updateListeners.forEach {
      it.valueUpdated(id, propertyId, newValue)
    }
  }

  fun addListener(updateListener: UpdateListener) {
    updateListeners.add(updateListener)
  }

  open fun changeState(propertyId: String, newValue: String) {
    throw UnsupportedStateChangeException(id, propertyId)
  }
}

class UnsupportedStateChangeException(deviceId: String, propertyId: String): Exception("deviceId=$deviceId, propertyId=$propertyId")