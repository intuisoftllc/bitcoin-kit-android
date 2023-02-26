package io.horizontalsystems.bitcoincore.transactions

import com.intuisoft.plaid.common.coroutines.PlaidScope
import com.intuisoft.plaid.common.util.Constants
import kotlinx.coroutines.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class TransactionSendTimer(private val period: Long) {

    interface Listener {
        fun onTimePassed()
    }

    var listener: Listener? = null
    private var task: Job? = null

    @Synchronized
    fun startIfNotRunning() {
        if (task == null) {
            task = PlaidScope.applicationScope.launch(Dispatchers.IO) {
                while(true) {
                    this.ensureActive()
                    listener?.onTimePassed()
                    delay(period * Constants.Time.MILLS_PER_SEC)
                }
            }
        }
    }

    @Synchronized
    fun stop() {
        task?.let {
            it.cancel()
            task = null
        }
    }

}
