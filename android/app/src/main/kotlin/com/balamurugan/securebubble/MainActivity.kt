package com.balamurugan.securebubble

import android.content.Intent
import android.os.Build
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {

    companion object {
        const val CHANNEL = "nukezero/service"
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        MethodChannel(
            flutterEngine.dartExecutor.binaryMessenger,
            CHANNEL
        ).setMethodCallHandler { call, result ->

            when (call.method) {

                "startBubble" -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !android.provider.Settings.canDrawOverlays(this)) {
                        val overlayIntent = Intent(
                            android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            android.net.Uri.parse("package:$packageName")
                        ).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        startActivity(overlayIntent)
                        result.error(
                            "PERMISSION_REQUIRED",
                            "Display over other apps permission required. Please enable it in Settings.",
                            null
                        )
                        return@setMethodCallHandler
                    }

                    try {
                        val intent = Intent(
                            this,
                            BubbleService::class.java
                        )

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            startForegroundService(intent)
                        } else {
                            startService(intent)
                        }

                        result.success(true)
                    } catch (e: Exception) {
                        result.error("SERVICE_START_FAILED", e.localizedMessage, null)
                    }
                }

                "stopBubble" -> {
                    try {
                        val intent = Intent(
                            this,
                            BubbleService::class.java
                        )
                        stopService(intent)
                        result.success(true)
                    } catch (e: Exception) {
                        result.error("SERVICE_STOP_FAILED", e.localizedMessage, null)
                    }
                }

                "requestOverlayPermission" -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        val overlayIntent = Intent(
                            android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            android.net.Uri.parse("package:$packageName")
                        ).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        startActivity(overlayIntent)
                    }
                    result.success(true)
                }

                "openAccessibilitySettings" -> {
                    val accIntent = Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(accIntent)
                    result.success(true)
                }

                "scanHyperlink" -> {
                    val service = SecureBubbleAccessibilityService.instance
                    if (service != null) {
                        val hyperlinkJson = service.extractHyperlinkInfo()
                        result.success(hyperlinkJson)
                    } else {
                        val fallbackJson = """
                            {
                                "visibleText": "No visible text",
                                "actualUrl": "Not available",
                                "sourceApp": "Accessibility Service Disabled",
                                "domain": "Not available",
                                "finalUrl": "Not available",
                                "redirects": [],
                                "domainMatch": false,
                                "detectionType": "VISIBLE_URL_ONLY",
                                "riskLevel": "SUSPICIOUS",
                                "reason": "This application does not expose the hyperlink destination through its UI."
                            }
                        """.trimIndent()
                        result.success(fallbackJson)
                    }
                }

                else -> {
                    result.notImplemented()
                }
            }
        }
    }
}