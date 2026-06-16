package com.gswxxn.restoresplashscreen.repository

import android.content.Context
import android.net.Uri
import com.gswxxn.restoresplashscreen.data.preference.Preferences
import com.gswxxn.restoresplashscreen.utils.XMLog
import com.gswxxn.restoresplashscreen.utils.RemotePreferenceStore
import dev.lackluster.hyperx.ui.layout.HyperXLayoutConfig
import dev.lackluster.hyperx.ui.preference.core.PreferenceKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException

class GlobalPreferencesRepository(
    private val context: Context,
    private val prefStore: RemotePreferenceStore
) {
    private val _uiConfigFlow = MutableStateFlow(
        HyperXLayoutConfig(
            isBlurEnabled = Preferences.Module.MODULE_APP_BLUR.default,
            isSplitScreenEnabled = Preferences.Module.SPLIT_VIEW.default
        )
    )
    val uiConfigFlow = _uiConfigFlow.asStateFlow()

    private val _globalReloadEvent = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val globalReloadEvent = _globalReloadEvent.asSharedFlow()

    private val _preferenceUpdates = MutableSharedFlow<PreferenceKey<*>>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val preferenceUpdates = _preferenceUpdates.asSharedFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        initAndCheck()

        scope.launch {
            prefStore.globalReloadEvent.collect {
                initAndCheck()
                _globalReloadEvent.emit(Unit)
            }
        }
    }

    private fun initAndCheck() {
        XMLog.isDebugEnabled = prefStore.get(Preferences.Log.ENABLE_LOG)

        _uiConfigFlow.value = HyperXLayoutConfig(
            isSplitScreenEnabled = prefStore.get(Preferences.Module.SPLIT_VIEW),
            isBlurEnabled = prefStore.get(Preferences.Module.MODULE_APP_BLUR)
        )
    }

    fun <T : Any> get(key: PreferenceKey<T>): T {
        return prefStore.get(key)
    }

    fun <T : Any> update(key: PreferenceKey<T>, value: T) {
        // 1. 统一持久化写入 SP
        prefStore.put(key, value)

        _preferenceUpdates.tryEmit(key)

        // 2. 精准的局部状态更新（仅对会影响 UI 的 Key 做派发）
        when (key) {
            Preferences.Module.SPLIT_VIEW -> _uiConfigFlow.update { it.copy(isSplitScreenEnabled = value as Boolean) }
            Preferences.Module.MODULE_APP_BLUR -> _uiConfigFlow.update { it.copy(isBlurEnabled = value as Boolean) }

            Preferences.Log.ENABLE_LOG -> XMLog.isDebugEnabled = (value as Boolean)
            // 其余仅需持久化的 Key 在第 1 步已写入 SP，Hook 端可直接读取，这里无需处理。
        }
    }

    suspend fun exportBackup(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val kvs = prefStore.getAll() ?: throw IllegalStateException("SharedPreferences not initialized")
            val outputStream = context.contentResolver.openOutputStream(uri)
                ?: throw IOException("Can't open output stream")

            val jsonObject = JSONObject()
            for ((key, value) in kvs) {
                if (key in Preferences.BACKUP_BLACKLIST) continue
                when (value) {
                    is Int -> jsonObject.put(key, "#i#$value")
                    is Float -> jsonObject.put(key, "#f#$value")
                    is String -> jsonObject.put(key, value)
                    is Boolean -> jsonObject.put(key, value)
                    is Long -> jsonObject.put(key, value)
                    is Set<*> -> jsonObject.put(key, value.joinToString(",", "[", "]"))
                    else -> jsonObject.put(key, value)
                }
            }

            outputStream.bufferedWriter().use { it.write(jsonObject.toString()) }
        }
    }

    suspend fun importBackup(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: throw IOException("Can't open input stream")

            val allText = inputStream.bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(allText)

            if (!checkBackupFileValid(jsonObject)) {
                throw IllegalArgumentException("Unsupported backup version or invalid file format")
            }

            val kvs = mutableMapOf<String, Any>()
            for (key in jsonObject.keys()) {
                if (key in Preferences.BACKUP_BLACKLIST) continue
                when (val value = jsonObject.get(key)) {
                    is Boolean, is Int, is Float -> kvs[key] = value
                    is String -> {
                        if (value.startsWith("[") && value.endsWith("]")) {
                            val stringList = value.removeSurrounding("[", "]").replace(" ", "").split(",")
                            val stringSet = HashSet(stringList)
                            kvs[key] = stringSet
                        } else if (value.startsWith("#i#")) {
                            kvs[key] = value.removePrefix("#i#").toInt()
                        } else if (value.startsWith("#f#")) {
                            kvs[key] = value.removePrefix("#f#").toFloat()
                        } else {
                            kvs[key] = value
                        }
                    }
                }
            }
            prefStore.setAll(kvs)

            initAndCheck()
            _globalReloadEvent.emit(Unit)
        }
    }

    suspend fun resetSettings(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            prefStore.clearAll()
            initAndCheck()
            _globalReloadEvent.emit(Unit)
        }
    }

    private fun checkBackupFileValid(jsonObject: JSONObject): Boolean {
        val versionKey = Preferences.Module.SP_VERSION.name

        if (!jsonObject.has(versionKey)) {
            return true
        }

        return try {
            when (val value = jsonObject.get(versionKey)) {
                is Int -> true
                is String -> {
                    val version = value.removePrefix("#i#").toInt()
                    version <= Preferences.VERSION
                }
                else -> false
            }
        } catch (_: Exception) {
            false
        }
    }
}
