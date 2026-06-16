package com.gswxxn.restoresplashscreen.hook

import android.content.Context
import com.gswxxn.restoresplashscreen.data.Scope
import com.gswxxn.restoresplashscreen.data.preference.Preferences
import com.gswxxn.restoresplashscreen.hook.systemui.GenerateHookHandler
import com.gswxxn.restoresplashscreen.hook.utils.RemotePreferences
import com.gswxxn.restoresplashscreen.hook.utils.RemotePreferences.observe
import com.gswxxn.restoresplashscreen.utils.XMLog
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface

/**
 * libxposed 模块入口
 */
class HookEntry : XposedModule() {
    private lateinit var processName: String
    private var isSystemServer: Boolean = false

    override fun onModuleLoaded(param: XposedModuleInterface.ModuleLoadedParam) {
        initModule(param.processName, param.isSystemServer)
    }

    /**
     * 初始化模块级单例（进程内全局状态）
     *
     * [onModuleLoaded] 与热重载后的 [onHotReloaded] 都需要执行
     */
    private fun initModule(processName: String, isSystemServer: Boolean) {
        this.processName = processName
        this.isSystemServer = isSystemServer
        XMLog.init(this)
        RemotePreferences.init(this)
        Preferences.Log.ENABLE_LOG.observe { XMLog.isDebugEnabled = it }
    }

    override fun onSystemServerStarting(param: XposedModuleInterface.SystemServerStartingParam) {
        // Android 系统相关 Hook（system_server 进程）
        AndroidHooker.init(this, param.classLoader)
    }

    override fun onPackageLoaded(param: XposedModuleInterface.PackageLoadedParam) {
        if (param.packageName != Scope.SYSTEM_UI) return
        // SystemUI 相关 Hook
        SystemUIHooker.init(this, param.defaultClassLoader)
    }

    /**
     * 热重载即将开始（运行在**旧代码**中）
     *
     * 返回 `true` 放行后，框架会冻结旧代码、捕获旧 Hook 句柄并载入新一代
     */
    override fun onHotReloading(param: XposedModuleInterface.HotReloadingParam): Boolean {
        val state = runCatching {
            when {
                isSystemServer -> AndroidHooker.classLoader?.let { arrayOf<Any?>(it) }
                processName == Scope.SYSTEM_UI -> {
                    GenerateHookHandler.cancelPendingDelays()
                    arrayOf(SystemUIHooker.classLoader, SystemUIHooker.appContext)
                }

                else -> null
            }
        }.getOrNull() ?: return false
        param.setSavedInstanceState(state)
        return true
    }

    /**
     * 热重载完成（运行在**新代码**中）。
     *
     * 框架不会重放 [onSystemServerStarting] / [onPackageLoaded]，因此需手动：先移除旧一代安装的所有
     * Hook，再用上一代交接的宿主 classLoader / Context 重新安装本代 Hook。
     */
    override fun onHotReloaded(param: XposedModuleInterface.HotReloadedParam) {
        param.oldHookHandles.forEach { it.unhook() }

        initModule(param.processName, param.isSystemServer)

        val state = param.savedInstanceState as? Array<*> ?: return
        when {
            param.isSystemServer ->
                (state.getOrNull(0) as? ClassLoader)?.let { AndroidHooker.init(this, it) }

            param.processName == Scope.SYSTEM_UI -> {
                val classLoader = state.getOrNull(0) as? ClassLoader ?: return
                val appContext = state.getOrNull(1) as? Context
                SystemUIHooker.reHook(this, classLoader, appContext)
            }
        }
    }
}
