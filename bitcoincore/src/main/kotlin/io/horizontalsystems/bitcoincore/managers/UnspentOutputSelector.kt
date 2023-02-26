package io.horizontalsystems.bitcoincore.managers

import io.horizontalsystems.bitcoincore.storage.UnspentOutput
import io.horizontalsystems.bitcoincore.transactions.TransactionSizeCalculator
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptType

class UnspentOutputSelector(private val calculator: TransactionSizeCalculator, private val unspentOutputProvider: IUnspentOutputProvider, private val outputsLimit: Int? = null) : IUnspentOutputSelector {

    override fun select(
        unspentOutputAddresses: List<Pair<Long, String>>,
        value: Long,
        feeRate: Int,
        outputType: ScriptType,
        changeType: ScriptType,
        senderPay: Boolean,
        dust: Int,
        pluginDataOutputSize: Int
    ): SelectedUnspentOutputInfo {
        if (value <= dust) {
            throw SendValueErrors.Dust
        }


        val spendableUTXOs = unspentOutputProvider.getSpendableUtxo()
        val unspentOutputs = spendableUTXOs.filter { utxo -> unspentOutputAddresses.find { utxo.output.address == it.second && utxo.output.value == it.first } != null }

        return selectOutputs(unspentOutputs, value, feeRate, outputType, changeType, senderPay, dust, pluginDataOutputSize)
    }

    override fun select(value: Long, feeRate: Int, outputType: ScriptType, changeType: ScriptType, senderPay: Boolean, dust: Int, pluginDataOutputSize: Int): SelectedUnspentOutputInfo {
        if (value <= dust) {
            throw SendValueErrors.Dust
        }

        val unspentOutputs = unspentOutputProvider.getSpendableUtxo()
        return selectOutputs(unspentOutputs, value, feeRate, outputType, changeType, senderPay, dust, pluginDataOutputSize)
    }

    private fun selectOutputs(
        unspentOutputs: List<UnspentOutput>,
        value: Long,
        feeRate: Int,
        outputType: ScriptType,
        changeType: ScriptType,
        senderPay: Boolean,
        dust: Int,
        pluginDataOutputSize: Int
    ): SelectedUnspentOutputInfo {
        if (value <= dust) {
            throw SendValueErrors.Dust
        }

        if (unspentOutputs.isEmpty()) {
            throw SendValueErrors.EmptyOutputs
        }

        val sortedOutputs = unspentOutputs.sortedWith(compareByDescending<UnspentOutput> {
            it.output.failedToSpend
        }.thenBy {
            it.output.value
        })

        val selectedOutputs = mutableListOf<UnspentOutput>()
        var totalValue = 0L
        var maxValue = sortedOutputs.sumOf { it.output.value }
        var recipientValue = 0L
        var sentValue = 0L
        var fee: Long

        for (unspentOutput in sortedOutputs) {
            selectedOutputs.add(unspentOutput)
            totalValue += unspentOutput.output.value

            outputsLimit?.let {
                if (selectedOutputs.size > it) {
                    val outputToExclude = selectedOutputs.first()
                    selectedOutputs.removeAt(0)
                    totalValue -= outputToExclude.output.value
                }
            }

            fee = calculator.transactionSize(selectedOutputs.map { it.output }, listOf(outputType), pluginDataOutputSize) * feeRate

            recipientValue = if (senderPay) value else value - fee
            sentValue = if (senderPay) value + fee else value

            if(totalValue >= sentValue) {
                if(recipientValue < dust) {
                    // Here senderPay is false, because otherwise "dust" exception would throw far above.
                    // Adding more UTXOs will make fee even greater, making recipientValue even less and dust anyway
                    throw SendValueErrors.Dust
                } else if(sentValue >= recipientValue) {
                    break
                }
            }
        }

        // if all outputs are selected and total value less than needed throw error
        if (totalValue < sentValue) {
            throw SendValueErrors.InsufficientUnspentOutputs
        }

        if(totalValue != maxValue || (totalValue - sentValue) > dust) {
            val feeWithChangeAddress = calculator.transactionSize(
                selectedOutputs.map { it.output },
                listOf(outputType, changeType),
                pluginDataOutputSize
            ) * feeRate
            val sendValueWithChange =
                if (senderPay) value + feeWithChangeAddress else value - feeWithChangeAddress

            // if selected UTXOs total value >= recipientValue(toOutput value) + fee(for transaction with change output) + dust(minimum changeOutput value)
            if (totalValue >= sendValueWithChange) {
                // totalValue is too much, we must have change output
                if (sendValueWithChange <= dust) {
                    throw SendValueErrors.Dust
                }

                return SelectedUnspentOutputInfo(
                    selectedOutputs,
                    value,
                    totalValue - sendValueWithChange
                )
            }
        }

        // No change needed
        return SelectedUnspentOutputInfo(selectedOutputs, recipientValue, null)
    }
}
