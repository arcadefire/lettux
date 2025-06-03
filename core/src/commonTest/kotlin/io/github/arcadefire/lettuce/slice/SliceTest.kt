package io.github.arcadefire.lettuce.slice

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import io.github.arcadefire.lettuce.IncrementAction
import io.github.arcadefire.lettuce.NestedState
import io.github.arcadefire.lettuce.PlainState
import io.github.arcadefire.lettuce.UnHandledAction
import io.github.arcadefire.lettuce.core.Action
import io.github.arcadefire.lettuce.core.ActionHandler
import io.github.arcadefire.lettuce.core.Middleware
import io.github.arcadefire.lettuce.core.Outcome
import io.github.arcadefire.lettuce.core.Store
import io.github.arcadefire.lettuce.core.Subscription
import io.github.arcadefire.lettuce.extension.state
import io.github.arcadefire.lettuce.factory.sliceStore
import io.github.arcadefire.lettuce.factory.createStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.TestScope

internal class SliceTest {

    data class SetValueAction(val value: Int) : Action

    private val testActionHandler = ActionHandler<NestedState> { action ->
        if (action is IncrementAction) {
            commit(
                state.copy(
                    innerState = state.innerState.copy(value = state.innerState.value + 1)
                )
            )
        }
    }

    private fun testStore(storeScope: CoroutineScope) = createStore(
        initialState = NestedState(),
        actionHandler = testActionHandler,
        middlewares = emptyList(),
        subscription = null,
        storeScope = storeScope,
    )

    @Test
    fun `should slice from the parent store`() = runTest {
        val sliced: Store<PlainState> = sliceStore(
            store = testStore(storeScope = this),
            stateToSlice = { state -> state.innerState },
            sliceToState = { state, slice -> state.copy(innerState = slice) },
            sliceScope = this,
        )

        sliced.send(IncrementAction)

        sliced.state shouldBe PlainState(value = 1)
    }

    @Test
    fun `slice middlewares should intercept the action once`() = runTest {
        var counter = 0
        val first = Middleware { action, _, chain ->
            counter++
            chain.proceed(action)
        }
        val second = Middleware { action, _, chain ->
            counter++
            chain.proceed(action)
        }

        val sliced: Store<PlainState> = sliceStore(
            store = testStore(storeScope = this),
            stateToSlice = { state -> state.innerState },
            sliceToState = { state, slice -> state.copy(innerState = slice) },
            middlewares = listOf(first, second),
            sliceScope = this,
        )

        sliced.send(IncrementAction)

        counter shouldBe 2
    }

    @Test
    fun `slice middlewares should not interfere with parent store middlewares`() = runTest {
        var parentCounter = 0
        val parentMiddleware = Middleware { action, _, chain ->
            parentCounter++
            chain.proceed(action)
        }
        val parentStore = createStore(
            initialState = NestedState(),
            actionHandler = testActionHandler,
            middlewares = listOf(parentMiddleware),
            subscription = null,
            storeScope = this,
        )
        var sliceCounter = 0
        val sliceMiddleware = Middleware { action, _, chain ->
            sliceCounter++
            chain.proceed(action)
        }
        val slice = sliceStore(
            store = parentStore,
            stateToSlice = { it.innerState },
            sliceToState = { state, slice -> state.copy(innerState = slice) },
            middlewares = listOf(sliceMiddleware),
            sliceScope = this,
        )
        slice.send(IncrementAction)
        advanceUntilIdle()
        parentCounter shouldBe 1
        sliceCounter shouldBe 1
    }

    @Test
    fun `middlewares should be executed in the order they are provided`() = runTest {
        val callOrder = mutableListOf<Int>()
        val first = Middleware { action, _, chain ->
            callOrder.add(1)
            chain.proceed(action)
        }
        val second = Middleware { action, _, chain ->
            callOrder.add(2)
            chain.proceed(action)
        }
        val sliced: Store<PlainState> = sliceStore(
            store = testStore(storeScope = this),
            stateToSlice = { state -> state.innerState },
            sliceToState = { state, slice -> state.copy(innerState = slice) },
            middlewares = listOf(first, second),
            sliceScope = this,
        )

        sliced.send(IncrementAction)

        callOrder shouldBe listOf(1, 2)
    }

