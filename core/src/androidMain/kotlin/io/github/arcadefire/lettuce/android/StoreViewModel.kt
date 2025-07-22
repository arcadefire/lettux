package io.github.arcadefire.lettuce.android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.arcadefire.lettuce.core.Action
import io.github.arcadefire.lettuce.core.State
import io.github.arcadefire.lettuce.core.Store
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

open class StoreViewModel<S : State>(storeCreator: StoreCreator<S>) : Store<S>, ViewModel() {

    private val store: Store<S> by lazy {
        storeCreator.create(this.viewModelScope)
    }

    override val storeScope: CoroutineScope
        get() = viewModelScope

    override val states: StateFlow<S>
        get() = store.states

    override fun send(action: Action) = store.send(action)
}
