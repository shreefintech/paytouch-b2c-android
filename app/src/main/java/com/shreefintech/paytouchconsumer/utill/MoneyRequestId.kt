package com.shreefintech.paytouchconsumer.utill

import java.util.UUID

/**
 * `request_id` (idempotency key) for money calls — wallet withdrawal, earning lock / withdraw.
 * The same id is returned while one attempt's outcome is unknown (timeout / 5xx), so a retry can't pay twice;
 * [clear] once the server gave a definite answer. Not a transaction_id — the server still issues that.
 */
class MoneyRequestId {
    private var attempt: String? = null
    private var id: String? = null

    fun forAttempt(attempt: String): String =
        id?.takeIf { this.attempt == attempt }
            ?: UUID.randomUUID().toString().also { this.attempt = attempt; id = it }

    fun clear() {
        attempt = null
        id = null
    }
}
