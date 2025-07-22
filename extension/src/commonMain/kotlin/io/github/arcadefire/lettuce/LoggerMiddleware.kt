package io.github.arcadefire.lettuce

import io.github.arcadefire.lettuce.core.Action
import io.github.arcadefire.lettuce.core.Chain
import io.github.arcadefire.lettuce.core.Middleware
import io.github.arcadefire.lettuce.core.Outcome
import io.github.arcadefire.lettuce.core.State
import kotlinx.atomicfu.atomic

class LoggerMiddleware(private val logger: LogsWriter) : Middleware {

    private val indentationCounter = atomic(0)

    override suspend fun intercept(action: Action, state: State, chain: Chain): Outcome {
        val localIndentation = (0 until indentationCounter.value).joinToString(separator = "") { "\t|" }

        indentationCounter.incrementAndGet()

        logger.writeLog("$localIndentation ⇨ $action")
        val outcome = chain.proceed(action)

        indentationCounter.decrementAndGet()

        when (outcome) {
            is Outcome.StateMutated -> {
                logger.writeLog("$localIndentation \tState: ${outcome.state}")
            }

            Outcome.NoMutation -> {
                logger.writeLog("$localIndentation No state mutation")
            }
        }

        logger.writeLog("$localIndentation ⇦ $action")
        logger.writeLog("\n")

        return outcome
    }
}
