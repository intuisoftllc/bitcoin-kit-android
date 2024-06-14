package io.horizontalsystems.common.checkpoints

interface CheckpointResultListener {
    fun onResult(results: List<String>)
}