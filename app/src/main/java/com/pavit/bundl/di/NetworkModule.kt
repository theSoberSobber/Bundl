package com.pavit.bundl.di

import android.content.Context
import android.content.SharedPreferences
import com.pavit.bundl.data.local.TokenManager
import com.pavit.bundl.data.remote.api.CreditsService
import com.pavit.bundl.data.remote.api.AuthApiService
import com.pavit.bundl.data.remote.api.OrderApiService
import com.pavit.bundl.utils.network.PlainTextConverterFactory
import com.pavit.bundl.utils.network.AuthAuthenticator
import com.pavit.bundl.utils.network.AuthInterceptor
import com.pavit.bundl.utils.network.TimingInterceptor
import com.pavit.bundl.domain.payment.PaymentService
import com.pavit.bundl.data.remote.dto.OrderStatusResponse
import com.pavit.bundl.presentation.orders.OrderStatusResponseDeserializer
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthOkHttpClient

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RegularOkHttpClient

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthenticatorOkHttpClient

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RegularRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthenticatorRetrofit

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    private const val BASE_URL = "https://backend-bundl.1110777.xyz"
    // private const val BASE_URL = "http://192.168.53.152:3002"

    @Provides
    @Singleton
    fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor { message ->
            // Add a custom prefix to make logs easier to find
            android.util.Log.d("BundlAPI", "╔══════════════════════════════════════")
            android.util.Log.d("BundlAPI", "║ $message")
            android.util.Log.d("BundlAPI", "╚══════════════════════════════════════")
        }.apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }
    
    @Provides
    @Singleton
    @AuthOkHttpClient
    fun provideAuthOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        timingInterceptor: TimingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(timingInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @AuthenticatorOkHttpClient
    fun provideAuthenticatorOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        timingInterceptor: TimingInterceptor,
        authInterceptor: AuthInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(timingInterceptor)
            .addInterceptor(loggingInterceptor)
            .addInterceptor(authInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    
    @Provides
    @Singleton
    @RegularOkHttpClient
    fun provideRegularOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        timingInterceptor: TimingInterceptor,
        authInterceptor: AuthInterceptor,
        authAuthenticator: AuthAuthenticator
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(timingInterceptor)
            .addInterceptor(loggingInterceptor)
            .addInterceptor(authInterceptor)
            .authenticator(authAuthenticator)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    
    @Provides
    @Singleton
    @AuthRetrofit
    fun provideAuthRetrofit(@AuthOkHttpClient okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    @AuthenticatorRetrofit
    fun provideAuthenticatorRetrofit(@AuthenticatorOkHttpClient okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    @Provides
    @Singleton
    @RegularRetrofit
    fun provideRegularRetrofit(@RegularOkHttpClient okHttpClient: OkHttpClient): Retrofit {
        val gson = GsonBuilder()
            .registerTypeAdapter(OrderStatusResponse::class.java, OrderStatusResponseDeserializer())
            .create()
        
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(PlainTextConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }
    
    @Provides
    @Singleton
    fun provideAuthApiService(@AuthenticatorRetrofit retrofit: Retrofit): AuthApiService {
        return retrofit.create(AuthApiService::class.java)
    }
    
    @Provides
    @Singleton
    fun provideCreditsService(@RegularRetrofit retrofit: Retrofit): CreditsService {
        return retrofit.create(CreditsService::class.java)
    }
    
    @Provides
    @Singleton
    fun provideOrderApiService(@RegularRetrofit retrofit: Retrofit): OrderApiService {
        return retrofit.create(OrderApiService::class.java)
    }

    @Provides
    @Singleton
    fun providePaymentService(creditsService: CreditsService): PaymentService {
        return PaymentService(creditsService)
    }
} 