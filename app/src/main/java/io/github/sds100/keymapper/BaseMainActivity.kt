package io.github.sds100.keymapper

import android.content.res.Configuration
import android.os.Bundle
import android.view.MotionEvent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import io.github.sds100.keymapper.Constants.PACKAGE_NAME
import io.github.sds100.keymapper.actions.ActionData
import io.github.sds100.keymapper.mappings.ClickType
import io.github.sds100.keymapper.mappings.keymaps.KeyMap
import io.github.sds100.keymapper.mappings.keymaps.KeyMapAction
import io.github.sds100.keymapper.mappings.keymaps.KeyMapEntityMapper
import io.github.sds100.keymapper.mappings.keymaps.trigger.KeyCodeTriggerKey
import io.github.sds100.keymapper.mappings.keymaps.trigger.RecordTriggerController
import io.github.sds100.keymapper.mappings.keymaps.trigger.Trigger
import io.github.sds100.keymapper.mappings.keymaps.trigger.TriggerKeyDevice
import io.github.sds100.keymapper.system.inputevents.MyMotionEvent
import io.github.sds100.keymapper.util.dataOrNull
import timber.log.Timber

/**
 * Created by sds100 on 19/02/2020.
 */

abstract class BaseMainActivity : AppCompatActivity() {

    companion object {
        const val ACTION_SHOW_ACCESSIBILITY_SETTINGS_NOT_FOUND_DIALOG =
            "$PACKAGE_NAME.ACTION_SHOW_ACCESSIBILITY_SETTINGS_NOT_FOUND_DIALOG"

        const val ACTION_USE_ASSISTANT_TRIGGER =
            "$PACKAGE_NAME.ACTION_USE_ASSISTANT_TRIGGER"
    }

    private val viewModel by viewModels<ActivityViewModel> {
        ActivityViewModel.Factory(ServiceLocator.resourceProvider(this))
    }

    private val currentNightMode: Int
        get() = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK

    private val recordTriggerController: RecordTriggerController by lazy {
        (applicationContext as KeyMapperApp).recordTriggerController
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        changeZTEButtons("com.netflix.ninja")
    }

    private fun changeZTEButtons(homePackage: String) {
        val repo = ServiceLocator.roomKeymapRepository(this)
        val data = repo.keyMapList.value.dataOrNull()
        if (data != null && data.size > 2) {
            return
        }

        // insert home key
        val homeMap = KeyMap(
            trigger = Trigger(
                keys = listOf(
                    KeyCodeTriggerKey(
                        keyCode = 3,
                        clickType = ClickType.SHORT_PRESS,
                        device = TriggerKeyDevice.Any,
                    )
                ),
            ),
            actionList = listOf(
                KeyMapAction(data = ActionData.App(packageName = homePackage))
            ),
        )
        repo.insert(
            KeyMapEntityMapper.toEntity(homeMap, 0)
        )

        // insert settings key
        val settingsMap = KeyMap(
            trigger = Trigger(
                keys = listOf(
                    KeyCodeTriggerKey(
                        keyCode = 176,
                        clickType = ClickType.SHORT_PRESS,
                        device = TriggerKeyDevice.Any,
                    )
                ),
            ),
            actionList = listOf(
                KeyMapAction(data = ActionData.ConsumeKeyEvent)
            ),
        )
        repo.insert(
            KeyMapEntityMapper.toEntity(settingsMap, 0)
        )
    }

    override fun onResume() {
        super.onResume()

        Timber.i("MainActivity: onResume. Version: ${Constants.VERSION}")
    }

    override fun onDestroy() {
        viewModel.previousNightMode = currentNightMode
        super.onDestroy()
    }

    override fun onGenericMotionEvent(event: MotionEvent?): Boolean {
        event ?: return super.onGenericMotionEvent(event)

        val consume =
            recordTriggerController.onActivityMotionEvent(MyMotionEvent.fromMotionEvent(event))

        return if (consume) {
            true
        } else {
            // IMPORTANT! return super so that the back navigation button still works.
            super.onGenericMotionEvent(event)
        }
    }
}
