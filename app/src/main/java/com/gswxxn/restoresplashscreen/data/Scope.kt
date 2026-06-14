package com.gswxxn.restoresplashscreen.data

/**
 * libxposed 作用域关键字。
 *
 * scope.list 现为动态作用域（staticScope=false），仅推荐 [SYSTEM_UI]，故启用模块时**默认只勾选** SystemUI。
 * [SYSTEM]（system_server）为可选作用域：需用户在管理器中自行勾选「Android System」，
 * AndroidHooker 的强制显示 / 彻底关闭 / 热启动兼容功能方可生效。
 */
object Scope {
    const val SYSTEM = "system"
    const val SYSTEM_UI = "com.android.systemui"
}
