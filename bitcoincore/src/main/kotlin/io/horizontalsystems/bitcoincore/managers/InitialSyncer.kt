package io.horizontalsystems.bitcoincore.managers

import com.intuisoft.plaid.common.coroutines.PlaidScope
import com.intuisoft.plaid.common.util.extensions.remove
import io.horizontalsystems.bitcoincore.core.IPublicKeyManager
import io.horizontalsystems.bitcoincore.core.IStorage
import io.horizontalsystems.bitcoincore.models.BlockHash
import io.horizontalsystems.bitcoincore.models.PublicKey
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArrayList
import java.util.logging.Logger

class InitialSyncer(
    private val storage: IStorage,
    private val blockDiscovery: IBlockDiscovery,
    private val publicKeyManager: IPublicKeyManager,
    private val multiAccountPublicKeyFetcher: IMultiAccountPublicKeyFetcher?
) {

    interface Listener {
        fun onSyncSuccess()
        fun onSyncFailed(error: Throwable)
    }

    var listener: Listener? = null

    private val logger = Logger.getLogger("InitialSyncer")
    private var syncJob: Job? = null

    fun terminate() {
        syncJob?.cancel()
        syncJob = null
    }

    fun sync() {
        syncJob = PlaidScope.applicationScope.launch(Dispatchers.IO) {
            runDiscovery(this.coroutineContext.job)
            syncJob = null
        }
    }

    private suspend fun runDiscovery(job: Job) {
        try {
            blockDiscovery.discoverBlockHashes(job).let { (publicKeys, blockHashes) ->
                val sortedUniqueBlockHashes = blockHashes.distinctBy { it.height }.sortedBy { it.height }
                handle(job, publicKeys, sortedUniqueBlockHashes)
            }
        } catch (e: Throwable) {
            handleError(e)
        }
    }

    private suspend fun handle(job: Job, keys: List<PublicKey>, blockHashes: List<BlockHash>) {
        publicKeyManager.addKeys(keys)

        if (multiAccountPublicKeyFetcher != null) {
            if (blockHashes.isNotEmpty()) {
                storage.addBlockHashes(blockHashes)
                multiAccountPublicKeyFetcher.increaseAccount()
                runDiscovery(job)
            } else {
                handleSuccess()
            }
        } else {
            storage.addBlockHashes(blockHashes)
            handleSuccess()
        }
    }

    private fun handleSuccess() {
        listener?.onSyncSuccess()
    }

    private fun handleError(error: Throwable) {
        logger.severe("Initial Sync Error: ${error.message}")

        listener?.onSyncFailed(error)
    }
}
