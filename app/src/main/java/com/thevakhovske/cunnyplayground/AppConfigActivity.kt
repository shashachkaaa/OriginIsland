package com.thevakhovske.cunnyplayground

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.preference.RadioButtonPreference
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import java.io.File

class AppConfigActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val packageName = intent.getStringExtra("package_name") ?: run { finish(); return }

        setContent {
            val controller = remember { ThemeController(ColorSchemeMode.System) }
            MiuixTheme(controller = controller) {
                AppConfigScreen(packageName, onBack = { finish() }, onSave = { finish() })
            }
        }
    }
}

@Composable
fun AppConfigScreen(packageName: String, onBack: () -> Unit, onSave: () -> Unit) {
    val context = LocalContext.current
    val pm = context.packageManager
    val prefs = context.getSharedPreferences("experimental_prefs", Context.MODE_PRIVATE)

    var appLabel by remember { mutableStateOf(packageName) }
    var appIcon by remember { mutableStateOf<android.graphics.drawable.Drawable?>(null) }

    LaunchedEffect(packageName) {
        try {
            val appInfo = pm.getApplicationInfo(packageName, 0)
            appLabel = appInfo.loadLabel(pm).toString()
            appIcon = appInfo.loadIcon(pm)
        } catch (_: Exception) {}
    }

    val castMode = remember { prefs.getString("cast_mode", "live_updates") ?: "live_updates" }

    var iconSource by remember { mutableStateOf(prefs.getString("${packageName}_icon_source", "default") ?: "default") }

    var hypLeftSource by remember { mutableStateOf(prefs.getString("${packageName}_hyper_left_source", "title") ?: "title") }
    var hypMainSource by remember { mutableStateOf(prefs.getString("${packageName}_hyper_main_source", "text") ?: "text") }
    var hypLeftRegex by remember { mutableStateOf(prefs.getString("${packageName}_hyper_left_regex", "") ?: "") }
    var hypMainRegex by remember { mutableStateOf(prefs.getString("${packageName}_hyper_main_regex", "") ?: "") }

    var luTextSource by remember { mutableStateOf(prefs.getString("${packageName}_text_source", "text") ?: "text") }
    var luRegex by remember { mutableStateOf(prefs.getString("${packageName}_regex_filter", "") ?: "") }

    var originLeftSource by remember { mutableStateOf(prefs.getString("${packageName}_origin_left_source", "title") ?: "title") }
    var originMainSource by remember { mutableStateOf(prefs.getString("${packageName}_origin_main_source", "text") ?: "text") }
    var originLeftRegex by remember { mutableStateOf(prefs.getString("${packageName}_origin_left_regex", "") ?: "") }
    var originMainRegex by remember { mutableStateOf(prefs.getString("${packageName}_origin_main_regex", "") ?: "") }
    var originRightTemplate by remember { mutableStateOf(prefs.getInt("${packageName}_origin_right_template", 4)) }

    val lastTitle = remember { prefs.getString("${packageName}_last_title", "N/A") ?: "N/A" }
    val lastText = remember { prefs.getString("${packageName}_last_text", "N/A") ?: "N/A" }
    val lastSubText = remember { prefs.getString("${packageName}_last_subtext", "N/A") ?: "N/A" }
    val lastDump = remember { prefs.getString("${packageName}_last_raw_dump", context.getString(R.string.msg_waiting)) ?: context.getString(R.string.msg_waiting) }

    val drawablesStr = remember { prefs.getString("${packageName}_last_drawables", "") ?: "" }
    val drawableIds = remember { drawablesStr.split(",").mapNotNull { it.trim().toIntOrNull() }.distinct() }

    val sourceContext = remember(packageName) {
        try { context.createPackageContext(packageName, 0) } catch (_: Exception) { null }
    }

    val previewIconBitmap = remember(iconSource, appIcon, drawableIds, sourceContext) {
        val drawable = when (iconSource) {
            "extracted" -> {
                val id = drawableIds.firstOrNull()
                if (id != null && sourceContext != null) {
                    try { ResourcesCompat.getDrawable(sourceContext.resources, id, sourceContext.theme) } catch (_: Exception) { appIcon }
                } else appIcon
            }
            "notification" -> {
                val iconFile = File(context.filesDir, "renders/${packageName}_small_icon.png")
                if (iconFile.exists()) {
                    try { android.graphics.drawable.BitmapDrawable(context.resources, BitmapFactory.decodeFile(iconFile.absolutePath)) } catch (_: Exception) { appIcon }
                } else appIcon
            }
            else -> appIcon
        }
        drawable?.toBitmap()?.asImageBitmap()
    }

    fun applyRegex(rawText: String, regexStr: String): String {
        if (regexStr.isEmpty()) return rawText
        return try {
            val regex = Regex(regexStr)
            val match = regex.find(rawText)
            if (match != null) {
                if (match.groups.size > 1) match.groupValues.drop(1).joinToString(" ") else match.value
            } else rawText
        } catch (_: Exception) { rawText }
    }

    fun getRawText(source: String): String = when (source) {
        "title" -> prefs.getString("${packageName}_last_title", "Title") ?: ""
        "subtext" -> prefs.getString("${packageName}_last_subtext", "SubText") ?: ""
        "titletext" -> {
            val t = prefs.getString("${packageName}_last_title", "Title") ?: ""
            val txt = prefs.getString("${packageName}_last_text", "Text") ?: ""
            "$t • $txt"
        }
        else -> prefs.getString("${packageName}_last_text", "Text") ?: ""
    }

    val limit7Char = remember { prefs.getBoolean("limit_chip_7char", false) }

    val previewLeftText = if (castMode == "originisland") {
        applyRegex(getRawText(originLeftSource), originLeftRegex)
    } else {
        applyRegex(getRawText(hypLeftSource), hypLeftRegex)
    }

    val previewMainText = if (castMode == "originisland") {
        applyRegex(getRawText(originMainSource), originMainRegex)
    } else {
        applyRegex(getRawText(hypMainSource), hypMainRegex)
    }

    val previewStatusTextRaw = applyRegex(getRawText(luTextSource), luRegex)
    val previewStatusText = if (limit7Char && previewStatusTextRaw.length > 7) previewStatusTextRaw.take(7) else previewStatusTextRaw

    fun saveSettings() {
        prefs.edit().apply {
            putString("${packageName}_icon_source", iconSource)
            if (castMode == "hyperisland") {
                putString("${packageName}_hyper_left_source", hypLeftSource)
                putString("${packageName}_hyper_main_source", hypMainSource)
                putString("${packageName}_hyper_left_regex", hypLeftRegex)
                putString("${packageName}_hyper_main_regex", hypMainRegex)
            } else if (castMode == "originisland") {
                putString("${packageName}_origin_left_source", originLeftSource)
                putString("${packageName}_origin_main_source", originMainSource)
                putString("${packageName}_origin_left_regex", originLeftRegex)
                putString("${packageName}_origin_main_regex", originMainRegex)
                putInt("${packageName}_origin_right_template", originRightTemplate)
            } else {
                putString("${packageName}_text_source", luTextSource)
                putString("${packageName}_regex_filter", luRegex)
            }
            apply()
        }
        context.sendBroadcast(android.content.Intent("com.thevakhovske.cunnyplayground.RELOAD_NOTIFICATIONS"))
        Toast.makeText(context, context.getString(R.string.msg_config_saved), Toast.LENGTH_SHORT).show()
        onSave()
    }

    val topAppBarScrollBehavior = MiuixScrollBehavior(rememberTopAppBarState())

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = appLabel,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(MiuixIcons.Back, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    Button(
                        onClick = { saveSettings() },
                        modifier = Modifier.padding(end = 8.dp)
                    ) { Text(stringResource(R.string.btn_save)) }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            contentPadding = paddingValues,
            modifier = Modifier
                .fillMaxSize()
                .scrollEndHaptic()
        ) {
            // App Info
            item {
                SmallTitle(stringResource(R.string.section_app_info))
                Card(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth()) {
                    BasicComponent(
                        title = appLabel,
                        summary = packageName,
                        startAction = {
                            appIcon?.let {
                                Image(
                                    painter = BitmapPainter(it.toBitmap().asImageBitmap()),
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }
                    )
                }
            }

            // Preview
            item {
                SmallTitle(stringResource(R.string.section_output_preview))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // Center the preview
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp, horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (castMode == "hyperisland") {
                            // Hyperisland Pill
                            Row(
                                modifier = Modifier
                                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(50))
                                    .background(androidx.compose.ui.graphics.Color.Black)
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                previewIconBitmap?.let { bitmap ->
                                    Image(
                                        bitmap = bitmap,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(20))
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = previewLeftText,
                                    color = androidx.compose.ui.graphics.Color.White,
                                    maxLines = 1,
                                    fontSize = 14.sp,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.widthIn(max = 100.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                // Camera Cutout Placeholder
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                        .background(androidx.compose.ui.graphics.Color.White)
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = previewMainText,
                                    color = androidx.compose.ui.graphics.Color.White,
                                    maxLines = 1,
                                    fontSize = 14.sp,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.widthIn(max = 140.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                            }
                        } else {
                            // Live Updates Pill
                            Row(
                                modifier = Modifier
                                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(50))
                                    .background(androidx.compose.ui.graphics.Color.Black)
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                previewIconBitmap?.let { bitmap ->
                                    Image(
                                        bitmap = bitmap,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(20))
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = previewStatusText,
                                    color = androidx.compose.ui.graphics.Color.White,
                                    maxLines = 1,
                                    fontSize = 14.sp,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.widthIn(max = 100.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                            }
                        }
                    }
                }
            }

            // Icon Source
            item {
                SmallTitle(stringResource(R.string.section_icon_source))
                Card(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth()) {
                    RadioButtonPreference(
                        selected = iconSource == "app",
                        onClick = { iconSource = "app" },
                        title = stringResource(R.string.pref_icon_app)
                    )
                    RadioButtonPreference(
                        selected = iconSource == "notification",
                        onClick = { iconSource = "notification" },
                        title = stringResource(R.string.pref_icon_notif)
                    )
                    RadioButtonPreference(
                        selected = iconSource == "extracted",
                        onClick = { iconSource = "extracted" },
                        title = if (drawableIds.isEmpty()) stringResource(R.string.pref_icon_extracted_none) else stringResource(R.string.pref_icon_extracted),
                        enabled = drawableIds.isNotEmpty()
                    )
                }
            }

            // Mode-specific Config
            if (castMode == "hyperisland") {
                item {
                    SmallTitle(stringResource(R.string.section_hyper_mapping))

                    SmallTitle(stringResource(R.string.section_left_source))
                    val sources = listOf("title", "text", "subtext", "titletext")
                    val sourceLabels = listOf(
                        stringResource(R.string.label_title),
                        stringResource(R.string.label_text),
                        stringResource(R.string.label_subtext),
                        "${stringResource(R.string.label_title)}+${stringResource(R.string.label_text)}"
                    )
                    Card(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth()) {
                        sources.forEachIndexed { index, source ->
                            RadioButtonPreference(
                                selected = hypLeftSource == source,
                                onClick = { hypLeftSource = source },
                                title = sourceLabels[index]
                            )
                        }
                        TextField(
                            value = hypLeftRegex,
                            onValueChange = { hypLeftRegex = it },
                            label = stringResource(R.string.label_left_regex),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    SmallTitle(stringResource(R.string.section_main_source))
                    Card(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth()) {
                        sources.forEachIndexed { index, source ->
                            RadioButtonPreference(
                                selected = hypMainSource == source,
                                onClick = { hypMainSource = source },
                                title = sourceLabels[index]
                            )
                        }
                        TextField(
                            value = hypMainRegex,
                            onValueChange = { hypMainRegex = it },
                            label = stringResource(R.string.label_main_regex),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                }
            } else if (castMode == "originisland") {
                item {
                    SmallTitle(stringResource(R.string.section_origin_mapping))

                    SmallTitle(stringResource(R.string.section_left_source))
                    val sources = listOf("title", "text", "subtext", "titletext")
                    val sourceLabels = listOf(
                        stringResource(R.string.label_title),
                        stringResource(R.string.label_text),
                        stringResource(R.string.label_subtext),
                        "${stringResource(R.string.label_title)}+${stringResource(R.string.label_text)}"
                    )
                    Card(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth()) {
                        sources.forEachIndexed { index, source ->
                            RadioButtonPreference(
                                selected = originLeftSource == source,
                                onClick = { originLeftSource = source },
                                title = sourceLabels[index]
                            )
                        }
                        TextField(
                            value = originLeftRegex,
                            onValueChange = { originLeftRegex = it },
                            label = stringResource(R.string.label_left_regex),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    SmallTitle(stringResource(R.string.section_main_source))
                    Card(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth()) {
                        sources.forEachIndexed { index, source ->
                            RadioButtonPreference(
                                selected = originMainSource == source,
                                onClick = { originMainSource = source },
                                title = sourceLabels[index]
                            )
                        }
                        TextField(
                            value = originMainRegex,
                            onValueChange = { originMainRegex = it },
                            label = stringResource(R.string.label_main_regex),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    SmallTitle(stringResource(R.string.section_right_template))
                    val rightTemplates = listOf(
                        stringResource(R.string.template_rhythm) to 1,
                        stringResource(R.string.template_dynamic_progress) to 2,
                        stringResource(R.string.template_loading) to 3,
                        stringResource(R.string.template_text_icon) to 4,
                        stringResource(R.string.template_icon_text) to 5,
                        stringResource(R.string.template_symmetry) to 6
                    )
                    Card(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth()) {
                        rightTemplates.forEach { (label, value) ->
                            RadioButtonPreference(
                                selected = originRightTemplate == value,
                                onClick = { originRightTemplate = value },
                                title = label
                            )
                        }
                    }
                }
            } else {
                item {
                    SmallTitle(stringResource(R.string.section_lu_mapping))

                    SmallTitle(stringResource(R.string.section_text_source))
                    Card(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth()) {
                        RadioButtonPreference(
                            selected = luTextSource == "title",
                            onClick = { luTextSource = "title" },
                            title = stringResource(R.string.label_title)
                        )
                        RadioButtonPreference(
                            selected = luTextSource == "text",
                            onClick = { luTextSource = "text" },
                            title = stringResource(R.string.label_text)
                        )
                        RadioButtonPreference(
                            selected = luTextSource == "subtext",
                            onClick = { luTextSource = "subtext" },
                            title = stringResource(R.string.label_subtext)
                        )
                        RadioButtonPreference(
                            selected = luTextSource == "titletext",
                            onClick = { luTextSource = "titletext" },
                            title = "${stringResource(R.string.label_title)}+${stringResource(R.string.label_text)}"
                        )

                        TextField(
                            value = luRegex,
                            onValueChange = { luRegex = it },
                            label = stringResource(R.string.label_text_regex),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Discovered Resources
            if (drawableIds.isNotEmpty()) {
                item {
                    SmallTitle(stringResource(R.string.section_discovered_res))

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        items(drawableIds) { id ->
                            sourceContext?.let { ctx ->
                                val drawable = remember(id) {
                                    try { ResourcesCompat.getDrawable(ctx.resources, id, ctx.theme) } catch (_: Exception) { null }
                                }
                                drawable?.let {
                                    Image(
                                        painter = BitmapPainter(it.toBitmap().asImageBitmap()),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clickable {
                                                val resName = try { ctx.resources.getResourceEntryName(id) } catch (_: Exception) { id.toString() }
                                                Toast.makeText(context, context.getString(R.string.msg_resource_info, id, resName), Toast.LENGTH_SHORT).show()
                                            }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Notification Render Preview
            item {
                val renderFile = remember(packageName) { File(context.filesDir, "renders/${packageName}.png") }
                if (renderFile.exists()) {
                    SmallTitle(stringResource(R.string.section_last_render))
                    val bitmap = remember(packageName) {
                        try { BitmapFactory.decodeFile(renderFile.absolutePath) } catch (_: Exception) { null }
                    }
                    if (bitmap != null) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = stringResource(R.string.cd_render_preview),
                                modifier = Modifier.fillMaxWidth().padding(8.dp)
                            )
                        }
                    }
                }
            }

            // Raw Data
            item {
                SmallTitle(stringResource(R.string.section_raw_data))
                Card(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth()) {
                    BasicComponent(title = stringResource(R.string.raw_field, stringResource(R.string.label_title), lastTitle))
                    BasicComponent(title = stringResource(R.string.raw_field, stringResource(R.string.label_text), lastText))
                    BasicComponent(title = stringResource(R.string.raw_field, stringResource(R.string.label_subtext), lastSubText))
                }
            }

            item {
                SmallTitle(stringResource(R.string.section_raw_dump))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = lastDump,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText(context.getString(R.string.clip_label_dump), lastDump))
                        Toast.makeText(context, context.getString(R.string.msg_dump_copied), Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                ) { Text(stringResource(R.string.btn_copy_dump)) }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
