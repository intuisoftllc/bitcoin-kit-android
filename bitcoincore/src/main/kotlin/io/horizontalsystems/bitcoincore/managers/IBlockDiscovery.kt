package io.horizontalsystems.bitcoincore.managers

import io.horizontalsystems.bitcoincore.models.BlockHash
import io.horizontalsystems.bitcoincore.models.PublicKey
import io.reactivex.Single
import kotlinx.coroutines.Job

interface IBlockDiscovery {
    suspend fun discoverBlockHashes(job: Job): Pair<List<PublicKey>, List<BlockHash>>
}
