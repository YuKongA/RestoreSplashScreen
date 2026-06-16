package com.gswxxn.restoresplashscreen.manager

import com.gswxxn.restoresplashscreen.data.Scope
import com.gswxxn.restoresplashscreen.utils.XMLog
import io.github.libxposed.service.HookedTarget
import io.github.libxposed.service.HotReloadResult
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicInteger

class XposedServiceManager : XposedServiceHelper.OnServiceListener {
    private val _serviceFlow = MutableStateFlow<XposedService?>(null)
    val serviceFlow = _serviceFlow.asStateFlow()

    val currentService: XposedService?
        get() = _serviceFlow.value

    init {
        XposedServiceHelper.registerListener(this)
    }

    override fun onServiceBind(service: XposedService) {
        XMLog.d {
            "XposedServiceManager.onServiceBind"
        }
        _serviceFlow.value = service
    }

    override fun onServiceDied(service: XposedService) {
        XMLog.d {
            "XposedServiceManager.onServiceDied"
        }
        _serviceFlow.value = null
    }

    /**
     * 被 Hook 进程是否需要重启的判定结果。
     *
     * @property systemUI SystemUI 是否需要重启
     * @property android system_server 是否需要重启，null 表示未获取到（未注入 / 不在作用域）
     */
    data class RestartState(val systemUI: Boolean, val android: Boolean?)

    /**
     * 通过 libxposed service API 102 的 [XposedService.getRunningTargets] 判定各被 Hook 进程
     * 是否仍在运行旧模块代码（[HookedTarget.State.STALE]）。
     *
     * 框架低于 API 102 或查询失败时返回 null（调用方应据此保持安全默认值，避免误报）。
     */
    fun queryRestartState(): RestartState? {
        val service = currentService ?: return null
        if (service.apiVersion < XposedService.API_102) return null
        val targets = runCatching { service.runningTargets }.getOrNull() ?: return null
        return RestartState(
            systemUI = targets.needRestart(Scope.SYSTEM_UI) ?: false,
            android = targets.needRestart(SYSTEM_SERVER_PROCESS)
        )
    }

    /**
     * 对所有"仍在运行旧代码"（[HookedTarget.State.STALE] / [HookedTarget.State.FAILED]）的目标进程
     * 触发热重载，免重启地应用模块更新。需框架支持 service API 102。
     *
     * 注意：[onComplete] 在最后一个请求返回时回调，可能运行在 **Binder 线程**，调用方需自行切回主线程。
     *
     * @param onComplete 全部请求返回后回调：(成功数, 总数)
     * @return 是否成功发起（框架不支持 / 无服务时返回 false；无 STALE 目标时返回 true 且立即回调 0/0）
     */
    fun hotReloadStaleTargets(onComplete: (succeeded: Int, total: Int) -> Unit): Boolean {
        val service = currentService ?: return false
        if (service.apiVersion < XposedService.API_102) return false
        val targets = runCatching { service.runningTargets }.getOrNull() ?: return false
        val stale = targets.filter {
            it.state == HookedTarget.State.STALE || it.state == HookedTarget.State.FAILED
        }
        if (stale.isEmpty()) {
            onComplete(0, 0)
            return true
        }
        val total = stale.size
        val done = AtomicInteger(0)
        val succeeded = AtomicInteger(0)
        stale.forEach { target ->
            runCatching {
                service.hotReloadModule(target, null) { _, result ->
                    if (result.status == HotReloadResult.Status.SUCCEEDED) succeeded.incrementAndGet()
                    if (done.incrementAndGet() == total) onComplete(succeeded.get(), total)
                }
            }.onFailure {
                XMLog.e(it)
                if (done.incrementAndGet() == total) onComplete(succeeded.get(), total)
            }
        }
        return true
    }

    /**
     * 在运行中的 Hook 目标里查找指定进程，返回其是否需要重启。
     *
     * - [HookedTarget.State.STALE] / [HookedTarget.State.FAILED] → 进程内为旧代码，需重启
     * - [HookedTarget.State.UP_TO_DATE] / [HookedTarget.State.RELOADING] → 无需重启
     * - 未找到该进程 → null（语义：未获取到）
     */
    private fun List<HookedTarget>.needRestart(processName: String): Boolean? =
        firstOrNull { it.processName == processName }
            ?.let { it.state == HookedTarget.State.STALE || it.state == HookedTarget.State.FAILED }

    companion object {
        /** system_server 的进程名（注意区别于 libxposed 作用域关键字 [Scope.SYSTEM]） */
        private const val SYSTEM_SERVER_PROCESS = "system_server"
    }
}
