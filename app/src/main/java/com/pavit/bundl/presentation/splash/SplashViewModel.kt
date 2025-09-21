package com.pavit.bundl.presentation.splash

import androidx.lifecycle.ViewModel
import com.pavit.bundl.data.utils.LocationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    val locationManager: LocationManager
) : ViewModel()