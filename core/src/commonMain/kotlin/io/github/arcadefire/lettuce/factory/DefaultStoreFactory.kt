package io.github.arcadefire.lettuce.factory

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import io.github.arcadefire.lettuce.DefaultActionContext
import io.github.arcadefire.lettuce.DefaultStore
import io.github.arcadefire.lettuce.core.ActionHandler
import io.github.arcadefire.lettuce.core.Chain
import io.github.arcadefire.lettuce.core.Middleware
import io.github.arcadefire.lettuce.core.Outcome
import io.github.arcadefire.lettuce.core.State
import io.github.arcadefire.lettuce.core.Store
import io.github.arcadefire.lettuce.core.Subscription

fun <STATE : State> createStore(
    initialState: STATE,
    actionHandler: ActionHandler<STATE>,
    middlewares: List<Middleware> = emptyList(),
    subscription: Subscription<STATE>? = null,
    storeScope: CoroutineScope,
): Store<STATE> {
    val statesFlow = MutableStateFlow(initialState)

    return DefaultStore(
        states = statesFlow,
        storeScope = storeScope,
        middlewares = middlewares,
        actionHandler = actionHandler,
    ).also { store ->
        subscription?.apply {
            subscribe(store.states)
                .onEach(store::send)
                .launchIn(storeScope)
        }
    }
}

