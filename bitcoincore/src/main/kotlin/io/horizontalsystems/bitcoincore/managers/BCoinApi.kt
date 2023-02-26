package io.horizontalsystems.bitcoincore.managers

import com.eclipsesource.json.Json
import com.eclipsesource.json.JsonObject
import com.intuisoft.plaid.common.CommonService
import io.horizontalsystems.bitcoincore.BitcoinCore
import io.horizontalsystems.bitcoincore.core.IInitialSyncApi
import java.util.logging.Logger

class BCoinApi() : IInitialSyncApi {
    private val logger = Logger.getLogger("BCoinApi")

    override fun getAllTransactions(addresses: List<String>): List<TransactionItem> {
        val transactions = mutableListOf<TransactionItem>()

        addresses.forEach {
            if(BitcoinCore.loggingEnabled)  logger.info("Request transactions for ${addresses.size} addresses: [${addresses.first()}, ...]")
            val result = CommonService.getApiRepositoryInstance().getAddressTransactions(it, true)

            if(BitcoinCore.loggingEnabled)  {
                if(result != null)
                    logger.info("Got transactions for requested address: $it")
                else
                    logger.info("Failed to get transactions for requested address: $it")
            }

            result.forEach {
                val blockHash = it.status.blockHash ?: return@forEach
                val height = it.status.height ?: return@forEach

                transactions.add(
                    TransactionItem(
                        blockHash = blockHash,
                        blockHeight = height,
                        txOutputs = it.outputs.map { output ->
                            TransactionOutputItem(
                                script = output.script,
                                address = output.address
                            )
                        }
                    )
                )
            }
        }

        return transactions
    }

}
