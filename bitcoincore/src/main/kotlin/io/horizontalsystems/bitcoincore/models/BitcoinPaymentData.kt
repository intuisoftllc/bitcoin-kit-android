package io.horizontalsystems.bitcoincore.models

import java.text.DecimalFormat

data class BitcoinPaymentData(
        val address: String,
        val version: String? = null,
        val amount: Double? = null,
        val label: String? = null,
        val message: String? = null,
        val parameters: MutableMap<String, String>? = null) {

    val uriPaymentAddress: String
        get() {
            var uriAddress = address
            var seperator = "?"
            version?.let {
                uriAddress += ";version=$version"
            }
            amount?.let {
                uriAddress += "?amount=${format(it)}"
                seperator = "&"
            }
            label?.let {
                uriAddress += "${seperator}label=$label"
                seperator = "&"
            }
            message?.let {
                uriAddress += "${seperator}message=$message"
                seperator = "&"
            }
            parameters?.let {
                for ((name, value) in it) {
                    uriAddress += "${seperator}$name=$value"
                    seperator = "&"
                }
            }

            return uriAddress
        }

    companion object {
        fun toBitcoinPaymentUri(info: BitcoinPaymentData): String {
            return "bitcoin:${info.uriPaymentAddress}"
        }

        fun format(value: Double): String? {
            val df = DecimalFormat("###,###,###,###,###,###.########")
            return df.format(value)
        }

    }
}
