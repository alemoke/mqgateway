package com.mqgateway.core.hardware.simulated

import com.mqgateway.core.hardware.MqSerial
import com.mqgateway.core.hardware.MqSerialDataEvent
import com.mqgateway.core.hardware.MqSerialDataEventListener
import mu.KotlinLogging

private val LOGGER = KotlinLogging.logger {}

class SimulatedSerial : MqSerial {

  private var opened = false
  private val eventListeners: MutableList<MqSerialDataEventListener> = mutableListOf()
  private val writtenMessages: MutableList<String> = mutableListOf()

  override fun open(devicePath: String, baudRate: Int) {
    LOGGER.debug { "Serial opened with devicePath: $devicePath and baudRate: $baudRate" }
    opened = true
  }

  override fun addListener(serialDataEventListener: MqSerialDataEventListener) {
    LOGGER.debug { "Serial data event listener added" }
    eventListeners.add(serialDataEventListener)
  }

  override fun write(message: String) {
    LOGGER.debug { "Serial message sent: $message" }
    writtenMessages.add(message)
  }

  fun sendFakeMessage(message: String) {
    eventListeners.forEach {
      it.dataReceived(SimulatedSerialDataEvent(message))
    }
  }

  fun getMessagesWrittenToSerial(): List<String> {
    return writtenMessages
  }
}

class SimulatedSerialDataEvent(private val receivedMessage: String) : MqSerialDataEvent {
  override fun getAsciiString(): String = receivedMessage
}
