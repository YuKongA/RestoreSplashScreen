# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
-keepclassmembers class * implements androidx.viewbinding.ViewBinding {
    *** inflate(android.view.LayoutInflater);
}
-keep class kotlin.Unit
-keep class com.gswxxn.restoresplashscreen.ui.MainActivity

# YukiHookAPI 1.3.x 的反射后端 KavaRef 引用了仅存在于 JDK 的类，Android 上不可用，忽略告警
-dontwarn java.lang.reflect.AnnotatedType