package com.thevakhovske.cunnyplayground

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import android.widget.Toast
import android.graphics.Bitmap
import android.widget.RemoteViews
import org.json.JSONObject
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toBitmap
import io.github.d4viddf.hyperisland_kit.HyperAction
import io.github.d4viddf.hyperisland_kit.HyperPicture

class PlaygroundService : Service() {

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_CANCEL = "ACTION_CANCEL"
        const val ACTION_STOP = "ACTION_STOP"
        const val CHANNEL_ID = "live_updates_channel"
        const val HYPER_CHANNEL_ID = "hyperslop_channel"
        const val NOTIFICATION_ID = 1001
    }

    private val activeIds = mutableSetOf<Int>()
    private var isForegroundActive = false
    private lateinit var notificationManager: NotificationManager

    private val isMiuiGlobalBuild: Boolean by lazy {
        try {
            val systemProperties = Class.forName("android.os.SystemProperties")
            val get = systemProperties.getMethod("get", String::class.java)
            val region = get.invoke(null, "ro.miui.region") as String
            region.isNotBlank() && region != "CN"
        } catch (e: Exception) {
            false
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isForegroundActive) {
            createNotificationChannel(CHANNEL_ID)
            val anchorNotification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.service_anchor_title))
                .setContentText(getString(R.string.service_anchor_text))
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setSilent(true)
                .setOngoing(true)
                .build()

            if (Build.VERSION.SDK_INT >= 29) {
                try {
                    if (Build.VERSION.SDK_INT >= 34) {
                        startForeground(9999, anchorNotification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
                    } else {
                        startForeground(9999, anchorNotification)
                    }
                } catch (e: Exception) {
                    startForeground(9999, anchorNotification)
                }
            } else {
                startForeground(9999, anchorNotification)
            }
            isForegroundActive = true
        }

        when (intent?.action) {
            ACTION_START -> startPromotedNotification(intent)
            ACTION_CANCEL -> cancelNotification(intent)
            ACTION_STOP -> {
                activeIds.forEach { notificationManager.cancel(it) }
                activeIds.clear()
                stopForeground(true)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun cancelNotification(intent: Intent) {
        val id = intent.getIntExtra("id", -1)
        if (id != -1) {
            // Signal Vivo framework to gracefully unmount the island pill 
            // before destroying the underlying notification object
            try {
                val endBundle = android.os.Bundle()
                endBundle.putInt("notification.superx.operation", 2)
                val builder = NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher_round)
                    .addExtras(endBundle)
                notificationManager.notify(id, builder.build())
            } catch (e: Exception) {}

            notificationManager.cancel(id)
            activeIds.remove(id)
        }
    }

    private fun startPromotedNotification(intent: Intent) {
        val title = intent.getStringExtra("title") ?: getString(R.string.default_notif_title)
        val text = intent.getStringExtra("text") ?: getString(R.string.default_notif_text)
        val subtext = intent.getStringExtra("subtext")
        val notificationId = intent.getIntExtra("id", NOTIFICATION_ID)
        val iconRes = intent.getIntExtra("icon_res", R.mipmap.ic_launcher_round)
        val iconObj = if (Build.VERSION.SDK_INT >= 23) {
            intent.getParcelableExtra<android.graphics.drawable.Icon>("small_icon_obj")
        } else null 
        val sourceApp = intent.getStringExtra("source_app")
        val isPromoted = intent.getBooleanExtra("is_promoted", true)
        val statusChipText = intent.getStringExtra("status_chip_text")
        val showProgress = intent.getBooleanExtra("show_progress", true)
        val timestamp = intent.getLongExtra("when", System.currentTimeMillis())
        
        val castMode = intent.getStringExtra("cast_mode") ?: "live_updates"
        val targetChannel = if (castMode == "hyperisland") HYPER_CHANNEL_ID else CHANNEL_ID

        val largeIconObj = if (Build.VERSION.SDK_INT >= 23) {
            intent.getParcelableExtra<android.graphics.drawable.Icon>("large_icon_obj")
        } else null
        val largeIconBitmap = intent.getParcelableExtra<android.graphics.Bitmap>("large_icon_bitmap")
        val sourceRv = intent.getParcelableExtra<android.widget.RemoteViews>("miui_rv")
        val segmentsCount = intent.getIntExtra("progress_segments", 0)

        activeIds.add(notificationId)
        createNotificationChannel(targetChannel)

        val builder = NotificationCompat.Builder(this, targetChannel)
        
        if (largeIconObj != null && Build.VERSION.SDK_INT >= 23) {
            builder.setLargeIcon(largeIconObj)
        } else if (largeIconBitmap != null) {
            builder.setLargeIcon(largeIconBitmap)
        }
        
        if (iconObj != null && Build.VERSION.SDK_INT >= 23) {
            builder.setSmallIcon(IconCompat.createFromIcon(this, iconObj))
        } else {
            builder.setSmallIcon(iconRes)
        }

        builder.setContentTitle(title)
            .setContentText(text)
            .setOngoing(intent.getBooleanExtra("is_ongoing", false))
            .setOnlyAlertOnce(true)
            .setShowWhen(true)
            .setWhen(timestamp)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSilent(true)

        if (!sourceApp.isNullOrEmpty()) {
            builder.setSubText(sourceApp)
        }

        // Apply Actions
        val actions = intent.getParcelableArrayListExtra<Notification.Action>("actions")
        actions?.forEach { action ->
            val icon = if (Build.VERSION.SDK_INT >= 23) {
                action.getIcon()?.let { IconCompat.createFromIcon(this, it) }
            } else null
            
            val builderAction = NotificationCompat.Action.Builder(
                icon,
                action.title,
                action.actionIntent
            ).build()
            builder.addAction(builderAction)
        }

        if (castMode == "live_updates") {
            // Apply Progress
            val progress = intent.getIntExtra("progress", 0)
            val progressMax = intent.getIntExtra("progress_max", 0)
            val isIndeterminate = intent.getBooleanExtra("progress_indeterminate", false)
            
            if (showProgress) {
                builder.setProgress(progressMax, progress, isIndeterminate)
            }
    
            if (isPromoted) {
                try {
                    val method = builder.javaClass.getMethod("setRequestPromotedOngoing", Boolean::class.java)
                    method.invoke(builder, true)
                } catch (e: Exception) {
                    builder.extras.putBoolean("android.app.extra.PROMOTED_ONGOING", true)
                }
            }
    
            if (!statusChipText.isNullOrEmpty()) {
                try {
                    val method = builder.javaClass.getMethod("setShortCriticalText", String::class.java)
                    method.invoke(builder, statusChipText)
                } catch (e: Exception) {
                    // Fail silently if API not available
                }
            }
    
            // Progress Style (Status Chip)
            if (showProgress && isPromoted && progressMax > 0) {
                try {
                    val progressStyle = NotificationCompat.ProgressStyle()
                    val totalDuration = 100000 // arbitrary base for percentage (Int)
                    val currentProgress = (progress.toDouble() / progressMax * totalDuration).toInt()
                    
                    // Apply Monet dynamic color accent to progress bar and icons
                    try {
                        val dynamicContext = com.google.android.material.color.DynamicColors.wrapContextIfAvailable(this)
                        val primaryColor = androidx.core.content.ContextCompat.getColor(this, R.color.purple_500)
                        progressStyle.addProgressSegment(
                        NotificationCompat.ProgressStyle.Segment(totalDuration).setColor(primaryColor)
                    )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    progressStyle.setProgress(currentProgress)
                    builder.setStyle(progressStyle)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else if (showProgress) {
                // Standard progress only
                try {
                    val progressStyle = NotificationCompat.ProgressStyle()
                    builder.setStyle(progressStyle)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        if (castMode == "hyperisland") {
            try {
                // Initialize builder early to supply resources unconditionally
                val hyperBuilder = io.github.d4viddf.hyperisland_kit.HyperIslandNotification.Builder(
                    this,
                    "live_updates_recaster",
                    "Incoming Notification"
                )
                
                val hPic = if (iconObj != null && Build.VERSION.SDK_INT >= 23) {
                    io.github.d4viddf.hyperisland_kit.HyperPicture("default_icon", iconObj)
                } else {
                    io.github.d4viddf.hyperisland_kit.HyperPicture("default_icon", this, iconRes)
                }
                hyperBuilder.addPicture(hPic)
                
                // Register LargeIcon if available for heads-up/expanded views
                if (largeIconObj != null && Build.VERSION.SDK_INT >= 23) {
                    hyperBuilder.addPicture(io.github.d4viddf.hyperisland_kit.HyperPicture("big_icon", largeIconObj))
                } else if (largeIconBitmap != null) {
                    hyperBuilder.addPicture(io.github.d4viddf.hyperisland_kit.HyperPicture("big_icon", largeIconBitmap))
                }

                // Inject dummy resources so user's manual notif.json testing doesn't break HyperOS rendering
                hyperBuilder.addPicture(io.github.d4viddf.hyperisland_kit.HyperPicture("file_preview", this, iconRes))
                hyperBuilder.addPicture(io.github.d4viddf.hyperisland_kit.HyperPicture("upload_status", this, iconRes))

                val rawJson = intent.getStringExtra("raw_hyper_json")
                if (!rawJson.isNullOrBlank()) {
                    builder.extras.putString("miui.focus.param", rawJson)
                    builder.extras.putAll(hyperBuilder.buildResourceBundle())
                    
                    // We still need to set some defaults for the notification shade part
                    builder.setSmallIcon(iconRes)
                    builder.setContentTitle(title)
                    builder.setContentText(text)
                } else if (io.github.d4viddf.hyperisland_kit.HyperIslandNotification.isSupported(this)) {

                    hyperBuilder.setBaseInfo(
                        title = title,
                        content = text,
                        pictureKey = null
                    )
                    val islandText = statusChipText?.takeIf { it.isNotBlank() } ?: title
                    
                    val hyperLeftExtra = intent.getStringExtra("hyper_left_text")
                    val hyperMainExtra = intent.getStringExtra("hyper_main_text")

                    // Structure the Big Island Area to populate both pill sides properly
                    val picInfo = io.github.d4viddf.hyperisland_kit.models.PicInfo(1, "default_icon", false, false, 0, null, null, null)
                    
                    val leftTextInfoObj = io.github.d4viddf.hyperisland_kit.models.TextInfo(
                        title = hyperLeftExtra?.takeIf { it.isNotBlank() } ?: title,
                        content = null,
                        showHighlightColor = false,
                        narrowFont = null
                    )
                    
                    val imageTextInfoLeft = io.github.d4viddf.hyperisland_kit.models.ImageTextInfoLeft(
                        1, picInfo, leftTextInfoObj, null
                    )
                    
                    val rootTextInfo = io.github.d4viddf.hyperisland_kit.models.TextInfo(
                        title = hyperMainExtra?.takeIf { it.isNotBlank() } ?: text, 
                        content = null, 
                        showHighlightColor = false, 
                        narrowFont = null
                    )
                    
                    // Pass rootTextInfo as the 3rd parameter mapping natively to "textInfo" block
                    hyperBuilder.setBigIslandInfo(imageTextInfoLeft, null, rootTextInfo, null, null, null)
                    
                    // Use setSmallIslandIcon to cleanly map a solo picInfo block matching tethering smallIslandArea
                    hyperBuilder.setSmallIslandIcon("default_icon")
                    
                    // The icon appearing in param_v2
                    hyperBuilder.setPicInfo(2, "default_icon")
                    
                    // Auto-popup priority
                    hyperBuilder.setIslandConfig(priority = 2)

                    //if (sourceRv != null) {
                        //builder.extras.putParcelable("miui.focus.rv", sourceRv)
                    //}
                    
                    // Add Interactive Actions (Buttons)
                    val originalActions = if (Build.VERSION.SDK_INT >= 34) {
                        intent.getParcelableArrayListExtra("actions", Notification.Action::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableArrayListExtra<Notification.Action>("actions")
                    }

                    originalActions?.forEachIndexed { index, action ->
                        val actionIntent = action.actionIntent
                        if (actionIntent != null) {
                            val type = when {
                                actionIntent.isActivity -> 1
                                actionIntent.isBroadcast -> 2
                                actionIntent.isForegroundService || actionIntent.isService -> 3
                                else -> 2 // Default to broadcast
                            }

                            val actionIcon = action.getIcon()
                            val actionBitmap: Bitmap? = if (actionIcon != null) {
                                try {
                                    actionIcon.loadDrawable(this)?.toBitmap(128, 128)
                                } catch (e: Exception) {
                                    null
                                }
                            } else null

                            val hAction = if (actionBitmap != null) {
                                HyperAction(
                                    key = "action_$index",
                                    title = action.title?.toString() ?: "Action",
                                    bitmap = actionBitmap,
                                    pendingIntent = actionIntent,
                                    actionIntentType = type
                                )
                            } else {
                                HyperAction(
                                    key = "action_$index",
                                    title = action.title?.toString() ?: "Action",
                                    pendingIntent = actionIntent,
                                    actionIntentType = type
                                )
                            }
                            hyperBuilder.addAction(hAction)
                        }
                    }
                    
                    val jsonPayloadRaw = hyperBuilder.buildJsonParam()
                    val jsonObj = org.json.JSONObject(jsonPayloadRaw)
                    val paramV2 = jsonObj.optJSONObject("param_v2")
                    val useLargeIcon = (largeIconObj != null || largeIconBitmap != null)
                    if (paramV2 != null) {
                        if (useLargeIcon) {
                            // we love json injections
                            val iconTextInfo = org.json.JSONObject().apply {
                                val animIconInfo = org.json.JSONObject().apply {
                                    put("type", 0)
                                    put("src", "miui.focus.pic_big_icon")
                                    put("loop", true)
                                    put("autoplay", true)
                                }
                                put("animIconInfo", animIconInfo)
                                put("title", title)
                                put("content", text)
                            }
                            paramV2.put("iconTextInfo", iconTextInfo)
                            paramV2.remove("picInfo")
                        }
                        
                        paramV2.put("enableFloat", false)
                        paramV2.put("islandFirstFloat", false)
                        
                        // Inject hintInfo if subtext exists
                        if (!subtext.isNullOrBlank()) {
                            paramV2.put("hintInfo", org.json.JSONObject().apply {
                                put("type", 1)
                                put("title", subtext)
                            })
                        }
                        
                        // Implement Progress Bar Support (Manual Injection)
                        val progress = intent.getIntExtra("progress", 0)
                        val progressMax = intent.getIntExtra("progress_max", 0)
                        if (showProgress && progressMax > 0 && progress < progressMax) {
                            val progressPercent = (progress * 100) / progressMax
                            val hasSegments = segmentsCount > 0
                            
                            // 1. Root Progress
                            if (hasSegments) {
                                val multiProgressInfo = org.json.JSONObject().apply {
                                    put("progress", progressPercent)
                                    put("points", segmentsCount)
                                    put("color", "#34C759")
                                }
                                paramV2.put("multiProgressInfo", multiProgressInfo)
                            } else {
                                val rootProgressInfo = org.json.JSONObject().apply {
                                    put("progress", progressPercent)
                                    put("colorProgress", "#34C759")
                                }
                                paramV2.put("progressInfo", rootProgressInfo)
                            }
                            
                            // 2. Small Island Progress (requires combinePicInfo wrapper)
                            val paramIsland = paramV2.optJSONObject("param_island")
                            val smallArea = paramIsland?.optJSONObject("smallIslandArea")
                            val picInfo = smallArea?.optJSONObject("picInfo")
                            if (smallArea != null && picInfo != null) {
                                val combinePicInfo = org.json.JSONObject().apply {
                                    put("picInfo", picInfo)
                                    val progKey = "progressInfo"
                                    put(progKey, org.json.JSONObject().apply {
                                        put("progress", progressPercent)
                                        put("colorReach", "#34C759")
                                    })
                                }
                                smallArea.remove("picInfo")
                                smallArea.put("combinePicInfo", combinePicInfo)
                            }
                            
                            // 3. Big Island Progress (requires progressTextInfo block)
                            val bigArea = paramIsland?.optJSONObject("bigIslandArea")
                            if (bigArea != null) {
                                val progressTextInfo = org.json.JSONObject().apply {
                                    val progKey = "progressInfo"
                                    put(progKey, org.json.JSONObject().apply {
                                        put("progress", progressPercent)
                                        put("colorReach", "#34C759")
                                    })
                                }
                                bigArea.put("progressTextInfo", progressTextInfo)
                            }
                        }
                    }
                    val jsonPayload = jsonObj.toString()
                    val resBundle = hyperBuilder.buildResourceBundle()
                    
                    builder.extras.putString("miui.focus.param", jsonPayload)
                    builder.extras.putAll(resBundle)

                    if (isMiuiGlobalBuild) {
                        // inject original remoteview (from preliminary impl)
                        val sourceRv = intent.getParcelableExtra<RemoteViews>("miui_rv")
                        if (sourceRv != null) {
                            val wrappedRv = RemoteViews(packageName, R.layout.focus_rv_wrapper)
                            wrappedRv.removeAllViews(R.id.rv_wrapper_container)
                            wrappedRv.addView(R.id.rv_wrapper_container, sourceRv)
                            builder.extras.putParcelable("miui.focus.rv", wrappedRv)
                        }

                        // miui.focus.pic_ticker needs to exists according to notificationfocusmanager
                        val tPic = if (iconObj != null && Build.VERSION.SDK_INT >= 23) {
                            io.github.d4viddf.hyperisland_kit.HyperPicture("ticker", iconObj)
                        } else {
                            io.github.d4viddf.hyperisland_kit.HyperPicture("ticker", this, iconRes)
                        }
                        hyperBuilder.addPicture(tPic)
                        
                        builder.extras.putAll(hyperBuilder.buildResourceBundle())

                        // miui.focus.param.custom
                        val customJson = JSONObject().apply {
                            put("ticker", title)
                            put("tickerPic", "miui.focus.pic_ticker")
                            put("enableFloat", false)
                            put("updatable", true)
                            put("isShowNotification", true)
                            put("islandFirstFloat", false)
                            put("timeout", 10000)

                            val paramIsland = JSONObject().apply {
                                put("islandProperty", 1)
                                put("islandPriority", 2)
                                put("islandOrder", false)
                                put("dismissIsland", false)
                                put("maxSize", false)
                                put("needCloseAnimation", true)

                                val bigIslandArea = JSONObject().apply {
                                    val imageTextInfoLeft = JSONObject().apply {
                                        put("type", 1)
                                        put("picInfo", JSONObject().apply {
                                            put("type", 1)
                                            put("pic", "miui.focus.pic_default_icon")
                                            put("loop", false)
                                            put("autoplay", false)
                                            put("number", 0)
                                        })
                                        put("textInfo", JSONObject().apply {
                                            put("title", title)
                                            put("showHighlightColor", false)
                                        })
                                    }
                                    put("imageTextInfoLeft", imageTextInfoLeft)
                                    
                                    put("textInfo", JSONObject().apply {
                                        put("title", text)
                                        put("showHighlightColor", false)
                                    })
                                }
                                put("bigIslandArea", bigIslandArea)

                                val smallIslandArea = JSONObject().apply {
                                    put("picInfo", JSONObject().apply {
                                        put("type", 1)
                                        put("pic", "miui.focus.pic_default_icon")
                                        put("loop", false)
                                        put("autoplay", false)
                                        put("number", 0)
                                    })
                                }
                                put("smallIslandArea", smallIslandArea)
                            }
                            put("param_island", paramIsland)
                        }
                        builder.extras.putString("miui.focus.param.custom", customJson.toString())
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else if (castMode == "originisland") {
            try {
                // Ensure required defaults for fallback shade rendering
                builder.setSmallIcon(iconRes)
                builder.setContentTitle(title)
                builder.setContentText(text)
                
                val superXBundle = android.os.Bundle()
                val progress = intent.getIntExtra("progress", 0)
                val progressMax = intent.getIntExtra("progress_max", 0)
                val isProgressMode = showProgress && progressMax > 0 && progress < progressMax
                
                val rawJson = intent.getStringExtra("raw_origin_json")
                if (!rawJson.isNullOrBlank()) {
                    // Manual UI testing injection based on JSON string
                    // Not natively supported yet but leaving space since placeholder supplies it.
                }

                try {
                    val clazz = android.app.NotificationManager::class.java
                    val method = clazz.getMethod("setSuperXInfosSceneList", MutableList::class.java, MutableList::class.java, MutableList::class.java, MutableList::class.java)
                    val sceneList = arrayListOf("TRAIN")
                    val switchList = arrayListOf("true")
                    val pkgList = arrayListOf(packageName)
                    val pkgSwitchList = arrayListOf("true")
                    method.invoke(notificationManager, sceneList, switchList, pkgList, pkgSwitchList)
                } catch (e: Exception) {
                    // Method may not exist on non-Vivo devices or newer versions
                }

                superXBundle.putInt("notification.superx.operation", 0)
                superXBundle.putBoolean("notification.superx.showNotify", true)
                superXBundle.putInt("notification.superx.template", if (isProgressMode) 2 else 1)
                superXBundle.putString("notification.superx.scene", "TRAIN")
                superXBundle.putInt("notification.superx.changedRecord", 0)
                
                // Base Infos
                val baseInfos = android.os.Bundle()
                baseInfos.putCharSequence("notification.superx.baseInfos.title", title)
                baseInfos.putCharSequence("notification.superx.baseInfos.content", text)
                
                val launchIntent = packageManager.getLaunchIntentForPackage(packageName)!!
                val clickResp = android.app.PendingIntent.getActivity(this, 0, launchIntent, android.app.PendingIntent.FLAG_IMMUTABLE)
                superXBundle.putParcelable("notification.superx.clickResp", clickResp)
                
                // Capsule (Required for Island template resolution)
                val capsuleBundle = android.os.Bundle()
                capsuleBundle.putInt("notification.superx.capsule.state", 1)
                capsuleBundle.putCharSequence("notification.superx.capsule.content", title)
                if (iconObj != null && Build.VERSION.SDK_INT >= 23) capsuleBundle.putParcelable("notification.superx.capsule.icon", iconObj)
                superXBundle.putBundle("notification.superx.capsule", capsuleBundle)
                
                if (iconObj != null && Build.VERSION.SDK_INT >= 23) {
                    baseInfos.putParcelable("notification.superx.baseInfos.icon", iconObj)
                }
                
                if (!subtext.isNullOrBlank()) {
                    baseInfos.putInt("notification.superx.baseInfos.subInfo", 1)
                    baseInfos.putString("notification.superx.baseInfos.subText", subtext)
                }
                superXBundle.putBundle("notification.superx.baseInfos", baseInfos)
                
                // Infos (Core data mapping)
                if (isProgressMode) {
                    val infoBundle = android.os.Bundle()
                    val progressPercent = (progress * 100) / progressMax
                    infoBundle.putInt("notification.superx.infos.progress", progressPercent)
                    infoBundle.putInt("notification.superx.infos.progressColor", androidx.core.content.ContextCompat.getColor(this, R.color.purple_500))
                    val iconList = ArrayList<android.graphics.drawable.Icon>()
                    // Provide required minimum (2-5) placeholder nodes
                    if (Build.VERSION.SDK_INT >= 23) {
                        iconList.add(android.graphics.drawable.Icon.createWithResource(this, R.drawable.ic_alert))
                        iconList.add(android.graphics.drawable.Icon.createWithResource(this, R.drawable.ic_alert))
                        infoBundle.putParcelableArrayList("notification.superx.infos.nodeIcon", iconList)
                        infoBundle.putParcelable("notification.superx.infos.indicatorIcon", android.graphics.drawable.Icon.createWithResource(this, R.drawable.ic_alert))
                        infoBundle.putInt("notification.superx.infos.indicatorLoc", 1)
                    }
                    superXBundle.putBundle("notification.superx.infos", infoBundle)
                } else {
                    val infoBundle = android.os.Bundle()
                    infoBundle.putString("notification.superx.infos.describe", subtext ?: "Details")
                    infoBundle.putString("notification.superx.infos.coreInfo", text)
                    if (iconObj != null && Build.VERSION.SDK_INT >= 23) {
                        infoBundle.putParcelable("notification.superx.infos.image", iconObj)
                    } else if (Build.VERSION.SDK_INT >= 23) {
                        infoBundle.putParcelable("notification.superx.infos.image", android.graphics.drawable.Icon.createWithResource(this, R.drawable.ic_alert))
                    }
                    superXBundle.putBundle("notification.superx.infos", infoBundle)
                }
                
                // Short Infos (Required for card expansion parity)
                val shortInfos = android.os.Bundle()
                shortInfos.putString("notification.superx.shortInfos.describeShort", subtext ?: "Details")
                shortInfos.putString("notification.superx.shortInfos.coreInfoShort", text)
                if (iconObj != null && Build.VERSION.SDK_INT >= 23) {
                    shortInfos.putParcelable("notification.superx.shortInfos.image", iconObj)
                } else if (Build.VERSION.SDK_INT >= 23) {
                    shortInfos.putParcelable("notification.superx.shortInfos.image", android.graphics.drawable.Icon.createWithResource(this, R.drawable.ic_alert))
                }
                superXBundle.putBundle("notification.superx.shortInfos", shortInfos)
                
                // Island Specs
                val islandBundle = android.os.Bundle()
                val originLeftText = intent.getStringExtra("origin_left_text")?.takeIf { it.isNotBlank() } ?: title
                val originMainText = intent.getStringExtra("origin_main_text")?.takeIf { it.isNotBlank() } ?: text
                
                val originRightTemplate = intent.getIntExtra("origin_right_template", if (isProgressMode) 2 else 4)
                islandBundle.putInt("island.superx.leftTemplate", 1)
                islandBundle.putInt("island.superx.rightTemplate", originRightTemplate)
                
                val leftBundle = android.os.Bundle()
                leftBundle.putString("island.superx.leftInfo.content", originLeftText)
                if (iconObj != null && Build.VERSION.SDK_INT >= 23) {
                    leftBundle.putParcelable("island.superx.leftInfo.icon", iconObj)
                } else if (Build.VERSION.SDK_INT >= 23) {
                    leftBundle.putParcelable("island.superx.leftInfo.icon", android.graphics.drawable.Icon.createWithResource(this, R.drawable.ic_alert))
                }
                islandBundle.putBundle("island.superx.leftInfo", leftBundle)
                
                val rightBundle = android.os.Bundle()
                when (originRightTemplate) {
                    1 -> { // Rhythm Pulse
                        rightBundle.putInt("island.superx.rightInfo.waveState", 1)
                        if (Build.VERSION.SDK_INT >= 23) {
                            val colors = java.util.ArrayList<Int>()
                            colors.add(androidx.core.content.ContextCompat.getColor(this, R.color.teal_200))
                            rightBundle.putIntegerArrayList("island.superx.rightInfo.waveColor", colors)
                        }
                    }
                    2 -> { // Progress
                        val progressPercent = if (progressMax > 0) (progress * 100) / progressMax else 50
                        rightBundle.putInt("island.superx.rightInfo.progressValue", progressPercent)
                        rightBundle.putInt("island.superx.rightInfo.progressState", 0)
                        rightBundle.putInt("island.superx.rightInfo.progressColor", androidx.core.content.ContextCompat.getColor(this, R.color.teal_200))
                    }
                    3 -> { // Loading
                        rightBundle.putInt("island.superx.rightInfo.loadingColor", androidx.core.content.ContextCompat.getColor(this, R.color.teal_200))
                    }
                    4, 5 -> { // Text+Icon or Icon+Text
                        rightBundle.putString("island.superx.rightInfo.content", originMainText)
                        if (iconObj != null && Build.VERSION.SDK_INT >= 23) {
                            rightBundle.putParcelable("island.superx.rightInfo.icon", iconObj)
                        }
                    }
                    6 -> { // Capsule Symmetry
                        rightBundle.putString("island.superx.rightInfo.capsuleContent", originMainText)
                        rightBundle.putInt("island.superx.rightInfo.capsuleBgColor", androidx.core.content.ContextCompat.getColor(this, R.color.teal_200))
                    }
                }
                islandBundle.putBundle("island.superx.rightInfo", rightBundle)
                
                superXBundle.putBundle("notification.superx.island", islandBundle)
                
                // Apply everything to standard builder
                builder.extras.putAll(superXBundle)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val notification = builder.build()
        notificationManager.notify(notificationId, notification)
    }

    private fun createNotificationChannel(channelId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (manager.getNotificationChannel(channelId) == null) {
                val channelName = if (channelId == HYPER_CHANNEL_ID) "HyperIsland" else "Live Updates"
                val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
                channel.description = "Channel for $channelName Service"
                channel.setSound(null, null) 
                channel.enableVibration(false)
                manager.createNotificationChannel(channel)
            }
        }
    }
}
