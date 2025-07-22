package io.github.arcadefire.lettuce.android

import io.github.arcadefire.lettuce.core.ActionHandler
import io.github.arcadefire.lettuce.core.Middleware
import io.github.arcadefire.lettuce.core.State
import io.github.arcadefire.lettuce.core.Store
import io.github.arcadefire.lettuce.core.Subscription
import io.github.arcadefire.lettuce.factory.createStore
import kotlinx.coroutines.CoroutineScope

fun interface StoreCreator<S : State> {
    fun create(scope: CoroutineScope): Store<S>
}

fun <S : State> storeCreator(
    initialState: S,
    actionHandler: ActionHandler<S>,
    middlewares: List<Middleware> = emptyList(),
    subscription: Subscription<S>? = null,
): StoreCreator<S> = StoreCreator { scope ->
    createStore(
        initialState = initialState,
        actionHandler = actionHandler,
        middlewares = middlewares,
        subscription = subscription,
        storeScope = scope
    )
}
