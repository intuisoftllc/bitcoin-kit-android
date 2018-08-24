package bitcoin.wallet.kit.messages

import bitcoin.wallet.kit.models.Block
import bitcoin.walllet.kit.common.io.BitcoinInput
import java.io.ByteArrayInputStream
import java.io.IOException
import java.util.*

/**
 * The 'block' message consists of a single serialized block.
 */
class BlockMessage : Message {

    lateinit var block: Block

    constructor() : super("block")

    @Throws(IOException::class)
    constructor(payload: ByteArray) : super("block") {
        BitcoinInput(ByteArrayInputStream(payload)).use { input ->
            block = Block(input)
        }
    }

    override fun getPayload(): ByteArray {
        return this.block.toByteArray()
    }

    /**
     * Validate block hash.
     */
    fun validateHash(): Boolean {
        val merkleHash = block.calculateMerkleHash()
        if (!Arrays.equals(merkleHash, block.header.merkleHash)) {
            return false
        }

        // TODO: validate bits:
        return true
    }

    override fun toString(): String {
        return "BlockMessage(txnCount=${block.txns.size})"
    }
}