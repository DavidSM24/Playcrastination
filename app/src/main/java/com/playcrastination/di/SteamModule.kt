package com.playcrastination.di

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.playcrastination.data.repository.LocalSteamRepository
import com.playcrastination.data.repository.SteamNetwork
import com.playcrastination.data.steam.AndroidSteamConfiguration
import com.playcrastination.data.steam.SteamApi
import com.playcrastination.data.steam.SteamConfiguration
import com.playcrastination.data.steam.SteamGateway
import com.playcrastination.data.steam.SteamMetadataSource
import com.playcrastination.data.steam.SteamRemoteDataSource
import com.playcrastination.data.steam.SteamStoreApi
import com.playcrastination.data.steam.SteamStoreMetadata
import com.playcrastination.domain.repository.SteamRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

@Module
@InstallIn(SingletonComponent::class)
internal abstract class SteamModule {
    @Binds abstract fun steamRepository(repository: LocalSteamRepository): SteamRepository
    @Binds abstract fun configuration(configuration: AndroidSteamConfiguration): SteamConfiguration
    @Binds abstract fun gateway(gateway: SteamRemoteDataSource): SteamGateway
    @Binds abstract fun metadata(metadata: SteamStoreMetadata): SteamMetadataSource

    companion object {
        private val SteamJson = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }

        @Provides
        @Singleton
        fun steamHttpClient(): OkHttpClient = OkHttpClient.Builder()
            .followRedirects(false)
            .followSslRedirects(false)
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .callTimeout(18, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
            .build()

        @Provides
        fun steamApi(http: OkHttpClient): SteamApi =
            retrofit("https://api.steampowered.com/", http).create(SteamApi::class.java)

        @Provides
        fun storeApi(http: OkHttpClient): SteamStoreApi =
            retrofit("https://store.steampowered.com/", http).create(SteamStoreApi::class.java)

        @Provides
        fun steamNetwork(@ApplicationContext context: Context): SteamNetwork {
            val manager = context.getSystemService(ConnectivityManager::class.java)
            return SteamNetwork {
                manager?.getNetworkCapabilities(manager.activeNetwork)
                    ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
            }
        }

        private fun retrofit(url: String, http: OkHttpClient): Retrofit = Retrofit.Builder()
            .client(http)
            .baseUrl(url)
            .addConverterFactory(SteamJson.asConverterFactory("application/json".toMediaType()))
            .build()
    }
}
