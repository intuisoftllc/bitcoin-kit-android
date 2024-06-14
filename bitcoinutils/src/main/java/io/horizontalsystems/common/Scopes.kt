package io.horizontalsystems.bitcoinutils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

object BitcoinScope {
    val applicationScope = CoroutineScope(SupervisorJob())

    val MainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
}