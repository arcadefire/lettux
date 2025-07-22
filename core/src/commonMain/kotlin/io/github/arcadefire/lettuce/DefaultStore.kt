package io.github.arcadefire.lettuce

import io.github.arcadefire.lettuce.core.Action
import io.github.arcadefire.lettuce.core.ActionHandler
import io.github.arcadefire.lettuce.core.Chain
import io.github.arcadefire.lettuce.core.Middleware
import io.github.arcadefire.lettuce.core.Outcome
import io.github.arcadefire.lettuce.core.SliceableStore
import io.github.arcadefire.lettuce.core.State
import io.github.arcadefire.lettuce.core.Store
import io.github.arcadefire.lettuce.extension.defaultLaunch
import io.github.arcadefire.lettuce.slice.SlicedStatesFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow

internal class DefaultStore<STATE : State>(
    override val states: MutableStateFlow<STATE>,
    override val storeScope: CoroutineScope,
    private val middlewares: List<Middleware>,
    private val actionHandler: ActionHandler<STATE>,
) : Store<STATE>, SliceableStore<STATE> {

    private val chain: Chain = middlewares
        .foldRight(
            Chain { action ->
                val actionContext = DefaultActionContext(
                    sendFunction = ::send,
                    getState = { states.value },
                    setState = { states.value = it },
                )
                val oldState = states.value

                with(actionHandler) {
                    with(actionContext) {
                        handle(action)
                    }
                }

                val newState = states.value
                if (oldState != newState) {
                    Outcome.StateMutated(newState)
                } else {
                    Outcome.NoMutation
                }
            }
        ) { middleware, chain ->
            Chain { action -> middleware.intercept(action, states.value, chain) }
        }

    private val doSend: suspend (Action) -> Outcome = { action ->
        chain.proceed(action)
    }

    override fun send(action: Action): Job = storeScope.defaultLaunch { doSend(action) }

    override fun <SLICE : State> slice(
        stateToSlice: (STATE) -> SLICE,
        sliceToState: (STATE, SLICE) -> STATE,
        middlewares: List<Middleware>,
        sliceScope: CoroutineScope,
        actionHandler: ActionHandler<SLICE>?
    ): Store<SLICE> {
        val sliceHandler = ActionHandler { action ->
            val outcome = this@DefaultStore.chain.proceed(action)

            // If the parent action handler didn't mutate the state for this action,
            // delegate it to the slice action handler (if provided).
            if (outcome is Outcome.NoMutation) {
                actionHandler?.run {
                    handle(action)
                }
            }
        }

        return DefaultStore(
            states = SlicedStatesFlow(states, stateToSlice, sliceToState),
            storeScope = sliceScope,
            middlewares = middlewares,
            actionHandler = sliceHandler,
        )
    }
}
