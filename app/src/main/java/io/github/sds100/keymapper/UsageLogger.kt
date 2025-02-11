package io.github.sds100.keymapper

import android.content.ContentValues
import android.content.Context
import io.github.sds100.keymapper.system.accessibility.AccessibilityNodeModel

class UsageLogger(context: Context) {
    var currentApp = ""
    var startTime: Long = System.currentTimeMillis()
    val dbHelper: DBHelper = DBHelper(context)

    fun update(accessibilityNodeModel: AccessibilityNodeModel) {
        val newApp = accessibilityNodeModel.packageName ?: ""
        val launch = currentApp != newApp

        if (!launch || currentApp.isEmpty()) {
            currentApp = newApp
            return
        }

        val currentTime = System.currentTimeMillis()
        val contentValues = ContentValues()
        contentValues.put("package_name", currentApp)
        contentValues.put("start_time", startTime)
        contentValues.put("end_time", currentTime)
        contentValues.put("launch", true)

        dbHelper.writableDatabase.insert(TABLE_NAME, null, contentValues)
        println("Usage Values: $contentValues")

        startTime = currentTime
        currentApp = newApp
    }
}