    @Test
    fun `slice middleware should receive the expected outcome when the parent state changes`() =
        runTest {
            lateinit var outcome: Outcome
            val middleware = Middleware { action, _, chain ->
                chain.proceed(action).also { outcome = it }
            }
            val sliced: Store<PlainState> = sliceStore(
                store = testStore(storeScope = this),
                stateToSlice = { state -> state.innerState },
                sliceToState = { state, slice -> state.copy(innerState = slice) },
                middlewares = listOf(middleware),
                sliceScope = this,
            )

            sliced.send(IncrementAction)

            outcome shouldBe Outcome.StateMutated(PlainState(value = 1))
        }

    @Test
    fun `slice middleware should receive the expected outcome when the parent state doesn't change`() =
        runTest {
            lateinit var outcome: Outcome
            val middleware = Middleware { action, _, chain ->
                chain.proceed(action).also { outcome = it }
            }
            val sliced: Store<PlainState> = sliceStore(
                store = testStore(storeScope = this),
                stateToSlice = { state -> state.innerState },
                sliceToState = { state, slice -> state.copy(innerState = slice) },
                middlewares = listOf(middleware),
                sliceScope = this,
            )

            sliced.send(UnHandledAction)

            outcome shouldBe Outcome.NoMutation
        }

    @Test
    fun `slice subscription should receive the expected sliced state`() =
        runTest {
            lateinit var subscribedState: PlainState
            val subscription = Subscription { states ->
                states
                    .onEach { subscribedState = it }
                    .map { UnHandledAction }
                    .take(1)
            }
            val sliced: Store<PlainState> = sliceStore(
                store = testStore(storeScope = this),
                stateToSlice = { state -> state.innerState },
                sliceToState = { state, slice -> state.copy(innerState = slice) },
                subscription = subscription,
                sliceScope = this,
            )

            sliced.send(IncrementAction)

            advanceUntilIdle()

            subscribedState shouldBe PlainState(value = 1)
        }

    @Test
    fun `slice scoped action handler should handle the action`() =
        runTest {
            val sliced: Store<PlainState> = sliceStore(
                store = testStore(storeScope = this),
                stateToSlice = { state -> state.innerState },
                sliceToState = { state, slice -> state.copy(innerState = slice) },
                sliceScope = this,
                actionHandler = ActionHandler<PlainState> { action ->
                    if (action is SetValueAction) {
                        commit(
                            state.copy(value = action.value)
                        )
                    }
                },
            )

            sliced.send(SetValueAction(value = 1_000))

            advanceUntilIdle()

            sliced.state shouldBe PlainState(value = 1_000)
        }

    @Test
    fun `slice action handlers should handle scoped and non-scoped actions`() =
        runTest {
            val sliced: Store<PlainState> = sliceStore(
                store = testStore(storeScope = this),
                stateToSlice = { state -> state.innerState },
                sliceToState = { state, slice -> state.copy(innerState = slice) },
                sliceScope = this,
                actionHandler = ActionHandler<PlainState> { action ->
                    if (action is SetValueAction) {
                        commit(
                            state.copy(value = action.value)
                        )
                    }
                },
            )

            sliced.send(SetValueAction(value = 1_000))
            sliced.send(IncrementAction)

            advanceUntilIdle()

            sliced.state shouldBe PlainState(value = 1_001)
        }

    @Test
    fun `multiple slices from the same store should operate independently`() = runTest {
        val parentStore = testStore(storeScope = this)
        val slice1 = sliceStore(
            store = parentStore,
            stateToSlice = { it.innerState },
            sliceToState = { state, slice -> state.copy(innerState = slice) },
            sliceScope = this,
        )
        val slice2 = sliceStore(
            store = parentStore,
            stateToSlice = { it.innerState },
            sliceToState = { state, slice -> state.copy(innerState = slice) },
            sliceScope = this,
        )
        slice1.send(IncrementAction)
        slice2.send(IncrementAction)
        advanceUntilIdle()
        slice1.state shouldBe PlainState(value = 2)
        slice2.state shouldBe PlainState(value = 2)
        parentStore.state.innerState.value shouldBe 2
    }

    @Test
    fun `slice should maintain state consistency after multiple actions`() = runTest {
        val slice = sliceStore(
            store = testStore(storeScope = this),
            stateToSlice = { it.innerState },
            sliceToState = { state, slice -> state.copy(innerState = slice) },
            sliceScope = this,
        )
        repeat(5) { slice.send(IncrementAction) }
        advanceUntilIdle()
        slice.state shouldBe PlainState(value = 5)
    }
}
