package com.example

import android.app.Application
import com.example.data.AppRepository

class App : Application() {
  lateinit var repository: AppRepository
    private set

  override fun onCreate() {
    super.onCreate()
    repository = AppRepository(applicationContext)
  }
}
