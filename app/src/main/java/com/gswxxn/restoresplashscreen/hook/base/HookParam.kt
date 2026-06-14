package com.gswxxn.restoresplashscreen.hook.base

import io.github.libxposed.api.XposedInterface

/**
 * Hook 回调作用域，包装 libxposed 的 [XposedInterface.Chain]
 *
 * - before 阶段：调用 [resultTrue]/[resultFalse]/[resultNull] 或给 [result] 赋值会**拦截原方法**；也可通过 [args] 修改入参
 * - after 阶段：[result] 即原方法返回值，可读可改
 * - replace：回调返回值即最终返回值；用 [callOriginal] 主动调用原方法
 */
class HookParam internal constructor(private val chain: XposedInterface.Chain) {

    internal enum class Phase { BEFORE, AFTER, REPLACE }

    internal var phase: Phase = Phase.BEFORE
    internal var intercepted: Boolean = false

    /** 被 Hook 方法的 this 对象（静态方法为 null） */
    val instance: Any? get() = chain.thisObject

    // 可变入参副本：首次访问时从 chain 拷贝；修改后调用原方法会使用改后的值
    private var mutableArgs: Array<Any?>? = null
    val args: Array<Any?>
        get() = mutableArgs ?: chain.args.toTypedArray().also { mutableArgs = it }

    fun args(index: Int): ArgAccessor = ArgAccessor(this, index)

    var result: Any? = null
        set(value) {
            field = value
            hasResult = true
            if (phase == Phase.BEFORE) intercepted = true
        }
    internal var hasResult: Boolean = false

    /** 拦截原方法并将返回值设为 true（仅 before 阶段有"拦截"含义） */
    fun resultTrue() { result = true }
    fun resultFalse() { result = false }
    fun resultNull() { result = null }

    /** 调用原方法（replace 场景使用）。会带上可能被修改过的 [args] */
    fun callOriginal(): Any? {
        val a = mutableArgs
        return if (a != null) chain.proceed(a) else chain.proceed()
    }

    internal fun proceedOriginal(): Any? = callOriginal()

    class ArgAccessor internal constructor(private val param: HookParam, private val index: Int) {
        fun any(): Any? = param.args[index]
        fun string(): String = param.args[index] as String
        fun stringOrNull(): String? = param.args[index] as? String
        fun boolean(): Boolean = param.args[index] as Boolean
        fun int(): Int = param.args[index] as Int
        fun long(): Long = param.args[index] as Long
        fun set(value: Any?) { param.args[index] = value }
    }
}
