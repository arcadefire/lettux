package io.github.arcadefire.lettuce.core

import kotlinx.coroutines.Job

interface ActionContext<S : State> {
    val state: S
    fun send(action: Action): Job
    fun commit(state: S)
    fun <SLICE : State> slice(
        stateToSlice: (S) -> SLICE,
        sliceToState: (S, SLICE) -> S,
    ): ActionContext<SLICE>
}
