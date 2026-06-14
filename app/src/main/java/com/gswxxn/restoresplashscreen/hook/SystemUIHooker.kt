package com.gswxxn.restoresplashscreen.hook

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.ComponentInfo
import androidx.annotation.Keep
import com.gswxxn.restoresplashscreen.hook.base.HookManager
import com.gswxxn.restoresplashscreen.hook.systemui.BgHookHandler
import com.gswxxn.restoresplashscreen.hook.systemui.BottomHookHandler
import com.gswxxn.restoresplashscreen.hook.systemui.OplusHookHandler
import com.gswxxn.restoresplashscreen.hook.systemui.GenerateHookHandler
import com.gswxxn.restoresplashscreen.hook.systemui.IconHookHandler
import com.gswxxn.restoresplashscreen.hook.systemui.XiaomiHookHandler
import com.gswxxn.restoresplashscreen.hook.systemui.ScopeHookHandler
import com.gswxxn.restoresplashscreen.hook.utils.HookExt.isColorOS
import com.gswxxn.restoresplashscreen.hook.utils.HookExt.isMIUI
import com.gswxxn.restoresplashscreen.hook.utils.HookExt.loadHookHandler
import com.gswxxn.restoresplashscreen.utils.MLog
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.classOf
import com.highcapable.kavaref.extension.makeAccessible
import com.highcapable.kavaref.extension.toClass
import com.highcapable.kavaref.extension.toClassOrNull
import io.github.libxposed.api.XposedModule

/**
 * SystemUI 进程内的 Hook 入口
 */
object SystemUIHooker {

    lateinit var module: XposedModule
        private set

    lateinit var classLoader: ClassLoader
        private set

    /** 宿主（SystemUI）Application 的 Context */
    @Volatile
    var appContext: Context? = null
        private set

    /** 当前用户 ID */
    val appUserId: Int = android.os.Process.myUid() / 100000

    @Keep
    object Members {
        val makeSplashScreenContentView = HookManager {
            "com.android.wm.shell.startingsurface.SplashscreenContentDrawer".toClassOrNull(loader = classLoader)
                ?.resolve()?.optional()?.firstMethodOrNull { name = "makeSplashScreenContentView" }?.self
        }
        val getWindowAttrs = HookManager {
            "com.android.wm.shell.startingsurface.SplashscreenContentDrawer".toClassOrNull(loader = classLoader)
                ?.resolve()?.optional()?.firstMethodOrNull {
                    name = "getWindowAttrs"
                    parameterCount = 2
                }?.self
        }
        val getBGColorFromCache = HookManager {
            "com.android.wm.shell.startingsurface.SplashscreenContentDrawer".toClassOrNull(loader = classLoader)
                ?.resolve()?.optional()?.firstMethodOrNull {
                    name = "getBGColorFromCache"
                    parameterCount = 2
                }?.self
        }
        val startingWindowViewBuilderConstructor = HookManager {
            ("com.android.wm.shell.startingsurface.SplashscreenContentDrawer\$StartingWindowViewBuilder".toClassOrNull(loader = classLoader)
                ?: "com.android.wm.shell.startingsurface.SplashscreenContentDrawer\$SplashViewBuilder".toClassOrNull(loader = classLoader)) // Android 14
                ?.resolve()?.optional()?.firstConstructorOrNull { parameterCount { it in 2..3 } }?.self
        }
        val createIconDrawable = HookManager {
            ("com.android.wm.shell.startingsurface.SplashscreenContentDrawer\$StartingWindowViewBuilder".toClassOrNull(loader = classLoader)
                ?: "com.android.wm.shell.startingsurface.SplashscreenContentDrawer\$SplashViewBuilder".toClassOrNull(loader = classLoader)) // Android 14
                ?.resolve()?.optional()?.firstMethodOrNull { name = "createIconDrawable" }?.self
        }
        val iconColor_constructor = HookManager {
            "com.android.wm.shell.startingsurface.SplashscreenContentDrawer\$ColorCache\$IconColor".toClassOrNull(loader = classLoader)
                ?.resolve()?.optional()?.firstConstructorOrNull()?.self
        }
        val getIcon_IconProvider = HookManager(!isColorOS) {
            "com.android.launcher3.icons.IconProvider".toClassOrNull(loader = classLoader)?.resolve()?.optional()?.firstMethodOrNull {
                name = "getIcon"
                parameterCount = 2
                parameters { types ->
                    Integer.TYPE in types &&
                            (classOf<ActivityInfo>() in types || classOf<ComponentInfo>() in types)
                }
            }?.self
        }
        val normalizeAndWrapToAdaptiveIcon = HookManager {
            "com.android.launcher3.icons.BaseIconFactory".toClassOrNull(loader = classLoader)?.resolve()?.optional()?.firstMethodOrNull {
                name = "normalizeAndWrapToAdaptiveIcon"
            }?.self
        }
        val createIconBitmap_BaseIconFactory = HookManager {
            "com.android.launcher3.icons.BaseIconFactory".toClassOrNull(loader = classLoader)?.resolve()?.optional()?.firstMethodOrNull {
                name = "createIconBitmap"
                parameters(android.graphics.drawable.Drawable::class, Float::class, Int::class)
            }?.self
        }
        val build_SplashScreenViewBuilder = HookManager {
            "android.window.SplashScreenView\$Builder".toClassOrNull(loader = classLoader)?.resolve()?.optional()?.firstMethodOrNull {
                name = "build"
            }?.self
        }
        val removeStartingWindow = HookManager {
            "com.android.wm.shell.ShellTaskOrganizer".toClassOrNull(loader = classLoader)?.resolve()?.optional()?.firstMethodOrNull {
                name = "removeStartingWindow"
            }?.self
        }

