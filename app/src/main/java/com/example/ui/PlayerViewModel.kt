package com.example.ui

import android.app.Application

/**
 * PlayerViewModel inheriting all functionality from MainViewModel
 * for backward compatibility and clean architecture.
 */
class PlayerViewModel(application: Application) : MainViewModel(application)
