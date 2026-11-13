package com.example.data.remote

import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object SupabaseConfig {
  val url: String
    get() = BuildConfig.SUPABASE_URL.trim().trimEnd('/')

  val anonKey: String
    get() = BuildConfig.SUPABASE_ANON_KEY.trim()

  val isConfigured: Boolean
    get() = url.startsWith("https://") &&
      url != "SUPABASE_URL_DEFAULT_VALUE" &&
      anonKey.isNotBlank() &&
      anonKey != "SUPABASE_ANON_KEY_DEFAULT_VALUE"

  fun createApi(
    baseUrl: String = url,
    apiKey: String = anonKey,
  ): SupabaseApi {
    val logging = HttpLoggingInterceptor().apply {
      level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
    }
    val client = OkHttpClient.Builder()
      .connectTimeout(15, TimeUnit.SECONDS)
      .readTimeout(25, TimeUnit.SECONDS)
      .writeTimeout(25, TimeUnit.SECONDS)
      .addInterceptor { chain ->
        val request = chain.request().newBuilder()
          .header("apikey", apiKey)
          .header("Authorization", "Bearer $apiKey")
          .header("Accept", "application/json")
          .build()
        chain.proceed(request)
      }
      .addInterceptor(logging)
      .build()

    val moshi = Moshi.Builder()
      .addLast(KotlinJsonAdapterFactory())
      .build()

    val normalized = baseUrl.trimEnd('/') + "/"
    return Retrofit.Builder()
      .baseUrl(normalized)
      .client(client)
      .addConverterFactory(MoshiConverterFactory.create(moshi))
      .build()
      .create(SupabaseApi::class.java)
  }
}
