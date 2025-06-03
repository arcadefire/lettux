package io.github.arcadefire.lettuce.core

import kotlinx.coroutines.CoroutineScope

/**
 * Represents a store that can be sliced into a more focused sub-store.
 *
 * It provides access to a subset of the state managed by the parent store.
 */
internal interface SliceableStore<STATE : State> {
    /**
     * Creates a slice of the store's state.
     */
    fun <SLICE : State> slice(
        stateToSlice: (STATE) -> SLICE,
        sliceToState: (STATE, SLICE) -> STATE,
        middlewares: List<Middleware> = emptyList(),
        sliceScope: CoroutineScope,
        actionHandler: ActionHandler<SLICE>? = null,
    ): Store<SLICE>
}
