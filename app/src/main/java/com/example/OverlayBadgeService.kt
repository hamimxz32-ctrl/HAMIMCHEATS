package com.example

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.app.NotificationCompat
import kotlin.math.abs

class OverlayBadgeService : Service() {
  private var windowManager: WindowManager? = null
  private var badgeView: View? = null

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onCreate() {
    super.onCreate()
    isRunning = true
    startAsForeground()
    if (Settings.canDrawOverlays(this)) {
      attachFloatingBadge()
    } else {
      stopSelf()
    }
  }

  override fun onDestroy() {
    isRunning = false
    badgeView?.let { view ->
      runCatching { windowManager?.removeView(view) }
    }
    badgeView = null
    super.onDestroy()
  }

  private fun startAsForeground() {
    val channelId = "codigos_overlay_badge"
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val manager = getSystemService(NotificationManager::class.java)
      val channel =
        NotificationChannel(
          channelId,
          getString(R.string.app_name),
          NotificationManager.IMPORTANCE_LOW,
        )
      manager?.createNotificationChannel(channel)
    }

    val openIntent =
      PendingIntent.getActivity(
        this,
        0,
        Intent(this, MainActivity::class.java).apply {
          flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        },
        PendingIntent.FLAG_IMMUTABLE,
      )

    val notification: Notification =
      NotificationCompat.Builder(this, channelId)
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentTitle(getString(R.string.app_name))
        .setContentText(getString(R.string.overlay_badge_toggle))
        .setContentIntent(openIntent)
        .setOngoing(true)
        .build()

    startForeground(101, notification)
  }

  private fun attachFloatingBadge() {
    val wm = getSystemService(WINDOW_SERVICE) as WindowManager
    windowManager = wm

    val density = resources.displayMetrics.density
    val sizePx = (58 * density).toInt()
    val paddingPx = (3 * density).toInt()

    val container =
      FrameLayout(this).apply {
        background =
          GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor("#FFFFF8FB"))
            setStroke((2.5f * density).toInt(), Color.parseColor("#FFFF4D8D"))
          }
        setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
        elevation = 10f * density
      }

    val logoView =
      ImageView(this).apply {
        setImageResource(R.drawable.ic_codigos_logo)
        scaleType = ImageView.ScaleType.CENTER_CROP
        clipToOutline = true
        outlineProvider =
          object : android.view.ViewOutlineProvider() {
            override fun getOutline(view: View, outline: android.graphics.Outline) {
              outline.setOval(0, 0, view.width, view.height)
            }
          }
      }

    container.addView(
      logoView,
      FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT,
        FrameLayout.LayoutParams.MATCH_PARENT,
      ),
    )

    val layoutType =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
      } else {
        @Suppress("DEPRECATION")
        WindowManager.LayoutParams.TYPE_PHONE
      }

    val params =
      WindowManager.LayoutParams(
        sizePx,
        sizePx,
        layoutType,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        PixelFormat.TRANSLUCENT,
      ).apply {
        gravity = Gravity.TOP or Gravity.START
        x = (20 * density).toInt()
        y = (180 * density).toInt()
      }

    var startX = 0
    var startY = 0
    var touchX = 0f
    var touchY = 0f
    var moved = false

    container.setOnTouchListener { v, event ->
      when (event.action) {
        MotionEvent.ACTION_DOWN -> {
          startX = params.x
          startY = params.y
          touchX = event.rawX
          touchY = event.rawY
          moved = false
          true
        }
        MotionEvent.ACTION_MOVE -> {
          val dx = (event.rawX - touchX).toInt()
          val dy = (event.rawY - touchY).toInt()
          if (abs(dx) > 6 || abs(dy) > 6) moved = true
          params.x = startX + dx
          params.y = startY + dy
          wm.updateViewLayout(v, params)
          true
        }
        MotionEvent.ACTION_UP -> {
          if (!moved) {
            v.performClick()
            startActivity(
              Intent(this@OverlayBadgeService, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
              }
            )
          }
          true
        }
        else -> false
      }
    }

    wm.addView(container, params)
    badgeView = container
  }

  companion object {
    @Volatile
    var isRunning: Boolean = false
      private set
  }
}
