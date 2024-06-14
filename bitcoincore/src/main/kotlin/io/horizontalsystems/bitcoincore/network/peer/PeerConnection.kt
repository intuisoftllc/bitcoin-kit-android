package io.horizontalsystems.bitcoincore.network.peer

import io.horizontalsystems.bitcoincore.BitcoinCore
import io.horizontalsystems.bitcoincore.io.BitcoinInput
import io.horizontalsystems.bitcoincore.network.Network
import io.horizontalsystems.bitcoincore.network.messages.IMessage
import io.horizontalsystems.bitcoincore.network.messages.NetworkMessageParser
import io.horizontalsystems.bitcoincore.network.messages.NetworkMessageSerializer
import io.horizontalsystems.bitcoincore.utils.NetworkUtils
import io.horizontalsystems.bitcoinutils.BitcoinScope
import kotlinx.coroutines.*
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.Logger

class PeerConnection(
        private val host: String,
        private val network: Network,
        private val listener: Listener,
        private val networkMessageParser: NetworkMessageParser,
        private val networkMessageSerializer: NetworkMessageSerializer) {

    interface Listener {
        fun socketConnected(address: InetAddress)
        fun disconnected(e: Exception? = null)
        fun onTimePeriodPassed() // didn't find better name
        fun onMessage(message: IMessage)
    }

    private val socket = NetworkUtils.createSocket()

    private val logger = Logger.getLogger("Peer[$host]")
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null
    private var disconnectError: Exception? = null

    private var isRunning = AtomicBoolean(false)

    suspend fun run(onRunning: () -> Unit) {
        isRunning.set(true)
        // connect:
        try {
            socket.connect(InetSocketAddress(host, network.port), 10000)
            socket.soTimeout = 10000

            outputStream = socket.getOutputStream()
            val inputStream = socket.getInputStream()
            val bitcoinInput = BitcoinInput(inputStream)

            if(BitcoinCore.loggingEnabled)  logger.info("Socket $host connected.")

            listener.socketConnected(socket.inetAddress)

            this.inputStream = inputStream
            onRunning()

            // loop:
            while (isRunning.get()) {
                listener.onTimePeriodPassed()

                delay(1000)

                // try receive message:
                while (isRunning.get() && inputStream.available() > 0) {
                    val parsedMsg = networkMessageParser.parseMessage(bitcoinInput)
                    if(BitcoinCore.loggingEnabled)  logger.info("<= $parsedMsg")
                    listener.onMessage(parsedMsg)
                }
            }
        } catch (e: Exception) {
            close(e)
        } finally {
            outputStream?.close()
            outputStream = null

            inputStream?.close()
            inputStream = null

            listener.disconnected(disconnectError)
        }
    }

    @Synchronized
    fun close(error: Exception?) {
        synchronized(this) {
            disconnectError = error
            isRunning.set(false)
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun sendMessage(message: IMessage) {
        BitcoinScope.applicationScope.launch(Dispatchers.IO) {
            synchronized(this@PeerConnection) {
                if (isRunning.get()) {
                    try {
                        if (BitcoinCore.loggingEnabled) logger.info("=> $message")
                        outputStream?.write(networkMessageSerializer.serialize(message))
                    } catch (e: Exception) {
                        close(e)
                    }
                }
            }
        }
    }

}
