package com.gswxxn.restoresplashscreen.ui.apppage

import android.content.pm.ApplicationInfo
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.gswxxn.restoresplashscreen.R
import com.gswxxn.restoresplashscreen.data.DataConst
import com.gswxxn.restoresplashscreen.data.Route
import com.gswxxn.restoresplashscreen.ui.component.MyAppInfo
import com.gswxxn.restoresplashscreen.ui.component.SpliceCard
import com.gswxxn.restoresplashscreen.ui.component.TextPreference
import com.gswxxn.restoresplashscreen.utils.CommonUtils.toMap
import com.highcapable.yukihookapi.hook.factory.prefs
import dev.lackluster.hyperx.navigation.LocalNavigator
import dev.lackluster.hyperx.ui.component.IconSize
import dev.lackluster.hyperx.ui.component.ImageIcon
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
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.SearchBar
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/**
 * 单独配置背景颜色
 */
@Composable
fun BgIndividualPage() {
    val context = LocalContext.current

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
    var queryString by remember { mutableStateOf("") }

    // 完整应用列表
    var appInfoList by remember { mutableStateOf<List<MyAppInfo>>(emptyList()) }

    // 在列表中的条目
    var appInfoFilter by remember { mutableStateOf<List<MyAppInfo>>(emptyList()) }

    var queryJob: Job? = null
    var isLoading by remember { mutableStateOf(true) }
    val deviceDarkMode = isSystemInDarkTheme()

    LaunchedEffect(Unit) {
        launch {
            isLoading = true
            // 使用 IO 调度器进行耗时操作
            val loadedApps = withContext(Dispatchers.IO) {
                val configMapPrefs =
                    if (deviceDarkMode) DataConst.INDIVIDUAL_BG_COLOR_APP_MAP_DARK else DataConst.INDIVIDUAL_BG_COLOR_APP_MAP
                val tmpCheckedList = mutableMapOf<String, String>().apply {
                    clear()
                    putAll(context.prefs().get(configMapPrefs).toMap())
                }
                val pm = context.packageManager
                val installedApps = pm.getInstalledApplications(0)

                // 创建应用信息列表并按字母顺序排序
                installedApps.map { appInfo ->
                    MyAppInfo(
                        appName = appInfo.loadLabel(pm).toString(),
                        packageName = appInfo.packageName,
                        icon = appInfo.loadIcon(pm),
                        isChecked = mutableStateOf(appInfo.packageName in tmpCheckedList.keys),
                        isSystemApp = appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
                    )
                }.sortedBy { it.appName }
            }

            appInfoList = loadedApps
            isLoading = false
        }
    }

    LaunchedEffect(appInfoList, queryString) {
        if (appInfoList.isEmpty()) return@LaunchedEffect

        queryJob?.cancel()
        queryJob = launch(Dispatchers.Default) {
            // 添加防抖延迟
            if (queryString.isNotBlank()) {
                delay(300)
            } else {
                delay(50)
            }

            // 在后台线程进行过滤
            val filtered = if (queryString.isBlank()) {
                appInfoList
            } else {
                appInfoList.filter {
                    it.appName.contains(queryString, true) || it.packageName.contains(queryString, true)
                }
            }

            // 切换回主线程更新 UI
            withContext(Dispatchers.Main) {
                appInfoFilter = filtered
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
                title = stringResource(R.string.configure_bg_colors_individually),
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    IconButton(
                        modifier = Modifier
                            .padding(start = 21.dp)
                            .size(40.dp),
                        onClick = {
                            navigator.pop()
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
                titlePadding = 28.dp + contentPadding.calculateStartPadding(LocalLayoutDirection.current),
                navigationIconPadding = contentPadding.calculateStartPadding(LocalLayoutDirection.current),
                actionIconPadding = contentPadding.calculateEndPadding(LocalLayoutDirection.current)
            )
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
                        insideMargin = PaddingValues(16.dp),
                        summary = stringResource(R.string.custom_bg_color_sub_setting_hint),
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
                        TextPreference(
                            icon = ImageIcon(
                                bitmap = item.icon.toBitmap().asImageBitmap(),
                                size = IconSize.App
                            ),
                            title = item.appName,
                            summary = item.packageName
                        ) {
                            navigator.push(Route.ColorPicker(item.packageName))
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}
