package com.gswxxn.restoresplashscreen.ui.apppage

import android.graphics.drawable.Drawable
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.captionBar
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.core.graphics.drawable.toBitmap
import androidx.navigation.NavController
import com.gswxxn.restoresplashscreen.R
import com.gswxxn.restoresplashscreen.data.DataConst
import com.gswxxn.restoresplashscreen.ui.MainActivity
import com.gswxxn.restoresplashscreen.ui.component.SpliceCard
import com.gswxxn.restoresplashscreen.utils.CommonUtils.notEqualsTo
import com.gswxxn.restoresplashscreen.utils.CommonUtils.toMap
import com.gswxxn.restoresplashscreen.utils.CommonUtils.toSet
import com.highcapable.yukihookapi.hook.factory.prefs
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.lackluster.hyperx.compose.base.AlertDialog
import dev.lackluster.hyperx.compose.base.AlertDialogMode
import dev.lackluster.hyperx.compose.base.BasePageDefaults
import dev.lackluster.hyperx.compose.base.DrawableResIcon
import dev.lackluster.hyperx.compose.base.HazeScaffold
import dev.lackluster.hyperx.compose.base.IconSize
import dev.lackluster.hyperx.compose.base.ImageIcon
import dev.lackluster.hyperx.compose.preference.EditTextDataType
import dev.lackluster.hyperx.compose.preference.EditTextDialog
import dev.lackluster.hyperx.compose.preference.EditTextPreference
import dev.lackluster.hyperx.compose.preference.PreferenceGroup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.BasicComponentDefaults
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.SearchBar
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import java.text.Collator
import java.util.Locale

/**
 * 基础设置 - 遮罩最小持续时长
 */
