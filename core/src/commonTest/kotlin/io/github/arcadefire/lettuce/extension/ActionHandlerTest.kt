package io.github.arcadefire.lettuce.extension

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import io.github.arcadefire.lettuce.IncrementAction
import io.github.arcadefire.lettuce.NestedState
import io.github.arcadefire.lettuce.PlainState
import io.github.arcadefire.lettuce.core.Action
import io.github.arcadefire.lettuce.core.ActionHandler
import io.github.arcadefire.lettuce.factory.createStore

internal class ActionHandlerTest {

    @Test
    fun `pullback should transform an action handler of a sub-state into an action handler of the parent state`() {
        runTest {
            val sliceActionHandler = ActionHandler<PlainState> {
                commit(state.copy(value = state.value + 1))
            }
            val store = createStore(
                initialState = NestedState(PlainState()),
                actionHandler = sliceActionHandler.pullback(
                    stateToSlice = { it.innerState },
                    sliceToState = { state, slice -> state.copy(innerState = slice) }
                ),
                storeScope = this,
            )

            store.send(IncrementAction)

            store.state shouldBe NestedState(PlainState(1))
        }
    }

    @Test
    fun `should send an action to the same store from within the handle function`() {
        runTest {
            val actionHandler = ActionHandler<NestedState> { action ->
                when (action) {
                    FirstAction -> send(SecondAction)
                    SecondAction -> {
                        commit(state.copy(innerState = state.innerState.copy(value = 1)))
                    }
                }
            }
            val store = createStore(
                initialState = NestedState(PlainState()),
                actionHandler = actionHandler,
                storeScope = this,
            )

            store.send(FirstAction)

            advanceUntilIdle()

            store.state shouldBe NestedState(PlainState(1))
        }
    }

    @Test
    fun `should combine action handlers into an aggregated one`() {
        runTest {
            val firstActionHandler = ActionHandler<NestedState> { action ->
                when (action) {
                    FirstAction -> {
                        commit(state.copy(innerState = state.innerState.copy(value = 1)))
                    }
                }
            }
            val secondActionHandler = ActionHandler<NestedState> { action ->
                when (action) {
                    SecondAction -> {
                        commit(state.copy(innerState = state.innerState.copy(value = 2)))
                    }
                }
            }
            val store = createStore(
                initialState = NestedState(PlainState()),
                actionHandler = firstActionHandler + secondActionHandler,
                storeScope = this,
            )

            store.send(FirstAction)
            store.state shouldBe NestedState(PlainState(1))

            store.send(SecondAction)
            store.state shouldBe NestedState(PlainState(2))
        }
    }

    private data object FirstAction : Action
    private data object SecondAction : Action
}
