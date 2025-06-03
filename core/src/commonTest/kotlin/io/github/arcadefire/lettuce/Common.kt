package io.github.arcadefire.lettuce

import io.github.arcadefire.lettuce.core.Action
import io.github.arcadefire.lettuce.core.State

object IncrementAction : Action
object UnHandledAction : Action

data class NestedState(val innerState: PlainState = PlainState()) : State
data class PlainState(val value: Int = 0) : State
