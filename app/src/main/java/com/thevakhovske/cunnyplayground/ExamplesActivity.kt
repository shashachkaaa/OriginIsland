package com.thevakhovske.cunnyplayground

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.widget.RemoteViews
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

class ExamplesActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "island_examples",
                getString(R.string.channel_examples),
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        setContent {
            val controller = remember { ThemeController(ColorSchemeMode.System) }
            MiuixTheme(controller = controller) {
                ExamplesScreen(onBack = { finish() })
            }
        }
    }
}

private fun reflectSetPromotedOngoing(builder: Notification.Builder) {
    try {
        val method = builder.javaClass.getMethod("setRequestPromotedOngoing", Boolean::class.javaPrimitiveType)
        method.invoke(builder, true)
    } catch (e: Exception) {
        // Ignore if method not found
    }
}

@Composable
fun ExamplesScreen(onBack: () -> Unit) {
    val scrollBehavior = MiuixScrollBehavior(rememberTopAppBarState())
    val context = LocalContext.current

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = stringResource(R.string.title_examples),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(MiuixIcons.Back, contentDescription = stringResource(R.string.back))
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        LazyColumn(
            contentPadding = paddingValues,
            modifier = Modifier
                .fillMaxSize()
                .scrollEndHaptic()
        ) {
            item {
                ExampleCard(
                    title = stringResource(R.string.example_a_title),
                    summary = stringResource(R.string.example_a_summary),
                    onClick = { runExampleA(context) }
                )
            }
            item {
                ExampleCard(
                    title = stringResource(R.string.example_b_title),
                    summary = stringResource(R.string.example_b_summary),
                    onClick = { runExampleB(context) }
                )
            }
            item {
                ExampleCard(
                    title = stringResource(R.string.example_c_title),
                    summary = stringResource(R.string.example_c_summary),
                    onClick = { runExampleC(context) }
                )
            }
            item {
                ExampleCard(
                    title = stringResource(R.string.example_d_title),
                    summary = stringResource(R.string.example_d_summary),
                    onClick = { runExampleD(context) }
                )
            }
            item {
                ExampleCard(
                    title = stringResource(R.string.example_e_title),
                    summary = stringResource(R.string.example_e_summary),
                    onClick = { runExampleE(context) }
                )
            }
            item {
                ExampleCard(
                    title = stringResource(R.string.example_f_title),
                    summary = stringResource(R.string.example_f_summary),
                    onClick = { runExampleF(context) }
                )
            }
        }
    }
}

@Composable
fun ExampleCard(title: String, summary: String, onClick: () -> Unit) {
    Card(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth()) {
        BasicComponent(
            title = title,
            summary = summary,
            endActions = {
                Button(onClick = onClick) {
                    Text(stringResource(R.string.btn_run))
                }
            }
        )
    }
}

// ── Examples Implementations ──

private fun getManager(context: Context) = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

private fun runExampleA(context: Context) {
    val remoteViews = RemoteViews(context.packageName, R.layout.my_insane_island_layout)
    val focusParamCustom = """
    {
      "protocol": 3,
      "business": "remote_view_hijack",
      "updatable": true,
      "enableFloat": true,
      "islandPriority": 2,
      "highlightColor": "#FF0055",
      "param_island": {
        "islandProperty": 1,
        "islandPriority": 2,
        "smallIslandArea": {},
        "bigIslandArea": {}
      }
    }
    """.trimIndent()
    val builder = Notification.Builder(context, "island_examples")
        .setSmallIcon(R.drawable.ic_alert)
        .setContentTitle(context.getString(R.string.example_a_notif_title))
        .setContentText(context.getString(R.string.example_a_notif_text))
        .setOngoing(true)
    
    reflectSetPromotedOngoing(builder)

    val extras = Bundle().apply {
        putParcelable("miui.focus.rv", remoteViews)
        putParcelable("miui.focus.rv.island.expand", remoteViews)
        putString("miui.focus.param.custom", focusParamCustom)
        putBoolean("miui.exitFloating", true)
        putBoolean("miui.focus.isPromoted", true)
        putBoolean("android.app.extra.PROMOTED_ONGOING", true)
        putBoolean("miui.focus.isFocus", true)
    }
    builder.addExtras(extras)

    val notification = builder.build()
    getManager(context).notify(1337, notification)
}

