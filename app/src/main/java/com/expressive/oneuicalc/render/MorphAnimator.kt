package com.expressive.oneuicalc.render

import android.animation.ValueAnimator
import android.view.MotionEvent
import android.view.View
import android.view.animation.OvershootInterpolator
import java.util.WeakHashMap

object MorphAnimator {
    private class AnimState(
        var progress: Float = 0f,
        var animator: ValueAnimator? = null,
        var lastEventTime: Long = -1L,
        var lastAction: Int = -1
    )

    private val states = WeakHashMap<View, AnimState>()

    fun getProgress(view: View): Float {
        return states[view]?.progress ?: 0f
    }

    fun onTouch(view: View, event: MotionEvent) {
        val action = event.actionMasked
        if (action != MotionEvent.ACTION_DOWN &&
            action != MotionEvent.ACTION_UP &&
            action != MotionEvent.ACTION_CANCEL
        ) {
            return
        }
        val state = states.getOrPut(view) { AnimState() }
        if (state.lastEventTime == event.eventTime && state.lastAction == action) {
            return
        }
        state.lastEventTime = event.eventTime
        state.lastAction = action

        when (action) {
            MotionEvent.ACTION_DOWN -> animateTo(state, view, 1f, durationMs = 130L, overshoot = 1.35f)
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> animateTo(state, view, 0f, durationMs = 280L, overshoot = 1.65f)
        }
    }

    private fun animateTo(
        state: AnimState,
        view: View,
        target: Float,
        durationMs: Long,
        overshoot: Float
    ) {
        state.animator?.cancel()
        val start = state.progress
        val animator = ValueAnimator.ofFloat(start, target).apply {
            duration = durationMs
            interpolator = OvershootInterpolator(overshoot)
            addUpdateListener { anim ->
                state.progress = anim.animatedValue as Float
                view.invalidate()
            }
        }
        state.animator = animator
        animator.start()
    }
}