        // Xiaomi
        val isMiuiHome_TaskSnapshotHelperImpl = HookManager(
            isMIUI && "android.app.TaskSnapshotHelperImpl".toClassOrNull(loader = classLoader) != null
        ) {
            "android.app.TaskSnapshotHelperImpl".toClassOrNull(loader = classLoader)?.resolve()?.optional()?.firstMethodOrNull {
                name = "isMiuiHome"
                parameters(String::class)
            }?.self
        }
        val updateForceDarkSplashScreen_ForceDarkHelperStubImpl = HookManager(isMIUI) {
            $$"android.window.SplashScreenView$Builder".toClassOrNull(loader = classLoader)?.resolve()?.optional()?.firstMethodOrNull {
                name = "isStaringWindowUnderNightMode"
                emptyParameters()
            }?.self ?: "android.view.ForceDarkHelperStubImpl".toClassOrNull(loader = classLoader)?.resolve()?.optional()
                ?.firstMethodOrNull {
                    name = "updateForceDarkSplashScreen"
                }?.self
        }

        // Oplus
        val setContentViewBackground_OplusShellStartingWindowManager = HookManager(isColorOS) {
            "com.android.wm.shell.startingsurface.OplusShellStartingWindowManager".toClassOrNull(loader = classLoader)?.resolve()
                ?.optional()?.firstMethodOrNull {
                name = "setContentViewBackground"
            }?.self
        }
        val getIconExt_OplusShellStartingWindowManager = HookManager(isColorOS) {
            "com.android.wm.shell.startingsurface.OplusShellStartingWindowManager".toClassOrNull(loader = classLoader)?.resolve()
                ?.optional()?.firstMethodOrNull {
                name = "getIconExt"
                parameterCount { it in 4..6 }
            }?.self
        }
        val getWindowAttrsIfPresent_OplusShellStartingWindowManager = HookManager(isColorOS) {
            "com.android.wm.shell.startingsurface.OplusShellStartingWindowManager".toClassOrNull(loader = classLoader)?.resolve()
                ?.optional()?.firstMethodOrNull {
                name = "getWindowAttrsIfPresent"
            }?.self
        }
    }

    /** 是否已经完成首次 Hook（attachBaseContext 可能被多次调用，仅首次执行） */
    @Volatile
    private var isHooked = false

    /**
     * 由外部在 SystemUI 进程加载时调用：注入 [module]/[classLoader]，
     * Hook `Application#attachBaseContext` 以捕获宿主 Context 并触发实际 Hook。
     */
    fun init(module: XposedModule, classLoader: ClassLoader) {
        this.module = module
        this.classLoader = classLoader

        HookManager {
            "android.app.Application".toClass(loader = classLoader).resolve().optional().firstMethodOrNull {
                name = "attachBaseContext"
                parameters(Context::class)
                superclass()
            }?.self
        }.addAfterHook({ true }) {
            if (isHooked) return@addAfterHook
            appContext = args(0).any() as? Context
            isHooked = true
            onHook()
        }.startHook(module)
    }

    /**
     * 热重载后由新一代代码调用：复用上一代捕获的宿主 [classLoader] 与 [appContext]，直接重新安装功能 Hook。
     *
     * 热重载不会重放 `attachBaseContext`（宿主 Application 早已创建），因此不能走 [init] 的捕获流程，
     * 需在 classLoader/Context 就绪后直接执行 [onHook]。[Members] 在新一代为全新单例，
     * 首次访问时会基于此处设置的 [classLoader] 解析宿主成员。
     */
    fun reHook(module: XposedModule, classLoader: ClassLoader, appContext: Context?) {
        this.module = module
        this.classLoader = classLoader
        this.appContext = appContext
        isHooked = true
        onHook()
    }

    /** 开始 Hook */
    private fun onHook() {
        loadHookHandler(
            GenerateHookHandler,
            ScopeHookHandler,
            IconHookHandler,
            BottomHookHandler,
            BgHookHandler,
            XiaomiHookHandler,
            OplusHookHandler
        )

        // 执行 Hook
        Members.javaClass.declaredFields.forEach { field ->
            field.makeAccessible()
            val hookManager = field.get(null)

            if (hookManager is HookManager) try {
                hookManager.startHook(module)
            } catch (e: Throwable) {
                MLog.e(e)
            }
        }
    }
}