private fun runExampleB(context: Context) {
    val paramJson = """
    {
      "param_v2": {
        "protocol": 3,
        "business": "sports_ticker",
        "updatable": true,
        "enableFloat": true,
        "islandPriority": 1,
        "highlightColor": "#FFCC00",
        "param_island": {
          "islandPriority": 1,
          "smallIslandArea": {
            "picInfo": { "type": 0, "pic": "ic_sports_basketball" }
          },
          "bigIslandArea": {
            "imageTextInfoLeft": {
              "type": 1,
              "picInfo": { "type": 0, "pic": "ic_team_logo" },
              "textInfo": { "title": "LAL", "subTitle": "112" }
            },
            "sameWidthDigitInfo": {
              "content": "Q4 ",
              "digit": "02:15",
              "showHighlightColor": true,
              "turnAnim": true,
              "timerInfo": {
                "timerType": 1,
                "timerTotal": 720000, 
                "timerWhen": 1729372100000, 
                "timerSystemCurrent": 1729371965000 
              }
            }
          }
        },
        "baseInfo": { "type": 1, "title": "LAL takes the lead!", "content": "Quarter 4 ending..." },
        "iconTextInfo": { "animIconInfo": { "type": 0, "src": "ic_sports" }, "title": "LAL takes the lead!", "content": "Quarter 4 ending..." }
      }
    }
    """.trimIndent()
    val extras = Bundle().apply {
        putString("miui.focus.param", paramJson)
        putString("miui.ticker.data", Base64.encodeToString("LAL takes the lead!".toByteArray(), Base64.NO_WRAP))
    }
    val builder = Notification.Builder(context, "island_examples")
        .setSmallIcon(R.mipmap.ic_launcher_round)
        .setOngoing(true)
    
    reflectSetPromotedOngoing(builder)

    builder.addExtras(extras.apply {
        putBoolean("miui.focus.isPromoted", true)
        putBoolean("android.app.extra.PROMOTED_ONGOING", true)
        putBoolean("miui.focus.isFocus", true)
    })

    val notification = builder.build()
    getManager(context).notify(1338, notification)
}

private fun runExampleC(context: Context) {
    val paramJson = """
    {
      "param_v2": {
        "protocol": 3,
        "business": "magical_item_found",
        "updatable": true,
        "enableFloat": true,
        "highlightColor": "#9932CC",
        "outEffectSrc": "outer_glow",
        "picInfo": {
          "type": 2,
          "pic": "raw_lottie_sparkles_id", 
          "loop": true,
          "autoplay": true
        },
        "param_island": {
          "smallIslandArea": {
            "picInfo": { "type": 2, "pic": "raw_lottie_sparkles_id", "loop": true }
          },
          "bigIslandArea": {
            "imageTextInfoLeft": {
              "type": 1,
              "picInfo": { "type": 2, "pic": "raw_lottie_sparkles_id", "loop": true },
              "textInfo": {
                "title": "Legendary Drop!",
                "subTitle": "Tap to claim..."
              }
            }
          }
        },
        "baseInfo": { "type": 1, "title": "Legendary Drop!", "content": "Tap to claim..." },
        "iconTextInfo": { "animIconInfo": { "type": 0, "src": "ic_loot" }, "title": "Legendary Drop!", "content": "Tap to claim..." }
      }
    }
    """.trimIndent()
    val extras = Bundle().apply {
        putString("miui.focus.param", paramJson)
        putString("miui.effect.color", "#9932CC")
        putString("miui.bigIsland.effect.src", "anim_aura_glow.json") 
        putString("miui.effect.src", "anim_aura_glow.json")
    }
    val builder = Notification.Builder(context, "island_examples")
        .setSmallIcon(R.mipmap.ic_launcher_round)
        .setOngoing(true)
    
    reflectSetPromotedOngoing(builder)

    builder.addExtras(extras.apply {
        putBoolean("miui.focus.isPromoted", true)
        putBoolean("android.app.extra.PROMOTED_ONGOING", true)
        putBoolean("miui.focus.isFocus", true)
    })

    val notification = builder.build()
    getManager(context).notify(1339, notification)
}

private fun runExampleD(context: Context) {
    val userAvatarBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.BLUE) }
    val cameraPreviewBitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.GREEN) }
    val bitmapBundle = Bundle().apply {
        putParcelable("live_avatar", userAvatarBitmap)
        putParcelable("live_preview", cameraPreviewBitmap)
    }
    val paramJson = """
    {
      "param_v2": {
        "protocol": 3,
        "business": "video_call",
        "updatable": true,
        "enableFloat": true,
        "highlightColor": "#34C759",
        "param_island": {
          "smallIslandArea": {
            "picInfo": { "type": 0, "pic": "live_avatar" } 
          },
          "bigIslandArea": {
            "imageTextInfoLeft": {
              "type": 1,
              "picInfo": { "type": 0, "pic": "live_avatar" },
              "textInfo": {
                "title": "Incoming Call",
                "subTitle": "Mom"
              }
            },
            "imageTextInfoRight": {
              "type": 6, 
              "picInfo": { "type": 0, "pic": "live_preview" } 
            }
          }
        },
        "baseInfo": { "type": 1, "title": "Incoming Call", "content": "Mom" },
        "iconTextInfo": { "animIconInfo": { "type": 0, "src": "ic_call" }, "title": "Incoming Call", "content": "Mom" }
      }
    }
    """.trimIndent()
    val extras = Bundle().apply {
        putString("miui.focus.param", paramJson)
        putBundle("miui.focus.pics", bitmapBundle)
        putBoolean("miui.focus.isPromoted", true)
    }
    val builder = Notification.Builder(context, "island_examples")
        .setSmallIcon(R.drawable.ic_call)
        .setOngoing(true)
    
    reflectSetPromotedOngoing(builder)

    builder.addExtras(extras.apply {
        putBoolean("miui.focus.isPromoted", true)
        putBoolean("android.app.extra.PROMOTED_ONGOING", true)
        putBoolean("miui.focus.isFocus", true)
    })

    val notification = builder.build()
    getManager(context).notify(1340, notification)
}

