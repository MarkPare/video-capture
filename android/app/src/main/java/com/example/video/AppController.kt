package com.example.video

import android.app.Application
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class AppController : Application() {
    val TAG = "AppController"
    val executorService: ExecutorService = Executors.newFixedThreadPool(4)
}
