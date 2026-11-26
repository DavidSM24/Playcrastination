package com.playcrastination.data.steam

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.playcrastination.domain.model.SteamFailure
import com.playcrastination.domain.model.SteamProblem
import com.playcrastination.domain.model.SteamSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class SteamCredentials(val steamId: String, val apiKey: String)

interface SteamConfiguration {
    val settings: Flow<SteamSettings>
    suspend fun credentials(): SteamCredentials?
    suspend fun save(credentials: SteamCredentials)
    suspend fun clear()
    suspend fun setAutomatic(enabled: Boolean)
    suspend fun setInterval(minutes: Int)
}

private val Context.steamPreferences by preferencesDataStore(name = "steam_preferences")

@Singleton
class AndroidSteamConfiguration @Inject constructor(@ApplicationContext context: Context) : SteamConfiguration {
    private val store = context.steamPreferences
    private val keyAccess = Mutex()

    override val settings = store.data.catch { failure ->
        if (failure is IOException) emit(emptyPreferences()) else throw failure
    }.map { values ->
        SteamSettings(
            steamId = values[Id],
            hasKey = values[EncryptedKey] != null,
            automatic = values[Automatic] ?: true,
            intervalMinutes = (values[Interval] ?: 30).coerceIn(15, 1440)
        )
    }

    override suspend fun credentials(): SteamCredentials? = secureOperation {
        val values = store.data.first()
        val id = values[Id] ?: return@secureOperation null
        val encrypted = values[EncryptedKey] ?: return@secureOperation null
        val bytes = Base64.decode(encrypted, Base64.NO_WRAP)
        require(bytes.size > IvSize && bytes[0] == FormatVersion)
        val cipher = Cipher.getInstance(Transformation)
        cipher.init(Cipher.DECRYPT_MODE, key(create = false), GCMParameterSpec(128, bytes, 1, IvSize))
        cipher.updateAAD(id.toByteArray(Charsets.UTF_8))
        val secret = cipher.doFinal(bytes, 1 + IvSize, bytes.size - 1 - IvSize)
        try {
            SteamCredentials(id, secret.toString(Charsets.UTF_8))
        } finally {
            secret.fill(0)
        }
    }

    override suspend fun save(credentials: SteamCredentials): Unit = secureOperation {
        val cipher = Cipher.getInstance(Transformation)
        cipher.init(Cipher.ENCRYPT_MODE, key(create = true))
        cipher.updateAAD(credentials.steamId.toByteArray(Charsets.UTF_8))
        val secret = credentials.apiKey.toByteArray(Charsets.UTF_8)
        val encrypted = try {
            cipher.doFinal(secret)
        } finally {
            secret.fill(0)
        }
        require(cipher.iv.size == IvSize)
        val encoded = Base64.encodeToString(byteArrayOf(FormatVersion) + cipher.iv + encrypted, Base64.NO_WRAP)
        store.edit { values ->
            values[Id] = credentials.steamId
            values[EncryptedKey] = encoded
        }
    }

    override suspend fun clear(): Unit = secureOperation {
        store.edit {
            it.remove(Id)
            it.remove(EncryptedKey)
        }
    }

    override suspend fun setAutomatic(enabled: Boolean): Unit = secureOperation {
        store.edit { it[Automatic] = enabled }
    }

    override suspend fun setInterval(minutes: Int): Unit = secureOperation {
        store.edit { it[Interval] = minutes }
    }

    private suspend fun <T> secureOperation(block: suspend () -> T): T =
        keyAccess.withLock {
            withContext(Dispatchers.IO) {
                try {
                    block()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    throw SteamFailure(SteamProblem.STORAGE)
                }
            }
        }

    private fun key(create: Boolean): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(KeyAlias, null) as? SecretKey)?.let { return it }
        check(create)
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(KeyAlias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setKeySize(256)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build())
        }.generateKey()
    }

    private companion object {
        const val KeyAlias = "playcrastination.steam.configuration"
        const val Transformation = "AES/GCM/NoPadding"
        const val IvSize = 12
        const val FormatVersion: Byte = 1
        val Id = stringPreferencesKey("steam_id")
        val EncryptedKey = stringPreferencesKey("encrypted_steam_key")
        val Automatic = booleanPreferencesKey("auto_sync")
        val Interval = intPreferencesKey("interval_minutes")
    }
}
