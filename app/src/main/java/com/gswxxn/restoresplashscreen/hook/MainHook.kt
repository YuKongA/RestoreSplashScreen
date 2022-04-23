package com.gswxxn.restoresplashscreen.hook

import android.content.pm.ActivityInfo
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import com.gswxxn.restoresplashscreen.Data.DataConst
import com.highcapable.yukihookapi.YukiHookAPI.configs
import com.highcapable.yukihookapi.annotation.xposed.InjectYukiHookWithXposed
import com.highcapable.yukihookapi.hook.factory.encase
import com.highcapable.yukihookapi.hook.log.loggerI
import com.highcapable.yukihookapi.hook.log.loggerW
import com.highcapable.yukihookapi.hook.type.java.BooleanType
import com.highcapable.yukihookapi.hook.xposed.proxy.YukiHookXposedInitProxy
import de.robv.android.xposed.XposedHelpers

@InjectYukiHookWithXposed
class MainHook : YukiHookXposedInitProxy {
    override fun onInit() = configs {
        debugTag = "RestoreSplashScreen"
        isDebug = false
    }

    override fun onHook() = encase {
        when {
            prefs.get(DataConst.ENABLE_MODULE).not() -> loggerW(msg = "Aborted Hook -> Hook Closed")
            else -> loadApp("com.android.systemui") {

//            XposedHelpers.findClass(
//                "com.android.wm.shell.startingsurface.SplashscreenContentDrawer\$StartingWindowViewBuilder",
//                lpparam.classLoader
//            )
//                .let {
//
//                    XposedHelpers.findAndHookMethod(it, "build",
//                        object : XC_MethodHook() {
//                            override fun beforeHookedMethod(param: MethodHookParam?) {
//                                super.beforeHookedMethod(param)
//
//                                XposedBridge.log(
//                                    "[RestoreSplashScreen] " +
//                                            "${
//                                                XposedHelpers.getObjectField(
//                                                    XposedHelpers.getObjectField(
//                                                        param?.thisObject,
//                                                        "mActivityInfo"
//                                                    ) as ActivityInfo, "packageName"
//                                                )
//                                            }" +
//                                            " mSuggestType = " +
//                                            "${XposedHelpers.getIntField(param?.thisObject, "mSuggestType")}"
//                                )
//                            }
//                        })
//                }

//                    XposedHelpers.findClass(
//                "com.android.wm.shell.startingsurface.SplashscreenContentDrawer\$StartingWindowViewBuilder",
//                lpparam.classLoader
//            ).let {
//
//                XposedBridge.hookAllMethods(it, "fillViewWithIcon",
//                    object : XC_MethodHook() {
//                        override fun beforeHookedMethod(param: MethodHookParam?) {
//                            super.beforeHookedMethod(param)
//
//                            if (XposedHelpers.getObjectField(
//                                    XposedHelpers.getObjectField(
//                                        param?.thisObject,
//                                        "mActivityInfo"
//                                    ) as ActivityInfo, "packageName"
//                                ) == "com.coolapk.market"){
//                                XposedHelpers.setIntField(param?.thisObject, "mThemeColor", Color.parseColor("#800080"))
//                            }
//                                XposedBridge.log("[RestoreSplashScreen] change color")
//                        }
//                    })
//            }
                fun printLog (msg : String){ if (prefs.get(DataConst.ENABLE_LOG)) loggerI(msg = msg) }

                // 关闭MIUI优化
                findClass("com.android.wm.shell.startingsurface.SplashscreenContentDrawer").hook {
                    // 借助 mContentSuggestType 传递作用域 flag
                    injectMember {
                        method {
                            name = "makeSplashScreenContentView"
                            paramCount = 3
                        }
                        beforeHook {
                            val packageName = args(1).cast<ActivityInfo>()?.packageName
                            val list = prefs.get(DataConst.CUSTOM_SCOPE_LIST)
                            val isException = prefs.get(DataConst.IS_CUSTOM_SCOPE_EXCEPTION_MODE)

                            if (!prefs.get(DataConst.ENABLE_CUSTOM_SCOPE)
                                || isException && !(packageName in list)
                                || !isException && packageName in list
                            ) {
                                printLog("${packageName}: set enable splash screen flag")
                                XposedHelpers.setIntField(instance, "mContentSuggestType", 10)
                            } else { printLog("${packageName}: not set enable splash screen flag") }
                        }
                    }

                    // 主要 Hook 函数
                    injectMember {
                        method {
                            name = "isCTS"
                            emptyParam()
                        }
                        beforeHook {
                            if (XposedHelpers.getIntField(instance, "mContentSuggestType") == 10) {
                                printLog("isCTS() return true")
                                result = true
                            }
                        }
                    }
                }


                // 即使是自适应图标也按不自适应处理，防止使用第三方主题时出现方角
                findClass("com.android.wm.shell.startingsurface.SplashscreenContentDrawer\$StartingWindowViewBuilder")
                    .hook {
                        injectMember {
                            method {
                                name = "processAdaptiveIcon"
                                param(Drawable::class.java)
                            }
                            beforeHook {
                                printLog("set adaptable icon not adapt")
                                result = false
                            }
                        }
                    }

                // 为图标绘制圆角
                findClass("com.android.launcher3.icons.BaseIconFactory").hook {
                    injectMember {
                        method {
                            name = "createScaledBitmapWithoutShadow"
                            param(Drawable::class.java, BooleanType)
                        }
                        beforeHook {
                            args(0).set(
                                BitmapDrawable(
                                    appContext.resources,
                                    Utils.roundBitmapByShader(
                                        args(0).cast<Drawable>()?.let { Utils.drawable2Bitmap(it) },
                                        XposedHelpers.getIntField(instance, "mIconBitmapSize"),
                                        Utils.dp2px(appContext, 45F)
                                    )
                                )
                            )
                            printLog("set not adaptable icon round corner")
                            args(1).set(false)
                            printLog("set large icon")
                        }
                    }
                }

            }

        }
    }
}