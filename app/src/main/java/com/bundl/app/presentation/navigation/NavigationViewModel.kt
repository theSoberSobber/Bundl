package com.bundl.app.presentation.navigation

import androidx.lifecycle.ViewModel
import com.bundl.app.domain.repository.NavigationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class NavigationViewModel @Inject constructor(
    val navigationRepository: NavigationRepository
) : ViewModel()
