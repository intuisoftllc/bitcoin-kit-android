package io.horizontalsystems.bitcoincore.network.peer

import io.horizontalsystems.bitcoincore.BitcoinCore
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Logger

class PeerManager {

    private var peers = ConcurrentHashMap<String, Peer>()
    private val logger = Logger.getLogger("PeerManager")

    val peersCount: Int
        get() = peers.size

    fun add(peer: Peer) {
        peers[peer.host] = peer
    }

    fun remove(peer: Peer) {
        peers.remove(peer.host)
    }

    fun disconnectAll() {
        if(BitcoinCore.loggingEnabled)  logger.info("Disconnecting All.")
        peers.values.forEach { it.close() }
        peers.clear()
    }

    fun connected(): List<Peer> {
        return peers.values.filter { it.connected }
    }

    fun sorted(): List<Peer> {
        return connected().sortedBy { it.connectionTime }
    }

    fun readyPears(): List<Peer> {
        return peers.values.filter { it.connected && it.ready }
    }

    fun hasSyncedPeer(): Boolean {
        return peers.values.any { it.connected && it.synced }
    }

}
