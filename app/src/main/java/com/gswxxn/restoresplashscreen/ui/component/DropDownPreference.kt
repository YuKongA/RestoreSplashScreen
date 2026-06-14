package com.gswxxn.restoresplashscreen.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.gswxxn.restoresplashscreen.R
import com.gswxxn.restoresplashscreen.utils.CommonUtils.toast
import com.highcapable.yukihookapi.YukiHookAPI
import com.highcapable.yukihookapi.hook.factory.prefs
import com.highcapable.yukihookapi.hook.xposed.prefs.data.PrefsData
import dev.lackluster.hyperx.ui.preference.DropDownEntry
import dev.lackluster.hyperx.ui.preference.DropDownPreference as HyperXDropDownPreference

/**
 * 下拉框可组合函数, 使用 YukiHookAPI 管理 SharedPreferences, 并在模块未激活时提示用户。
 *
 * 注意：[entries] 中每项的 [DropDownEntry.value] 即为该项的下标（与旧的 index 语义一致）。
 * 可用 `entries.mapIndexed { index, title -> DropDownEntry(value = index, title = title) }` 构造。
 */
@Composable
fun DropDownPreference(
    title: String,
    summary: String? = null,
    entries: List<DropDownEntry<Int>>,
    prefsData: PrefsData<Int>? = null,
    selectedIndex: MutableIntState? = null,
    showValue: Boolean = true,
    onSelectedIndexChange: ((Int) -> Unit)? = null,
) {
    val context = LocalContext.current
    val prefs = context.prefs()

    val currentSelectedIndex = selectedIndex
        ?: prefsData?.let { remember { mutableIntStateOf(prefs.get(it).coerceIn(0, entries.size - 1)) } }
        ?: remember { mutableIntStateOf(0) }

    HyperXDropDownPreference(
        title = title,
        summary = summary,
        value = currentSelectedIndex.intValue,
        entries = entries,
        showValue = showValue,
        onValueChange = { newValue ->
            if (!YukiHookAPI.Status.isXposedModuleActive) {
                context.toast(R.string.make_sure_active)
            } else {
                prefsData?.let { prefs.edit { put(it, newValue) } }
                currentSelectedIndex.intValue = newValue
                onSelectedIndexChange?.invoke(newValue)
            }
        },
    )
}