@Composable
fun MinDurationPage(
    navController: NavController,
    adjustPadding: PaddingValues,
    mode: BasePageDefaults.Mode,
    blurEnabled: MutableState<Boolean> = MainActivity.blurEnabled,
) {
    val context = LocalContext.current
//    val checkedListKey = DataConst.MIN_DURATION_LIST.key
//    val configMapKey = DataConst.MIN_DURATION_CONFIG_MAP.key
    val scrollBehavior = MiuixScrollBehavior(rememberTopAppBarState())
    val hazeTint = MiuixTheme.colorScheme.surface.copy(
        if (scrollBehavior.state.collapsedFraction <= 0f) 1f
        else lerp(1f, 0.67f, (scrollBehavior.state.collapsedFraction))
    )
    val listState = rememberLazyListState()
    val modifiedDialogVisibility = remember { mutableStateOf(false) }

    val dialogMessage = stringResource(R.string.set_min_duration) + "\n" + stringResource(R.string.set_min_duration_unit)
    var queryString by remember { mutableStateOf("") }
    var sortTrigger by remember { mutableIntStateOf(0) }

    val emptyMapString = stringResource(R.string.not_set_min_duration)

    // 完整应用列表
    var appInfoList by remember { mutableStateOf<List<DurationAppInfo>>(emptyList()) }

    // 在列表中的条目
    var appInfoFilter by remember { mutableStateOf<List<DurationAppInfo>>(emptyList()) }

    // 保存前的配置
    val tmpCheckedList = mutableSetOf<String>().apply {
        clear()
        addAll(context.prefs().get(DataConst.MIN_DURATION_LIST))
    }
    val tmpConfigMap = mutableMapOf<String, String>().apply {
        clear()
        putAll(context.prefs().get(DataConst.MIN_DURATION_CONFIG_MAP).toMap())
    }

    val coroutineScope = rememberCoroutineScope()
    var queryJob: Job? = null
    var isLoading by remember { mutableStateOf(true) }

    BackHandler(true) {
        val currentCheckedList = appInfoList.filter { it.isChecked.value }.map {
            it.packageName
        }.toSet()
        val currentConfigMap = appInfoList.filter {
            it.config.value != null && it.config.value != emptyMapString
        }.map {
            "${it.packageName}_${it.config.value}"
        }.toSet()
        if (currentCheckedList.notEqualsTo(tmpCheckedList) || currentConfigMap.notEqualsTo(tmpConfigMap.toSet())) {
            modifiedDialogVisibility.value = true
        } else {
            navController.popBackStack()
        }
    }

    LaunchedEffect(Unit) {
        launch {
            isLoading = true
            // 使用 IO 调度器进行耗时操作
            val loadedApps = withContext(Dispatchers.IO) {
                val pm = context.packageManager
                val installedApps = pm.getInstalledApplications(0)

                // 创建应用信息列表
                installedApps.map { appInfo ->
                    DurationAppInfo(
                        appName = appInfo.loadLabel(pm).toString(),
                        packageName = appInfo.packageName,
                        icon = appInfo.loadIcon(pm),
                        isChecked = mutableStateOf(appInfo.packageName in tmpCheckedList),
                        config = mutableStateOf(tmpConfigMap[appInfo.packageName])
                    )
                }.sortedWith(
                    // 按应用类别排序：已勾选且有配置的应用优先显示
                    compareByDescending<DurationAppInfo> { it.isChecked.value }
                        .thenByDescending { it.config.value != null }
                        .thenBy(Collator.getInstance(Locale.getDefault())) { it.appName }
                )
            }

            appInfoList = loadedApps
            isLoading = false
        }
    }

    LaunchedEffect(appInfoList, queryString, sortTrigger) {
        if (appInfoList.isEmpty()) return@LaunchedEffect

        queryJob?.cancel()
        queryJob = launch(Dispatchers.Default) {
            if (queryString.isNotBlank()) {
                delay(300)
            } else {
                delay(50)
            }

            // 在后台线程进行过滤和排序
            val filtered = if (queryString.isBlank()) {
                appInfoList
            } else {
                appInfoList.filter {
                    it.appName.contains(queryString, true) || it.packageName.contains(queryString, true)
                }
            }

            // 排序：已勾选的应用优先，有配置的其次，然后按应用名称排序
            val sorted = filtered.sortedWith(
                compareByDescending<DurationAppInfo> { it.isChecked.value }
                    .thenByDescending { it.config.value != null }
                    .thenBy(Collator.getInstance(Locale.getDefault())) { it.appName }
            )

            // 切换回主线程更新 UI
            withContext(Dispatchers.Main) {
                appInfoFilter = sorted
            }
        }
    }
    val layoutDirection = LocalLayoutDirection.current
    val systemBarInsets =
        WindowInsets.systemBars.add(WindowInsets.displayCutout).only(WindowInsetsSides.Horizontal).asPaddingValues()
    val navigationIconPadding = PaddingValues.Absolute(
        left = if (mode != BasePageDefaults.Mode.SPLIT_RIGHT) systemBarInsets.calculateLeftPadding(layoutDirection) else 0.dp
    )

    HazeScaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { contentPadding ->
            TopAppBar(
                color = if (blurEnabled.value) Color.Transparent else MiuixTheme.colorScheme.surface,
                title = stringResource(R.string.min_duration_title),
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    IconButton(
                        modifier = Modifier
                            .padding(navigationIconPadding)
                            .padding(start = 21.dp)
                            .size(40.dp),
                        onClick = {
                            val currentCheckedList = appInfoList.filter { it.isChecked.value }.map {
                                it.packageName
                            }.toSet()
                            val currentConfigMap = appInfoList.filter {
                                it.config.value != null && it.config.value != emptyMapString
                            }.map {
                                "${it.packageName}_${it.config.value}"
                            }.toSet()
                            if (currentCheckedList.notEqualsTo(tmpCheckedList) || currentConfigMap.notEqualsTo(tmpConfigMap.toSet())) {
                                modifiedDialogVisibility.value = true
                            } else {
                                navController.popBackStack()
                            }
                        }
                    ) {
                        Icon(
                            modifier = Modifier.size(26.dp),
                            imageVector = MiuixIcons.Back,
                            contentDescription = "Back",
                            tint = MiuixTheme.colorScheme.onSurfaceSecondary
                        )
                    }
                },
                defaultWindowInsetsPadding = false,
                horizontalPadding = 28.dp + contentPadding.calculateLeftPadding(LocalLayoutDirection.current)
            )
        },
        bottomBar = { contentPadding ->
            val captionBarBottomPadding by rememberUpdatedState(
                WindowInsets.captionBar.only(WindowInsetsSides.Bottom).asPaddingValues().calculateBottomPadding()
            )
            val buttonPaddingValues = with(LocalLayoutDirection.current) {
                PaddingValues(
                    start = contentPadding.calculateStartPadding(this) + 16.dp,
                    top = 16.dp,
                    end = contentPadding.calculateEndPadding(this) + 16.dp,
                    bottom = WindowInsets.navigationBars.asPaddingValues()
                        .calculateBottomPadding() + captionBarBottomPadding + 16.dp
                )
            }
            Surface(
                color = MiuixTheme.colorScheme.surface.copy(
                    if (blurEnabled.value) 0f else 1f
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Transparent)
                ) {
                    HorizontalDivider(
                        thickness = 0.75.dp,
                        color = MiuixTheme.colorScheme.dividerLine
                    )
                    TextButton(
                        modifier = Modifier
                            .padding(buttonPaddingValues)
                            .fillMaxWidth(),
                        text = stringResource(R.string.save),
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                        minHeight = 50.dp,
                        onClick = {
                            CoroutineScope(Dispatchers.Default).launch {
                                val currentCheckedList = appInfoList.filter { it.isChecked.value }.map {
                                    it.packageName
                                }.toMutableSet()
                                val currentConfigMap = appInfoList.filter {
                                    it.config.value != null && it.config.value != emptyMapString
                                }.map {
                                    "${it.packageName}_${it.config.value}"
                                }.toMutableSet()

                                context.prefs().edit {
                                    put(DataConst.MIN_DURATION_LIST, currentCheckedList)
                                    put(DataConst.MIN_DURATION_CONFIG_MAP, currentConfigMap)
                                }
                                tmpCheckedList.apply {
                                    clear()
                                    addAll(context.prefs().get(DataConst.MIN_DURATION_LIST))
                                }
                                tmpConfigMap.apply {
                                    clear()
                                    putAll(context.prefs().get(DataConst.MIN_DURATION_CONFIG_MAP).toMap())
                                }
                                coroutineScope.launch {
                                    context.let {
                                        Toast.makeText(it, it.getString(R.string.save_successful), Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    )
                }
            }
        },
        blurTopBar = blurEnabled.value,
        blurBottomBar = blurEnabled.value,
        hazeStyle = HazeStyle(
            blurRadius = 25.dp,
            noiseFactor = 0f,
            backgroundColor = MiuixTheme.colorScheme.surface,
            tint = HazeTint(hazeTint)
        ),
        adjustPadding = adjustPadding,
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxHeight()
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            state = listState,
            contentPadding = paddingValues,
            overscrollEffect = null
        ) {
            item {
                SearchBar(
                    modifier = Modifier.padding(top = 12.dp, bottom = 6.dp),
                    inputField = {
                        InputField(
                            query = queryString,
                            onQueryChange = { queryString = it },
                            onSearch = { },
                            expanded = false,
                            onExpandedChange = { },
                            label = stringResource(R.string.search_hint)
                        )
                    },
                    expanded = false,
                    onExpandedChange = { },
                    content = { }
                )
            }
            item {
                PreferenceGroup {
                    BasicComponent(
                        insideMargin = PaddingValues(16.dp),
                        summary = stringResource(R.string.min_duration_sub_setting_hint),
                        summaryColor = BasicComponentDefaults.titleColor(),
                        startAction = {
                            Image(
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .size(20.dp),
                                imageVector = MiuixIcons.Info,
                                contentDescription = null,
                                colorFilter = ColorFilter.tint(MiuixTheme.colorScheme.onSurface)
                            )
                        }
                    )
                }
            }
            item {
                PreferenceGroup {
                    EditTextPreference(
                        title = stringResource(R.string.set_default_min_duration),
                        key = DataConst.MIN_DURATION.key,
                        dataType = EditTextDataType.INT,
                        dialogMessage = stringResource(R.string.set_min_duration_unit),
                        isValueValid = { (it as? Int) in 0..1000 }
                    )
                }
            }
            if (isLoading) {
                item {
                    SmallTitle(
                        text = stringResource(R.string.loading),
                        modifier = Modifier.padding(top = 6.dp),
                        textColor = MiuixTheme.colorScheme.onBackgroundVariant
                    )
                }
            } else {
                item {
                    SmallTitle(
                        text = stringResource(R.string.min_duration_separate_configuration),
                        modifier = Modifier.padding(top = 6.dp),
                        textColor = MiuixTheme.colorScheme.onBackgroundVariant
                    )
                }
                itemsIndexed(appInfoFilter, key = { index, item ->
                    item.packageName + item.isChecked + index + appInfoFilter.size
                }) { index, item ->
                    val topCornerRadius = if (index == 0) CardDefaults.CornerRadius else 0.dp
                    val bottomCornerRadius = if (index == appInfoFilter.size - 1) CardDefaults.CornerRadius else 0.dp
                    SpliceCard(
                        topCornerRadius,
                        bottomCornerRadius
                    ) {
                        MinDurationPreference(
                            icon = ImageIcon(
                                iconBitmap = item.icon.toBitmap().asImageBitmap(),
                                iconSize = IconSize.App
                            ),
                            title = item.appName,
                            summary = item.packageName,
                            checked = item.isChecked,
                            defValue = item.config.value?.toIntOrNull() ?: 0,
                            dialogMessage = dialogMessage,
                            onCheckedChange = {
                                // 在用户切换选中状态后，延迟触发重新排序
                                coroutineScope.launch {
                                    delay(200)
                                    sortTrigger++
                                }
                            },
                            onValueChange = { text, value ->
                                if (value == 0) {
                                    item.config.value = null
                                } else {
                                    item.config.value = text
                                }
                                // 配置变更后触发重新排序
                                coroutineScope.launch {
                                    delay(200)
                                    sortTrigger++
                                }
                            }
                        )
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
    AlertDialog(
        visibility = modifiedDialogVisibility,
        title = stringResource(R.string.not_saved_title),
        message = stringResource(R.string.not_saved_hint),
        cancelable = false,
        mode = AlertDialogMode.NegativeAndPositive,
        negativeText = stringResource(R.string.button_abandonment),
        positiveText = stringResource(R.string.button_reedit),
        onNegativeButton = {
            modifiedDialogVisibility.value = false
            navController.popBackStack()
        }
    )
}

@Composable
fun MinDurationPreference(
    icon: ImageIcon? = null,
    title: String,
    summary: String? = null,
    checked: MutableState<Boolean>,
    defValue: Int = 0,
    dialogMessage: String? = null,
    onCheckedChange: (() -> Unit)? = null,
    onValueChange: ((String, Int) -> Unit)? = null,
) {
    val emptyString = stringResource(R.string.not_set_min_duration)
    val dialogVisibility = remember { mutableStateOf(false) }
    val spValue = remember { mutableIntStateOf(defValue) }
    val stringValue = if (spValue.intValue != 0) spValue.intValue.toString() else emptyString
    val doOnInputConfirm: (String) -> Unit = { newString: String ->
        val oldValue = spValue.intValue
        val newValue = newString.toIntOrNull()
        if (newValue != null && (newValue in 0..1000) && oldValue != newValue) {
            spValue.intValue = newValue
            onValueChange?.let { it(newString, newValue) }
        }
    }
    BasicComponent(
        title = title,
        summary = summary,
        startAction = {
            icon?.let {
                DrawableResIcon(it)
            }
        },
        endActions = {
            Text(
                modifier = Modifier
                    .widthIn(max = 130.dp)
                    .padding(end = 12.dp),
                text = stringValue,
                fontSize = MiuixTheme.textStyles.body2.fontSize,
                color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                textAlign = TextAlign.End,
                overflow = TextOverflow.Ellipsis,
                maxLines = 2
            )
            Switch(
                checked = checked.value,
                onCheckedChange = { newValue ->
                    checked.value = newValue
                    onCheckedChange?.invoke()
                }
            )
        },
        insideMargin = PaddingValues((icon?.getHorizontalPadding() ?: 16.dp), 16.dp, 16.dp, 16.dp),
        onClick = {
            dialogVisibility.value = true
        }
    )
    EditTextDialog(
        visibility = dialogVisibility,
        title = title,
        message = dialogMessage,
        value = spValue.intValue.toString(),
        onInputConfirm = { newString ->
            doOnInputConfirm(newString)
        }
    )
}

data class DurationAppInfo(
    val appName: String,
    val packageName: String,
    val icon: Drawable,
    // 该 isChecked 用于存储应用是否被勾选, 0 为未勾选, 1 为勾选
    var isChecked: MutableState<Boolean>,
    var config: MutableState<String?>,
)