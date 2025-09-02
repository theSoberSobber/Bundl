package com.pavit.bundl.presentation.navigation

import androidx.lifecycle.ViewModel
import com.pavit.bundl.domain.repository.NavigationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class NavigationViewModel @Inject constructor(
    val navigationRepository: NavigationRepository
) : ViewModel()
