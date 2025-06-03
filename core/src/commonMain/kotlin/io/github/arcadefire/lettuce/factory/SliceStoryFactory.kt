package io.github.arcadefire.lettuce.factory

import io.github.arcadefire.lettuce.core.ActionHandler
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import io.github.arcadefire.lettuce.core.Middleware
import io.github.arcadefire.lettuce.core.SliceableStore
import io.github.arcadefire.lettuce.core.State
import io.github.arcadefire.lettuce.core.Store
import io.github.arcadefire.lettuce.core.Subscription
import kotlinx.coroutines.CoroutineScope

fun <STATE : State, SLICE : State> sliceStore(
    store: Store<STATE>,
    stateToSlice: (STATE) -> SLICE,
    sliceToState: (STATE, SLICE) -> STATE,
    middlewares: List<Middleware> = emptyList(),
    subscription: Subscription<SLICE>? = null,
    actionHandler: ActionHandler<SLICE>? = null,
    sliceScope: CoroutineScope,
): Store<SLICE> {
    return (store as SliceableStore<STATE>).slice(
        sliceToState = sliceToState,
        stateToSlice = stateToSlice,
        middlewares = middlewares,
        sliceScope = sliceScope,
        actionHandler = actionHandler,
    ).also { slicedStore ->
        subscription
            ?.subscribe(slicedStore.states)
            ?.onEach(store::send)
            ?.launchIn(sliceScope)
    }
}
