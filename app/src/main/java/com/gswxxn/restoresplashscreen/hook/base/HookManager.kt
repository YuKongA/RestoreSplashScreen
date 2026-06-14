package com.gswxxn.restoresplashscreen.hook.base

import com.gswxxn.restoresplashscreen.utils.MLog
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import java.lang.reflect.Executable
import java.lang.reflect.Method

/**
 * 用于进行 Hook 操作的 HookManager
 *
 * @param createCondition 创建条件（如系统判断）
 * @param block 返回要 Hook 的成员
 */
class HookManager(private val createCondition: Boolean = true, block: () -> Executable?) {

    companion object {
        // 默认不执行 Hook；获取到包名后会被替换为「仅作用域内应用才执行」
        var defaultExecCondition: (() -> Boolean) = { false }
    }

    private var member: Executable? = null

    private val beforeHooks = mutableListOf<HookParam.() -> Unit>()
    private val afterHooks = mutableListOf<HookParam.() -> Unit>()
    private var replaceHook: (HookParam.() -> Any?)? = null

    private val hasReplaceHook get() = replaceHook != null
    private val hasBeforeHooks get() = beforeHooks.isNotEmpty()
    private val hasAfterHooks get() = afterHooks.isNotEmpty()

    init {
        if (createCondition) try {
            member = block()
        } catch (e: Throwable) {
            MLog.e(e)
        }
    }

    /** 若 member 是方法，返回其返回类型，否则 null */
    val returnType get() = (member as? Method)?.returnType

    fun addBeforeHook(execCondition: (() -> Boolean) = defaultExecCondition, block: HookParam.() -> Unit): HookManager {
        beforeHooks += { if (execCondition()) block() }
        return this
    }

    fun addAfterHook(execCondition: (() -> Boolean) = defaultExecCondition, block: HookParam.() -> Unit): HookManager {
        afterHooks += { if (execCondition()) block() }
        return this
    }

    /** ReplaceHook 只能存在一个，后加的覆盖先加的；条件不满足时调用原方法 */
    fun addReplaceHook(execCondition: (() -> Boolean) = defaultExecCondition, block: HookParam.() -> Any?): HookManager {
        replaceHook = { if (execCondition()) block() else callOriginal() }
        return this
    }

    fun startHook(module: XposedModule) {
        val m = member ?: return
        if (hasReplaceHook && (hasBeforeHooks || hasAfterHooks)) {
            MLog.w { "Conflict: ReplaceHook 与 Before/After 不应共存，before/after 将被忽略。成员: ${m.declaringClass.name}#${m.name}" }
        }
        try {
            module.hook(m).intercept(object : XposedInterface.Hooker {
                override fun intercept(chain: XposedInterface.Chain): Any? {
                    val param = HookParam(chain)

                    // ReplaceHook 优先且独占
                    if (hasReplaceHook) {
                        param.phase = HookParam.Phase.REPLACE
                        return replaceHook!!.invoke(param)
                    }

                    // before：依次执行（任一拦截则跳过原方法）
                    param.phase = HookParam.Phase.BEFORE
                    beforeHooks.forEach { it(param) }
                    val skipOriginal = param.intercepted

                    // 原方法（除非被 before 拦截）
                    param.phase = HookParam.Phase.AFTER
                    if (!skipOriginal) {
                        param.result = param.proceedOriginal()
                    }

                    // after：依次执行（可读写 result）
                    afterHooks.forEach { it(param) }

                    return param.result
                }
            })
        } catch (e: Throwable) {
            MLog.e(e)
        }
    }
}
