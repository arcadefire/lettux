package org.lettux.core

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow

interface Store<STATE> {
    val states: StateFlow<STATE>

    fun send(action: Action, withDispatcher: CoroutineDispatcher = Dispatchers.Main): Job

    fun <SLICE : Any> slice(
        stateToSlice: (STATE) -> SLICE,
        sliceToState: (STATE, SLICE) -> STATE,
        middlewares: List<Middleware> = emptyList(),
    ): Store<SLICE>
}

val <STATE> Store<STATE>.state: STATE get() = states.value