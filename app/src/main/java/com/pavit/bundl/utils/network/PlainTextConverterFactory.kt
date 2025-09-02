package com.pavit.bundl.utils.network

import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.Type

class PlainTextConverterFactory : Converter.Factory() {
    override fun responseBodyConverter(
        type: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit
    ): Converter<ResponseBody, *>? {
        if (type == String::class.java) {
            return Converter<ResponseBody, String> { value -> value.string() }
        }
        return null
    }

    companion object {
        fun create(): PlainTextConverterFactory {
            return PlainTextConverterFactory()
        }
    }
} 