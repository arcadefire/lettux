package io.github.arcadefire.lettuce.core

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import io.github.arcadefire.lettuce.IncrementAction
import kotlin.test.Test
import io.github.arcadefire.lettuce.PlainState
import io.github.arcadefire.lettuce.UnHandledAction
import io.github.arcadefire.lettuce.extension.combine
import io.github.arcadefire.lettuce.factory.createStore

internal class SubscriptionTest {

    private val testActionHandler = ActionHandler<PlainState> { action ->
        if (action is IncrementAction) {
            commit(state.copy(value = state.value + 1))
        }
    }

    @Test
    fun `should subscribe to state changes`() {
        val collectedStates = ArrayList<PlainState>(2)
        val testScope = TestScope()
        val store = createStore(
            initialState = PlainState(value = 0),
            actionHandler = testActionHandler,
            subscription = { states ->
                states
                    .onEach {
                        collectedStates.add(it)
                    }
                    .map {
                        it.value.takeIf { it == 1 }?.let { IncrementAction } ?: UnHandledAction
                    }
            },
            storeScope = testScope,
        )

        store.send(IncrementAction)

        testScope.advanceUntilIdle()
        testScope.cancel()

        collectedStates shouldBe listOf(PlainState(value = 1), PlainState(value = 2))
    }

    @Test
    fun `should combine two subscriptions in a single one`() {
        val testScope = TestScope()
        val callOrder = ArrayList<Int>(2)
        val first = Subscription<PlainState> { states ->
            states.map {
                callOrder.add(1)
                UnHandledAction
            }
        }
        val second = Subscription<PlainState> { states ->
            states.map {
                callOrder.add(2)
                UnHandledAction
            }
        }
        val store = createStore(
            initialState = PlainState(value = 0),
            actionHandler = testActionHandler,
            subscription = combine(first, second),
            storeScope = testScope,
        )

        store.send(UnHandledAction)

        testScope.advanceUntilIdle()
        testScope.cancel()

        callOrder shouldBe listOf(1, 2)
    }

    @Test
    fun `combine with a single subscriber just returns the subscriber`() {
        val testScope = TestScope()
        var counter = 0
        val first = Subscription<PlainState> { states ->
            states.map {
                counter++
                UnHandledAction
            }
        }
        val store = createStore(
            initialState = PlainState(value = 0),
            actionHandler = testActionHandler,
            subscription = combine(first),
            storeScope = testScope,
        )

        store.send(UnHandledAction)

        testScope.advanceUntilIdle()
        testScope.cancel()

        counter shouldBe 1
    }
}
