package io.horizontalsystems.bitcoincore.network.peer

import io.horizontalsystems.bitcoincore.BitcoinCore
import io.horizontalsystems.bitcoincore.core.IPeerAddressManager
import io.horizontalsystems.bitcoinutils.BitcoinScope
import kotlinx.coroutines.*
import java.net.Inet6Address
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.logging.Logger

class PeerDiscover(private val peerAddressManager: IPeerAddressManager) {

    private val logger = Logger.getLogger("PeerDiscover")

    fun lookup(dnsList: List<String>) {
        if(BitcoinCore.loggingEnabled)  logger.info("Lookup peers from DNS seed...")

        BitcoinScope.applicationScope.launch(Dispatchers.IO) {
            dnsList.forEach { host ->
                try {
                    val ips = InetAddress
                        .getAllByName(host)
                        .filter { it !is Inet6Address }
                        .map { it.hostAddress }

                    if (BitcoinCore.loggingEnabled) logger.info("Fetched ${ips.size} peer addresses from host: $host")
                    peerAddressManager.addIps(ips)
                } catch (e: UnknownHostException) {
                    logger.warning("Cannot look up host: $host")
                }
            }
        }
    }
}