private fun runExampleE(context: Context) {
    val paramJson = """
    {
      "param_v2": {
        "protocol": 3,
        "business": "cloud_sync",
        "updatable": true,
        "enableFloat": true,
        "islandPriority": 1,
        "highlightColor": "#0A84FF",
        "param_island": {
          "islandPriority": 1,
          "smallIslandArea": {
            "combinePicInfo": {
              "picInfo": { "type": 0, "pic": "ic_cloud" },
              "smallPicInfo": { "type": 0, "pic": "ic_sync_arrow" },
              "progressInfo": { "progress": 82, "colorReach": "#0A84FF", "colorUnReach": "#330A84FF", "isCCW": false }
            }
          },
          "bigIslandArea": {
            "imageTextInfoLeft": {
              "type": 1,
              "picInfo": { "type": 0, "pic": "ic_cloud" },
              "textInfo": { "title": "Backing up to Xiaomi Cloud", "subTitle": "82% - 4 mins remaining" }
            },
            "progressTextInfo": {
              "progressInfo": { "progress": 82, "colorReach": "#0A84FF", "colorUnReach": "#330A84FF", "isCCW": false }
            }
          }
        },
        "baseInfo": { "type": 1, "title": "Backing up to Xiaomi Cloud", "content": "82% - 4 mins remaining" },
        "iconTextInfo": { "animIconInfo": { "type": 0, "src": "ic_cloud" }, "title": "Backing up to Xiaomi Cloud", "content": "82% - 4 mins remaining" }
      }
    }
    """.trimIndent()
    val builder = Notification.Builder(context, "island_examples")
        .setSmallIcon(R.mipmap.ic_launcher_round)
        .setOngoing(true)
    
    reflectSetPromotedOngoing(builder)

    builder.addExtras(Bundle().apply {
        putString("miui.focus.param", paramJson)
        putBoolean("miui.focus.isPromoted", true)
        putBoolean("android.app.extra.PROMOTED_ONGOING", true)
        putBoolean("miui.focus.isFocus", true)
    })

    val notification = builder.build()
    getManager(context).notify(1341, notification)
}

private fun runExampleF(context: Context) {
    val paramJson = """
    {
      "param_v2": {
        "protocol": 3,
        "business": "shareable_clipboard",
        "updatable": true,
        "enableFloat": true,
        "highlightColor": "#FF3B30",
        "param_island": {
          "shareData": {
            "title": "Copied Text",
            "content": "Drag to share anywhere!",
            "pic": "ic_drag_preview",
            "shareContent": "Hey, check out this secret copied text!",
            "sharePic": ""
          },
          "smallIslandArea": {
            "picInfo": { "type": 0, "pic": "ic_clipboard" }
          },
          "bigIslandArea": {
            "imageTextInfoLeft": {
              "type": 1,
              "picInfo": { "type": 0, "pic": "ic_clipboard" },
              "textInfo": {
                "title": "Clipboard Manager",
                "isTitleDigit": true,
                "narrowFont": true,
                "turnAnim": true
              }
            },
            "imageTextInfoRight": {
              "type": 6,
              "textInfo": {
                "content": "DRAG ME",
                "showHighlightColor": true
              }
            }
          }
        },
        "baseInfo": { "type": 1, "title": "Copied to Clipboard", "content": "Drag from Island to share." },
        "iconTextInfo": { "animIconInfo": { "type": 0, "src": "ic_clipboard" }, "title": "Copied to Clipboard", "content": "Drag from Island to share." }
      }
    }
    """.trimIndent()
    val builder = Notification.Builder(context, "island_examples")
        .setSmallIcon(R.mipmap.ic_launcher_round)
        .setOngoing(true)
    
    reflectSetPromotedOngoing(builder)

    builder.addExtras(Bundle().apply {
        putString("miui.focus.param", paramJson)
        putBoolean("miui.focus.isPromoted", true)
        putBoolean("android.app.extra.PROMOTED_ONGOING", true)
        putBoolean("miui.focus.isFocus", true)
    })

    val notification = builder.build()
    getManager(context).notify(1342, notification)
}
