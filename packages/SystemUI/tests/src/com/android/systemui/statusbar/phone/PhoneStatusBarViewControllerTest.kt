/*
 * Copyright (C) 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.statusbar.phone

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.android.systemui.R
import com.android.systemui.SysuiTestCase
import com.android.systemui.statusbar.CommandQueue
import com.android.systemui.util.mockito.any
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations

@SmallTest
class PhoneStatusBarViewControllerTest : SysuiTestCase() {

    private val stateChangeListener = TestStateChangedListener()
    private val touchEventHandler = TestTouchEventHandler()

    @Mock
    private lateinit var commandQueue: CommandQueue
    @Mock
    private lateinit var panelViewController: PanelViewController
    @Mock
    private lateinit var panelView: ViewGroup
    @Mock
    private lateinit var scrimController: ScrimController

    @Mock
    private lateinit var moveFromCenterAnimation: StatusBarMoveFromCenterAnimationController

    private lateinit var view: PhoneStatusBarView
    private lateinit var controller: PhoneStatusBarViewController

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        `when`(panelViewController.view).thenReturn(panelView)

        // create the view on main thread as it requires main looper
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val parent = FrameLayout(mContext) // add parent to keep layout params
            view = LayoutInflater.from(mContext)
                .inflate(R.layout.status_bar, parent, false) as PhoneStatusBarView
            view.setScrimController(scrimController)
            view.setBar(mock(StatusBar::class.java))
        }

        controller = PhoneStatusBarViewController(
                view,
                null,
                stateChangeListener,
                touchEventHandler,
        )
    }

    @Test
    fun constructor_setsTouchHandlerOnView() {
        val event = MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_DOWN, 0f, 0f, 0)

        view.onTouchEvent(event)

        assertThat(touchEventHandler.lastEvent).isEqualTo(event)
    }

    @Test
    fun constructor_moveFromCenterAnimationIsNotNull_moveFromCenterAnimationInitialized() {
        controller = PhoneStatusBarViewController(
                view, moveFromCenterAnimation, stateChangeListener, touchEventHandler
        )

        verify(moveFromCenterAnimation).init(any(), any())
    }

    @Test
    fun constructor_setsExpansionStateChangedListenerOnView() {
        assertThat(stateChangeListener.stateChangeCalled).isFalse()

        // If the constructor correctly set the listener, then it should be used when
        // [PhoneStatusBarView.panelExpansionChanged] is called.
        view.panelExpansionChanged(0f, false)

        assertThat(stateChangeListener.stateChangeCalled).isTrue()
    }

    private class TestStateChangedListener : PhoneStatusBarView.PanelExpansionStateChangedListener {
        var stateChangeCalled: Boolean = false

        override fun onPanelExpansionStateChanged() {
            stateChangeCalled = true
        }
    }

    private class TestTouchEventHandler : PhoneStatusBarView.TouchEventHandler {
        var lastEvent: MotionEvent? = null
        override fun handleTouchEvent(event: MotionEvent?): Boolean {
            lastEvent = event
            return false
        }

    }
}
