package com.gswxxn.restoresplashscreen.ui.component

import android.content.pm.ApplicationInfo
import android.graphics.drawable.Drawable
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.captionBar
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.gswxxn.restoresplashscreen.R
import com.gswxxn.restoresplashscreen.utils.CommonUtils.notEqualsTo
import com.highcapable.yukihookapi.hook.factory.prefs
import com.highcapable.yukihookapi.hook.xposed.prefs.data.PrefsData
import dev.lackluster.hyperx.navigation.LocalNavigator
import dev.lackluster.hyperx.ui.component.IconSize
import dev.lackluster.hyperx.ui.component.ImageIcon
import dev.lackluster.hyperx.ui.dialog.AlertDialog
import dev.lackluster.hyperx.ui.dialog.AlertDialogMode
import dev.lackluster.hyperx.ui.layout.HyperXScaffold
import dev.lackluster.hyperx.ui.layout.LocalHyperXLayoutConfig
import dev.lackluster.hyperx.ui.layout.LocalLayoutPadding
import dev.lackluster.hyperx.ui.preference.PreferenceGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.BasicComponentDefaults
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.ListPopupDefaults
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.SearchBar
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.MoreCircle
import top.yukonga.miuix.kmp.overlay.OverlayListPopup
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun AppListPage(
    title: String,
    checkedListKey: PrefsData<MutableSet<String>>,
    extraContent: (LazyListScope.() -> Unit)? = null
) {
    val context = LocalContext.current
    val prefs = context.prefs()

    val navigator = LocalNavigator.current
    val uiConfig = LocalHyperXLayoutConfig.current
    val blurEnabled = uiConfig.isBlurEnabled
    val layoutPadding = LocalLayoutPadding.current

    val containerColor = MiuixTheme.colorScheme.surface
    val blurTintAlpha = if (containerColor.luminance() >= 0.5f) {
        uiConfig.lightBlurAlpha
    } else {
        uiConfig.darkBlurAlpha
    }

    val scrollBehavior = MiuixScrollBehavior(rememberTopAppBarState())
    val listState = rememberLazyListState()
    val isTopPopupExpanded = remember { mutableStateOf(false) }
    val showTopPopup = remember { mutableStateOf(false) }
    val modifiedDialogVisibility = remember { mutableStateOf(false) }

    var selectSystemAppRequest by remember { mutableStateOf(false) }
    var clearSelectedRequest by remember { mutableStateOf(false) }
    var queryString by remember { mutableStateOf("") }
    var sortTrigger by remember { mutableIntStateOf(0) }

    // 完整应用列表
    var appInfoList by remember { mutableStateOf<List<MyAppInfo>>(emptyList()) }

    // 在列表中的条目
    var appInfoFilter by remember { mutableStateOf<List<MyAppInfo>>(emptyList()) }

    // 保存前的配置
    val tmpCheckedList = mutableSetOf<String>().apply {
        clear()
        addAll(prefs.get(checkedListKey))
    }

    val coroutineScope = rememberCoroutineScope()
    var queryJob: Job? = null
    var isLoading by remember { mutableStateOf(true) }

    BackHandler(true) {
        val currentCheckedList = appInfoList.filter { it.isChecked.value }.map {
            it.packageName
        }.toSet()
        if (currentCheckedList.notEqualsTo(tmpCheckedList)) {
            modifiedDialogVisibility.value = true
        } else {
            navigator.pop()
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
                    MyAppInfo(
                        appName = appInfo.loadLabel(pm).toString(),
                        packageName = appInfo.packageName,
                        icon = appInfo.loadIcon(pm),
                        isChecked = mutableStateOf(appInfo.packageName in tmpCheckedList),
                        isSystemApp = appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
                    )
                }.sortedWith(
                    // 按应用类别排序：已勾选的应用优先显示
                    compareByDescending<MyAppInfo> { it.isChecked.value }
                        .thenBy(java.text.Collator.getInstance(java.util.Locale.getDefault())) { it.appName }
                )
            }

            appInfoList = loadedApps
            isLoading = false
        }
    }

    LaunchedEffect(selectSystemAppRequest) {
        if (selectSystemAppRequest) {
            launch(Dispatchers.Default) {
                appInfoList.filter { it.isSystemApp }.forEach {
                    it.isChecked.value = true
                }
                // 触发重新过滤和排序
                withContext(Dispatchers.Main) {
                    sortTrigger++
                }
            }
        }
    }

    LaunchedEffect(clearSelectedRequest) {
        if (clearSelectedRequest) {
            launch(Dispatchers.Default) {
                appInfoList.forEach {
                    it.isChecked.value = false
                }
                // 触发重新过滤和排序
                withContext(Dispatchers.Main) {
                    sortTrigger++
                }
            }
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

            // 排序：已勾选的应用优先，然后按应用名称排序
            val sorted = filtered.sortedWith(
                compareByDescending<MyAppInfo> { it.isChecked.value }
                    .thenBy(java.text.Collator.getInstance(java.util.Locale.getDefault())) { it.appName }
            )

            // 切换回主线程更新 UI
            withContext(Dispatchers.Main) {
                appInfoFilter = sorted
            }
        }
    }

    HyperXScaffold(
        modifier = Modifier
            .fillMaxSize()
            .scrollEndHaptic(),
        containerColor = containerColor,
        layoutPadding = layoutPadding,
        topBar = { contentPadding ->
            TopAppBar(
                color = if (blurEnabled) Color.Transparent else MiuixTheme.colorScheme.surface,
                title = title,
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    IconButton(
                        modifier = Modifier
                            .padding(start = 21.dp)
                            .size(40.dp),
                        onClick = {
                            val currentCheckedList = appInfoList.filter { it.isChecked.value }.map {
                                it.packageName
                            }.toSet()
                            if (currentCheckedList.notEqualsTo(tmpCheckedList)) {
                                modifiedDialogVisibility.value = true
                            } else {
                                navigator.pop()
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
                actions = {
                    if (isTopPopupExpanded.value) {
                        OverlayListPopup(
                            show = showTopPopup.value,
                            popupPositionProvider = ListPopupDefaults.ContextMenuPositionProvider,
                            alignment = PopupPositionProvider.Align.TopEnd,
                            onDismissRequest = {
                                showTopPopup.value = false
                                isTopPopupExpanded.value = false
                            }
                        ) {
                            ListPopupColumn {
                                DropdownImpl(
                                    text = stringResource(R.string.select_system_apps),
                                    optionSize = 2,
                                    isSelected = false,
                                    onSelectedIndexChange = {
                                        selectSystemAppRequest = true
                                        clearSelectedRequest = false
                                        showTopPopup.value = false
                                        isTopPopupExpanded.value = false
                                    },
                                    index = 0
                                )
                                DropdownImpl(
                                    text = stringResource(R.string.clear_selected_apps),
                                    optionSize = 2,
                                    isSelected = false,
                                    onSelectedIndexChange = {
                                        selectSystemAppRequest = false
                                        clearSelectedRequest = true
                                        showTopPopup.value = false
                                        isTopPopupExpanded.value = false
                                    },
                                    index = 1
                                )
                            }
                        }
                        showTopPopup.value = true
                    }
                    AnimatedVisibility(
                        queryString.isBlank()
                    ) {
                        IconButton(
                            modifier = Modifier
                                .padding(end = 21.dp)
                                .size(40.dp),
                            onClick = {
                                isTopPopupExpanded.value = true
                            }
                        ) {
                            Icon(
                                imageVector = MiuixIcons.MoreCircle,
                                contentDescription = "Menu"
                            )
                        }
                    }
                },
                defaultWindowInsetsPadding = false,
                titlePadding = 28.dp + contentPadding.calculateStartPadding(LocalLayoutDirection.current),
                navigationIconPadding = contentPadding.calculateStartPadding(LocalLayoutDirection.current),
                actionIconPadding = contentPadding.calculateEndPadding(LocalLayoutDirection.current)
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
                color = if (blurEnabled) Color.Transparent else MiuixTheme.colorScheme.surface,
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
                            val currentCheckedList = appInfoList.filter { it.isChecked.value }.map {
                                it.packageName
                            }.toMutableSet()
                            prefs.edit { put(checkedListKey, currentCheckedList) }
                            tmpCheckedList.apply {
                                clear()
                                addAll(prefs.get(checkedListKey))
                            }
                            coroutineScope.launch {
                                context.let {
                                    Toast.makeText(it, it.getString(R.string.save_successful), Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }
            }
        },
        blurTopBar = blurEnabled,
        topBarBlurFractionProvider = { scrollBehavior.state.overlappedFraction },
        blurBottomBar = blurEnabled,
        blurTintAlpha = blurTintAlpha,
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
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 6.dp),
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
                        insideMargin = PaddingValues(start = 16.dp, top = 16.dp, bottom = 16.dp),
                        summary = stringResource(R.string.save_hint),
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
            extraContent?.let { it1 -> it1() }
            if (isLoading) {
                item {
                    SmallTitle(
                        text = stringResource(R.string.loading),
                        modifier = Modifier.padding(top = 6.dp),
                        textColor = MiuixTheme.colorScheme.onBackgroundVariant
                    )
                }
            } else {
                itemsIndexed(appInfoFilter, key = { index, item ->
                    item.packageName + item.isChecked + index + appInfoFilter.size
                }) { index, item ->
                    val topCornerRadius = if (index == 0) CardDefaults.CornerRadius else 0.dp
                    val bottomCornerRadius = if (index == appInfoFilter.size - 1) CardDefaults.CornerRadius else 0.dp
                    SpliceCard(
                        topCornerRadius,
                        bottomCornerRadius
                    ) {
                        SwitchPreference(
                            icon = ImageIcon(
                                bitmap = item.icon.toBitmap().asImageBitmap(),
                                size = IconSize.App
                            ),
                            title = item.appName,
                            summary = item.packageName,
                            checked = item.isChecked,
                            onCheckedChange = {
                                coroutineScope.launch {
                                    delay(200)
                                    sortTrigger++
                                }
                            }
                        )
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(6.dp))
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
            navigator.pop()
        }
    )
}

@Composable
fun SpliceCard(
    topCornerRadius: Dp,
    bottomCornerRadius: Dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = remember(topCornerRadius, bottomCornerRadius) {
        if (topCornerRadius == 0.dp && bottomCornerRadius == 0.dp)
            RectangleShape
        else
            RoundedCornerShape(
                topStart = topCornerRadius, topEnd = topCornerRadius,
                bottomStart = bottomCornerRadius, bottomEnd = bottomCornerRadius
            )
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = 12.dp,
                end = 12.dp,
                top = if (topCornerRadius != 0.dp) 6.dp else 0.dp,
                bottom = if (bottomCornerRadius != 0.dp) 6.dp else 0.dp
            ),
        shape = shape,
        color = MiuixTheme.colorScheme.surfaceContainer,
        contentColor = MiuixTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier.padding(CardDefaults.InsideMargin),
            content = content
        )
    }
}

data class MyAppInfo(
    val appName: String,
    val packageName: String,
    val icon: Drawable,
    // 该 isChecked 用于存储应用是否被勾选, 0 为未勾选, 1 为勾选
    var isChecked: MutableState<Boolean>,
    val isSystemApp: Boolean
)
