package com.android.systemui.statusbar.phone.shade.transition

import android.testing.AndroidTestingRunner
import androidx.test.filters.SmallTest
import com.android.systemui.R
import com.android.systemui.SysuiTestCase
import com.android.systemui.dump.DumpManager
import com.android.systemui.plugins.qs.QS
import com.android.systemui.statusbar.notification.stack.NotificationStackScrollLayoutController
import com.android.systemui.statusbar.phone.NotificationPanelViewController
import com.android.systemui.statusbar.phone.panelstate.PanelExpansionChangeEvent
import com.android.systemui.statusbar.phone.panelstate.PanelExpansionStateManager
import com.android.systemui.statusbar.phone.panelstate.STATE_OPENING
import com.android.systemui.statusbar.policy.FakeConfigurationController
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.reset
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyZeroInteractions
import org.mockito.MockitoAnnotations

@RunWith(AndroidTestingRunner::class)
@SmallTest
class ShadeTransitionControllerTest : SysuiTestCase() {

    @Mock private lateinit var npvc: NotificationPanelViewController
    @Mock private lateinit var nsslController: NotificationStackScrollLayoutController
    @Mock private lateinit var qs: QS
    @Mock private lateinit var noOpOverScroller: NoOpOverScroller
    @Mock private lateinit var splitShadeOverScroller: SplitShadeOverScroller
    @Mock private lateinit var scrimShadeTransitionController: ScrimShadeTransitionController
    @Mock private lateinit var dumpManager: DumpManager

    private lateinit var controller: ShadeTransitionController

    private val configurationController = FakeConfigurationController()
    private val panelExpansionStateManager = PanelExpansionStateManager()

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)

        controller =
            ShadeTransitionController(
                configurationController,
                panelExpansionStateManager,
                dumpManager,
                context,
                splitShadeOverScrollerFactory = { _, _ -> splitShadeOverScroller },
                noOpOverScroller,
                scrimShadeTransitionController)

        // Resetting as they are notified upon initialization.
        reset(noOpOverScroller, splitShadeOverScroller)
    }

    @Test
    fun onPanelExpansionChanged_inSplitShade_forwardsToSplitShadeOverScroller() {
        initLateProperties()
        enableSplitShade()

        startPanelExpansion()

        verify(splitShadeOverScroller).onPanelStateChanged(STATE_OPENING)
        verify(splitShadeOverScroller).onDragDownAmountChanged(DEFAULT_DRAG_DOWN_AMOUNT)
        verifyZeroInteractions(noOpOverScroller)
    }

    @Test
    fun onPanelStateChanged_inSplitShade_propertiesNotInitialized_forwardsToNoOpOverScroller() {
        enableSplitShade()

        startPanelExpansion()

        verify(noOpOverScroller).onPanelStateChanged(STATE_OPENING)
        verify(noOpOverScroller).onDragDownAmountChanged(DEFAULT_DRAG_DOWN_AMOUNT)
        verifyZeroInteractions(splitShadeOverScroller)
    }

    @Test
    fun onPanelStateChanged_notInSplitShade_forwardsToNoOpOverScroller() {
        initLateProperties()
        disableSplitShade()

        startPanelExpansion()

        verify(noOpOverScroller).onPanelStateChanged(STATE_OPENING)
        verify(noOpOverScroller).onDragDownAmountChanged(DEFAULT_DRAG_DOWN_AMOUNT)
        verifyZeroInteractions(splitShadeOverScroller)
    }

    @Test
    fun onPanelStateChanged_forwardsToScrimTransitionController() {
        initLateProperties()

        startPanelExpansion()

        verify(scrimShadeTransitionController).onPanelStateChanged(STATE_OPENING)
        verify(scrimShadeTransitionController).onPanelExpansionChanged(DEFAULT_EXPANSION_EVENT)
    }

    private fun initLateProperties() {
        controller.qs = qs
        controller.notificationStackScrollLayoutController = nsslController
        controller.notificationPanelViewController = npvc
    }

    private fun disableSplitShade() {
        setSplitShadeEnabled(false)
    }

    private fun enableSplitShade() {
        setSplitShadeEnabled(true)
    }

    private fun setSplitShadeEnabled(enabled: Boolean) {
        overrideResource(R.bool.config_use_split_notification_shade, enabled)
        configurationController.notifyConfigurationChanged()
    }

    private fun startPanelExpansion() {
        panelExpansionStateManager.onPanelExpansionChanged(
            DEFAULT_EXPANSION_EVENT.fraction,
            DEFAULT_EXPANSION_EVENT.expanded,
            DEFAULT_EXPANSION_EVENT.tracking,
            DEFAULT_EXPANSION_EVENT.dragDownPxAmount,
        )
    }

    companion object {
        private const val DEFAULT_DRAG_DOWN_AMOUNT = 123f
        private val DEFAULT_EXPANSION_EVENT =
            PanelExpansionChangeEvent(
                fraction = 0.5f,
                expanded = true,
                tracking = true,
                dragDownPxAmount = DEFAULT_DRAG_DOWN_AMOUNT)
    }
}
