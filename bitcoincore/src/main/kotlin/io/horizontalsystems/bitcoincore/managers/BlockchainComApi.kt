package io.horizontalsystems.bitcoincore.managers

import android.net.ConnectivityManager
import android.util.Log
import com.eclipsesource.json.JsonValue
import com.intuisoft.plaid.common.CommonService
import com.intuisoft.plaid.common.util.Group
import com.intuisoft.plaid.common.util.extensions.splitIntoGroupOf
import io.horizontalsystems.bitcoincore.BitcoinCore
import io.horizontalsystems.bitcoincore.core.IInitialSyncApi
import java.lang.Integer.min
import java.util.concurrent.Callable
import java.util.concurrent.Executors

class BlockchainComApi : IInitialSyncApi {
    private fun getTransactions(addresses: List<String>): List<TransactionResponse> {
        val transactions = mutableListOf<TransactionResponse>()

        addresses.forEach { address ->
            if(BitcoinCore.loggingEnabled)  {
                Log.i(BlockchainComApi::class.java.simpleName,"Request transactions for ${addresses.size} addresses: [${addresses.first()}, ...]")
            }

            var result = CommonService.getApiRepositoryInstance().getAddressTransactions(address, false)

            if(BitcoinCore.loggingEnabled)  {
                if(result.isNotEmpty())
                    Log.i(BlockchainComApi::class.java.simpleName,"Got transactions for requested address: $address")
            }

            result.forEach {
                val height = it.status.height ?: return@forEach
                val outputs = it.outputs.map { output ->
                    TransactionOutputResponse(
                        output.script,
                        output.address
                    )
                }
                val response = TransactionResponse(
                    height,
                    outputs
                )
                transactions.add(
                    response
                )
            }
        }

        return transactions
    }

    private fun blocks(heights: List<Int>): List<BlockResponse> {
        val response = mutableListOf<BlockResponse>()

        heights.forEach {
            val hash = CommonService.getApiRepositoryInstance().getHashForHeight(it, false)

            if(hash != null) {
                response.add(
                    BlockResponse(
                        it,
                        hash
                    )
                )
            } else {
                response.clear()
                return emptyList()
            }
        }

        return response
    }

    private fun getItems(addresses: List<String>): List<TransactionItem> {
        val transactionResponses = getTransactions(addresses)
        if (transactionResponses.isEmpty()) return listOf()

        val blockHeights = transactionResponses.map { it.blockHeight }.toSet().toList()
        val blocks = blocks(blockHeights)

        return transactionResponses.mapNotNull { transactionResponse ->
            val blockHash = blocks.firstOrNull { it.height == transactionResponse.blockHeight } ?: return@mapNotNull null

            val outputs = transactionResponse.outputs.mapNotNull { output ->
                val address = output.address ?: return@mapNotNull null

                TransactionOutputItem(output.script, address)
            }

            TransactionItem(blockHash.hash, transactionResponse.blockHeight, outputs)
        }
    }

    private fun getAllItems(allAddresses: List<String>): List<TransactionItem> {
        if(BitcoinCore.loggingEnabled)
            Log.i(BlockchainComApi::class.java.simpleName, "getting data for: ${allAddresses.size}, chunks: ${allAddresses.splitIntoGroupOf(addressesLimit).size}")

        return allAddresses.splitIntoGroupOf(addressesLimit).flatMapIndexed { index: Int, group: Group<String> ->
            if(BitcoinCore.loggingEnabled)
                Log.i(BlockchainComApi::class.java.simpleName, "chunk #$index")

            val addresses = group.items
            getItems(addresses)
        }
    }

    override fun getAllTransactions(addresses: List<String>): List<TransactionItem> {
        return getAllItems(addresses)
    }

    data class TransactionResponse(
        val blockHeight: Int,
        val outputs: List<TransactionOutputResponse>
    )

    data class TransactionOutputResponse(
        val script: String,
        val address: String?
    )

    data class BlockResponse(
        val height: Int,
        val hash: String
    )

    companion object {
        private const val paginationLimit = 100
        private const val addressesLimit = 50
    }

}
