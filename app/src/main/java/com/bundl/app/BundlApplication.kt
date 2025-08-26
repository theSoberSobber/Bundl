package com.bundl.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class for Bundl app.
 * Uses Hilt for dependency injection.
 */
@HiltAndroidApp
class BundlApplication : Application() 