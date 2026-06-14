package com.gswxxn.restoresplashscreen.hook.systemui

import com.gswxxn.restoresplashscreen.data.preference.Preferences
import com.gswxxn.restoresplashscreen.hook.SystemUIHooker
import com.gswxxn.restoresplashscreen.hook.base.BaseHookHandler
import com.gswxxn.restoresplashscreen.hook.utils.HookExt.printLog

/**
 * 此对象用于处理针对小米的 Hook
 */
object XiaomiHookHandler : BaseHookHandler() {

    /** 开始 Hook */
    override fun onHook() {
        /**
         * 背景 - 移除截图背景
         *
         * 类原始位置在 miui-framework.jar 中
         *
         * 此处在 com.android.wm.shell.startingsurface.SplashscreenContentDrawer
         *   .$StartingWindowViewBuilder.fillViewWithIcon() 中被调用
         *
         * 原理为干预 fillViewWithIcon() 中的 if 判断，使其将启动器判断为不是小米桌面
         */
        SystemUIHooker.Members.isMiuiHome_TaskSnapshotHelperImpl.addBeforeHook {
            if (prefs.get(Preferences.Background.REMOVE_BG_DRAWABLE)) {
                resultFalse()
                printLog("isMiuiHome(): set isMiuiHome() false")
            }
        }

        /**
         * 背景 - 忽略深色模式
         *
         * 类原始位置在 framework.jar 中
         *
         * 此处在 com.android.wm.shell.startingsurface.SplashscreenContentDrawer
         *   .$StartingWindowViewBuilder.fillViewWithIcon() 中被调用
         */
        SystemUIHooker.Members.updateForceDarkSplashScreen_ForceDarkHelperStubImpl.addBeforeHook {
            if (prefs.get(Preferences.Background.IGNORE_DARK_MODE)) {
                resultFalse()
                printLog("isStaringWindowUnderNightMode(): ignore dark mode")
            }
        }
    }
}
