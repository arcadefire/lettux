package io.github.arcadefire.lettuce.core

import kotlinx.coroutines.flow.Flow

fun interface Subscription<S : State> {
    fun subscribe(states: Flow<S>): Flow<Action>
}
