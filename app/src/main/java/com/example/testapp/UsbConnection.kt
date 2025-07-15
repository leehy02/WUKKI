package com.example.testapp

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import java.io.IOException

class UsbConnection(private val context: Context) {
    private val usbManager: UsbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    private var serialPort: UsbSerialPort? = null
    private var usbDevice: UsbDevice? = null

    fun setupUsbConnection(logCallback: (String) -> Unit) {
        val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)

        if (availableDrivers.isEmpty()) {
            logCallback("No USB serial devices found.")
            return
        }

        for (driver in availableDrivers) {
            val device = driver.device
            when (device.vendorId) {
                0x0403 -> logCallback("FTDI (FT232) detected")  // FTDI 칩셋
                0x10C4 -> logCallback("Silicon Labs (CP210x) detected") // CP210x 칩셋
                else -> logCallback("Unknown USB Serial Device: VendorID=${device.vendorId}")
            }
        }

        val driver = availableDrivers[0]
        val connection = usbManager.openDevice(driver.device)

        if (connection == null) {
            logCallback("Failed to open USB device.")
            return
        }

        serialPort = driver.ports[0] // FTDI & CP2102 둘 다 지원
        serialPort?.apply {
            try {
                open(connection)
                setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
                setDTR(true)
                setRTS(true)
                logCallback("USB device connected at 115200 baud, 8N1")
            } catch (e: IOException) {
                logCallback("Error setting up USB serial connection: ${e.message}")
                close()
            }
        }
    }

    fun sendData(data: ByteArray, logCallback: (String) -> Unit) {
        serialPort?.let {
            try {
                it.write(data, 100)
                logCallback("Data sent successfully: ${data.joinToString(" ") { "%02X".format(it) }}")
            } catch (e: IOException) {
                logCallback("Failed to send data: ${e.message}")
            }
        } ?: logCallback("USB serial port not initialized.")
    }

    fun receiveData(logCallback: (String) -> Unit): ByteArray? {
        serialPort?.let {
            try {
                val buffer = ByteArray(64) // CP2102는 기본적으로 64바이트 버퍼 사용
                val bytesRead = it.read(buffer, 100)
                return if (bytesRead > 0) {
                    val receivedData = buffer.copyOf(bytesRead)
                    logCallback("Data received: ${receivedData.joinToString(" ") { "%02X".format(it) }}")
                    receivedData
                } else {
                    logCallback("No data received.")
                    null
                }
            } catch (e: IOException) {
                logCallback("Error receiving data: ${e.message}")
                return null
            }
        } ?: logCallback("USB serial port not initialized.")
        return null
    }

    fun closeConnection(logCallback: (String) -> Unit) {
        try {
            serialPort?.close()
            logCallback("USB connection closed.")
        } catch (e: IOException) {
            logCallback("Error closing USB connection: ${e.message}")
        }
    }
}
