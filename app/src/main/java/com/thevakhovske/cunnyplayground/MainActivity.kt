package com.thevakhovske.cunnyplayground

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Notes
import top.yukonga.miuix.kmp.icon.extended.NotesFill
import top.yukonga.miuix.kmp.icon.extended.Send
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.CheckboxPreference
import top.yukonga.miuix.kmp.preference.RadioButtonPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.basic.SnackbarResult
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.Edit
import top.yukonga.miuix.kmp.icon.extended.SelectAll
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.utils.overScrollVertical

data class NotificationInfo(
    var id: Int,
    var title: String,
    var text: String,
    var iconRes: Int,
    var isPromoted: Boolean,
    var statusChipText: String?,
    var showProgress: Boolean,
    var timestamp: Long = System.currentTimeMillis()
)

data class EnabledApp(
    val name: String,
    val packageName: String,
    val icon: android.graphics.drawable.Drawable
)

class MainActivity : ComponentActivity() {

    companion object {
        const val CHANNEL_ID = "live_updates_channel"
        const val NOTIFICATION_ID = 1001
        const val PERMISSION_REQUEST_CODE = 101
        const val EXTRA_REQUEST_PROMOTED_ONGOING = "android.app.extra.PROMOTED_ONGOING"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel(notificationManager)
        checkPermissions()

        setContent {
            val controller = remember { ThemeController(ColorSchemeMode.System) }
            MiuixTheme(controller = controller) {
                MainScreen()
            }
        }
    }

    private fun createNotificationChannel(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_name)
            val descriptionText = getString(R.string.channel_desc)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), PERMISSION_REQUEST_CODE)
            }
        }
    }
}

@Composable
fun MainScreen() {
    var selectedTab by remember { mutableIntStateOf(0) }

    val isMiui = remember { isMiuiRegion() }
    val isVivo = remember { isVivoDevice() }
    val tabPlayground = stringResource(R.string.tab_playground)
    val tabHyperIsland = if (isMiui) stringResource(R.string.tab_hyperisland) else ""
    val tabRecaster = stringResource(R.string.tab_recaster)
    
    val labels = remember(isMiui, isVivo, tabPlayground, tabHyperIsland, tabRecaster) {
        mutableListOf(tabPlayground).apply {
            if (isMiui) add(tabHyperIsland)
            if (isVivo) add("OriginIsland")
            add(tabRecaster)
        }
    }

    val scrollBehavior = MiuixScrollBehavior()

    Scaffold(
        topBar = {
            TopAppBar(
                title = when {
                    isMiui && selectedTab == labels.indexOf(tabHyperIsland) -> stringResource(R.string.title_hyperisland)
                    isVivo && selectedTab == labels.indexOf("OriginIsland") -> stringResource(R.string.title_originisland)
                    selectedTab == labels.indexOf(tabRecaster) -> stringResource(R.string.title_recaster)
                    else -> stringResource(R.string.title_playground)
                },
                actions = {
                    val tabLabel = labels.getOrNull(selectedTab)
                    if (tabLabel == tabHyperIsland || tabLabel == "OriginIsland") {
                        val context = LocalContext.current
                        IconButton(onClick = { context.startActivity(Intent(context, ExamplesActivity::class.java)) }) {
                            Icon(imageVector = MiuixIcons.Settings, contentDescription = stringResource(R.string.settings))
                        }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = {
            NavigationBar {
                labels.forEachIndexed { index, label ->
                    val navIcon = when (label) {
                        tabPlayground -> MiuixIcons.Notes
                        tabHyperIsland -> MiuixIcons.NotesFill
                        "OriginIsland" -> MiuixIcons.NotesFill // Placeholder
                        else -> MiuixIcons.Send
                    }
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = navIcon,
                        label = label
                    )
                }
            }
        }
    ) { paddingValues ->
        when (labels.getOrNull(selectedTab)) {
            tabPlayground -> PlaygroundScreen(paddingValues, scrollBehavior)
            tabHyperIsland -> HyperIslandScreen(paddingValues, scrollBehavior)
            "OriginIsland" -> OriginIslandScreen(paddingValues, scrollBehavior)
            tabRecaster -> RecasterScreen(paddingValues, scrollBehavior, isMiui, isVivo)
            else -> PlaygroundScreen(paddingValues, scrollBehavior)
        }
    }
}

@Composable
fun PlaygroundScreen(paddingValues: PaddingValues, scrollBehavior: ScrollBehavior) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(context.getString(R.string.mode_live_updates)) }
    var text by remember { mutableStateOf("") }
    var subtext by remember { mutableStateOf("") }
    var statusChipText by remember { mutableStateOf("50%") }
    var isPromoted by remember { mutableStateOf(true) }
    var isOngoing by remember { mutableStateOf(true) }
    var showProgress by remember { mutableStateOf(true) }
    var useChrono by remember { mutableStateOf(false) }
    var selectedIcon by remember { mutableIntStateOf(0) }

    val notifications = remember { mutableStateListOf<NotificationInfo>() }
    var lastId by remember { mutableIntStateOf(1000) }
    var editingId by remember { mutableStateOf<Int?>(null) }

    LazyColumn(
        contentPadding = paddingValues,
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .scrollEndHaptic()
    ) {
        item {
            SmallTitle(stringResource(R.string.section_notif_info))
            TextField(
                value = title,
                onValueChange = { title = it },
                label = stringResource(R.string.label_title),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            )
            TextField(
                value = text,
                onValueChange = { text = it },
                label = stringResource(R.string.label_text),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            )
            TextField(
                value = subtext,
                onValueChange = { subtext = it },
                label = stringResource(R.string.label_subtext),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            )
            TextField(
                value = statusChipText,
                onValueChange = { statusChipText = it },
                label = stringResource(R.string.label_chip_text),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        item {
            SmallTitle(stringResource(R.string.section_settings))
            Card(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth()) {
                CheckboxPreference(
                    checked = isOngoing,
                    onCheckedChange = { isOngoing = it },
                    title = stringResource(R.string.pref_ongoing)
                )
                CheckboxPreference(
                    checked = isPromoted,
                    onCheckedChange = { isPromoted = it },
                    title = stringResource(R.string.pref_promoted)
                )
                CheckboxPreference(
                    checked = useChrono,
                    onCheckedChange = { useChrono = it },
                    title = stringResource(R.string.pref_chrono)
                )
                CheckboxPreference(
                    checked = showProgress,
                    onCheckedChange = { showProgress = it },
                    title = stringResource(R.string.pref_show_progress)
                )
            }
        }

        item {
            SmallTitle(stringResource(R.string.section_icon))
            Card(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth()) {
                RadioButtonPreference(
                    selected = selectedIcon == 0,
                    onClick = { selectedIcon = 0 },
                    title = stringResource(R.string.icon_timer)
                )
                RadioButtonPreference(
                    selected = selectedIcon == 1,
                    onClick = { selectedIcon = 1 },
                    title = stringResource(R.string.icon_call)
                )
                RadioButtonPreference(
                    selected = selectedIcon == 2,
                    onClick = { selectedIcon = 2 },
                    title = stringResource(R.string.icon_alert)
                )
                RadioButtonPreference(
                    selected = selectedIcon == 3,
                    onClick = { selectedIcon = 3 },
                    title = stringResource(R.string.icon_default)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        editingId = null
                        val iconRes = getIconRes(selectedIcon)
                        val nId = ++lastId
                        notifications.add(NotificationInfo(nId, title, text, iconRes, isPromoted, statusChipText, showProgress))
                        postNotification(context, title, text, subtext, statusChipText, nId, iconRes, isPromoted, showProgress)
                    },
                    modifier = Modifier.weight(1f)
                ) { Text(stringResource(R.string.btn_post)) }
                Button(
                    onClick = {
                        val updateId = editingId ?: notifications.lastOrNull()?.id
                        if (updateId != null) {
                            val iconRes = getIconRes(selectedIcon)
                            val updatedText = context.getString(R.string.text_updated, text)
                            val idx = notifications.indexOfFirst { it.id == updateId }
                            if (idx != -1) {
                                notifications[idx] = notifications[idx].copy(title = title, text = updatedText, iconRes = iconRes, isPromoted = isPromoted, statusChipText = statusChipText, showProgress = showProgress)
                            }
                            postNotification(context, title, updatedText, subtext, statusChipText, updateId, iconRes, isPromoted, showProgress)
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) { Text(stringResource(R.string.btn_update)) }
                Button(
                    onClick = {
                        stopService(context)
                        notifications.clear()
                    },
                    modifier = Modifier.weight(1f)
                ) { Text(stringResource(R.string.btn_clear_all)) }
            }
        }

        if (notifications.isNotEmpty()) {
            item { SmallTitle(stringResource(R.string.section_posted_notifs)) }
            items(notifications.toList()) { notif ->
                BasicComponent(
                    title = notif.title,
                    summary = notificationSummary(notif),
                    onClick = {
                        editingId = notif.id
                        title = notif.title
                        text = notif.text
                        statusChipText = notif.statusChipText ?: ""
                        isPromoted = notif.isPromoted
                        showProgress = notif.showProgress
                    }
                )
            }
        }
    }
}

@Composable
fun HyperIslandScreen(paddingValues: PaddingValues, scrollBehavior: ScrollBehavior) {
    val context = LocalContext.current
    var hTitle by remember { mutableStateOf(context.getString(R.string.mode_hyperisland)) }
    var hText by remember { mutableStateOf("") }
    var hSubText by remember { mutableStateOf("") }
    var hLeftText by remember { mutableStateOf("") }
    var hMainText by remember { mutableStateOf("") }
    var rawJson by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableIntStateOf(0) }

    LazyColumn(
        contentPadding = paddingValues,
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .scrollEndHaptic()
    ) {
        item {
            SmallTitle(stringResource(R.string.section_hyper_payload))
            TextField(
                value = hTitle,
                onValueChange = { hTitle = it },
                label = stringResource(R.string.label_title),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            )
            TextField(
                value = hText,
                onValueChange = { hText = it },
                label = stringResource(R.string.label_text),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            )
            TextField(
                value = hSubText,
                onValueChange = { hSubText = it },
                label = stringResource(R.string.label_subtext),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            )
            TextField(
                value = hLeftText,
                onValueChange = { hLeftText = it },
                label = stringResource(R.string.label_left_text),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            )
            TextField(
                value = hMainText,
                onValueChange = { hMainText = it },
                label = stringResource(R.string.label_main_text),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        item {
            SmallTitle(stringResource(R.string.section_raw_json))
            TextField(
                value = rawJson,
                onValueChange = { rawJson = it },
                label = stringResource(R.string.label_custom_json),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).height(120.dp)
            )
        }

        item {
            SmallTitle(stringResource(R.string.section_icon))
            Card(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth()) {
                RadioButtonPreference(
                    selected = selectedIcon == 0,
                    onClick = { selectedIcon = 0 },
                    title = stringResource(R.string.icon_timer)
                )
                RadioButtonPreference(
                    selected = selectedIcon == 1,
                    onClick = { selectedIcon = 1 },
                    title = stringResource(R.string.icon_call)
                )
                RadioButtonPreference(
                    selected = selectedIcon == 2,
                    onClick = { selectedIcon = 2 },
                    title = stringResource(R.string.icon_alert)
                )
                RadioButtonPreference(
                    selected = selectedIcon == 3,
                    onClick = { selectedIcon = 3 },
                    title = stringResource(R.string.icon_default)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        postHyperNotification(context, hTitle, hText, hSubText, hLeftText, hMainText, rawJson, getIconRes(selectedIcon))
                    },
                    modifier = Modifier.weight(1f)
                ) { Text(stringResource(R.string.btn_post_hyper)) }
                Button(
                    onClick = { stopService(context) },
                    modifier = Modifier.weight(1f)
                ) { Text(stringResource(R.string.btn_clear_all)) }
            }
        }
    }
}

@Composable
fun OriginIslandScreen(paddingValues: PaddingValues, scrollBehavior: ScrollBehavior) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("Origin Island") }
    var text by remember { mutableStateOf("Placeholder...") }
    var subtext by remember { mutableStateOf("") }
    var statusChipText by remember { mutableStateOf("50%") }
    var isPromoted by remember { mutableStateOf(true) }
    var isOngoing by remember { mutableStateOf(true) }
    var showProgress by remember { mutableStateOf(true) }
    var useChrono by remember { mutableStateOf(false) }
    var selectedIcon by remember { mutableIntStateOf(0) }

    val notifications = remember { mutableStateListOf<NotificationInfo>() }
    var lastId by remember { mutableIntStateOf(3000) }
    var editingId by remember { mutableStateOf<Int?>(null) }

    LazyColumn(
        contentPadding = paddingValues,
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .scrollEndHaptic()
    ) {
        item {
            SmallTitle(stringResource(R.string.section_origin_info))
            TextField(
                value = title,
                onValueChange = { title = it },
                label = stringResource(R.string.label_title),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            )
            TextField(
                value = text,
                onValueChange = { text = it },
                label = stringResource(R.string.label_text),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            )
            TextField(
                value = subtext,
                onValueChange = { subtext = it },
                label = stringResource(R.string.label_subtext),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            )
            TextField(
                value = statusChipText,
                onValueChange = { statusChipText = it },
                label = stringResource(R.string.label_chip_text),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        item {
            SmallTitle(stringResource(R.string.section_settings))
            Card(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth()) {
                CheckboxPreference(
                    checked = isOngoing,
                    onCheckedChange = { isOngoing = it },
                    title = stringResource(R.string.pref_ongoing)
                )
                CheckboxPreference(
                    checked = isPromoted,
                    onCheckedChange = { isPromoted = it },
                    title = stringResource(R.string.pref_promoted)
                )
                CheckboxPreference(
                    checked = useChrono,
                    onCheckedChange = { useChrono = it },
                    title = stringResource(R.string.pref_chrono)
                )
                CheckboxPreference(
                    checked = showProgress,
                    onCheckedChange = { showProgress = it },
                    title = stringResource(R.string.pref_show_progress)
                )
            }
        }

        item {
            SmallTitle(stringResource(R.string.section_icon))
            Card(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth()) {
                RadioButtonPreference(
                    selected = selectedIcon == 0,
                    onClick = { selectedIcon = 0 },
                    title = stringResource(R.string.icon_timer)
                )
                RadioButtonPreference(
                    selected = selectedIcon == 1,
                    onClick = { selectedIcon = 1 },
                    title = stringResource(R.string.icon_call)
                )
                RadioButtonPreference(
                    selected = selectedIcon == 2,
                    onClick = { selectedIcon = 2 },
                    title = stringResource(R.string.icon_alert)
                )
                RadioButtonPreference(
                    selected = selectedIcon == 3,
                    onClick = { selectedIcon = 3 },
                    title = stringResource(R.string.icon_default)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        editingId = null
                        val iconRes = getIconRes(selectedIcon)
                        val nId = ++lastId
                        notifications.add(NotificationInfo(nId, title, text, iconRes, isPromoted, statusChipText, showProgress))
                        postOriginNotification(context, title, text, subtext, "", "", "", iconRes)
                    },
                    modifier = Modifier.weight(1f)
                ) { Text(stringResource(R.string.btn_post)) }
                Button(
                    onClick = {
                        val updateId = editingId ?: notifications.lastOrNull()?.id
                        if (updateId != null) {
                            val iconRes = getIconRes(selectedIcon)
                            val updatedText = context.getString(R.string.text_updated, text)
                            val idx = notifications.indexOfFirst { it.id == updateId }
                            if (idx != -1) {
                                notifications[idx] = notifications[idx].copy(title = title, text = updatedText, iconRes = iconRes, isPromoted = isPromoted, statusChipText = statusChipText, showProgress = showProgress)
                            }
                            postOriginNotification(context, title, updatedText, subtext, "", "", "", iconRes)
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) { Text(stringResource(R.string.btn_update)) }
                Button(
                    onClick = {
                        stopService(context)
                        notifications.clear()
                    },
                    modifier = Modifier.weight(1f)
                ) { Text(stringResource(R.string.btn_clear_all)) }
            }
        }
    }
}

@Composable
fun RecasterScreen(paddingValues: PaddingValues, scrollBehavior: ScrollBehavior, isMiui: Boolean, isVivo: Boolean) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("experimental_prefs", Context.MODE_PRIVATE)

    var castEnabled by remember { mutableStateOf(prefs.getBoolean("cast_notifications", false)) }
    var useAppIcon by remember { mutableStateOf(prefs.getBoolean("use_app_icon", false)) }
    var showProgressPercent by remember { mutableStateOf(prefs.getBoolean("show_progress_percentage", false)) }
    var limitChipText by remember { mutableStateOf(prefs.getBoolean("limit_chip_7char", false)) }
    var castMode by remember { mutableStateOf(prefs.getString("cast_mode", "live_updates") ?: "live_updates") }

    val pm = context.packageManager
    val enabledApps = remember { mutableStateListOf<EnabledApp>() }

    fun loadEnabledApps() {
        val selectedPackages = prefs.getStringSet("cast_enabled_apps", emptySet()) ?: emptySet()
        val newList = selectedPackages.mapNotNull { pkg ->
            try {
                val appInfo = pm.getApplicationInfo(pkg, 0)
                EnabledApp(
                    name = appInfo.loadLabel(pm).toString(),
                    packageName = pkg,
                    icon = appInfo.loadIcon(pm)
                )
            } catch (_: Exception) { null }
        }.sortedBy { it.name.lowercase() }
        enabledApps.clear()
        enabledApps.addAll(newList)
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                loadEnabledApps()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) { loadEnabledApps() }

    LazyColumn(
        contentPadding = paddingValues,
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .scrollEndHaptic()
    ) {
        item {
            SmallTitle(stringResource(R.string.section_casting_settings))
            Card(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth()) {
                SwitchPreference(
                    checked = castEnabled,
                    onCheckedChange = {
                        castEnabled = it
                        prefs.edit().putBoolean("cast_notifications", it).apply()
                    },
                    title = stringResource(R.string.pref_cast_notifs),
                    summary = stringResource(R.string.pref_cast_notifs_summary)
                )
                SwitchPreference(
                    checked = useAppIcon,
                    onCheckedChange = {
                        useAppIcon = it
                        prefs.edit().putBoolean("use_app_icon", it).apply()
                    },
                    title = stringResource(R.string.pref_use_app_icon),
                    summary = stringResource(R.string.pref_use_app_icon_summary)
                )
                SwitchPreference(
                    checked = showProgressPercent,
                    onCheckedChange = {
                        showProgressPercent = it
                        prefs.edit().putBoolean("show_progress_percentage", it).apply()
                    },
                    title = stringResource(R.string.pref_show_progress_percent),
                    summary = stringResource(R.string.pref_show_progress_percent_summary)
                )
                SwitchPreference(
                    checked = limitChipText,
                    onCheckedChange = {
                        limitChipText = it
                        prefs.edit().putBoolean("limit_chip_7char", it).apply()
                    },
                    title = stringResource(R.string.pref_limit_chip),
                    summary = stringResource(R.string.pref_limit_chip_summary)
                )
            }
        }

        item {
            SmallTitle(stringResource(R.string.section_cast_as))
            Card(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth()) {
                RadioButtonPreference(
                    selected = castMode == "live_updates",
                    onClick = {
                        castMode = "live_updates"
                        prefs.edit().putString("cast_mode", "live_updates").apply()
                    },
                    title = stringResource(R.string.mode_live_updates)
                )
                if (isMiui) {
                    RadioButtonPreference(
                        selected = castMode == "hyperisland",
                        onClick = {
                            castMode = "hyperisland"
                            prefs.edit().putString("cast_mode", "hyperisland").apply()
                        },
                        title = stringResource(R.string.mode_hyperisland)
                    )
                }
                if (isVivo) {
                    RadioButtonPreference(
                        selected = castMode == "originisland",
                        onClick = {
                            castMode = "originisland"
                            prefs.edit().putString("cast_mode", "originisland").apply()
                        },
                        title = "OriginIsland"
                    )
                }
            }
        }

        if (isMiuiCN() && castMode == "hyperisland") {
            item {
                Card(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
                    colors = CardDefaults.defaultColors(
                        color = Color(0xFFFEE2E2) // Light red background
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = MiuixIcons.Settings, // Using Settings icon as fallback for warning
                                contentDescription = null,
                                tint = Color.Red,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.warning_cn_rom_title),
                                color = Color.Red,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.warning_cn_rom_msg),
                            color = Color(0xFF991B1B), // Darker red text
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        item {
            SmallTitle(stringResource(R.string.section_actions))
            Card(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth()) {
                ArrowPreference(
                    title = stringResource(R.string.action_grant_access),
                    summary = stringResource(R.string.action_grant_access_summary),
                    onClick = {
                        context.startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
                    }
                )
                ArrowPreference(
                    title = stringResource(R.string.action_select_apps),
                    summary = pluralStringResource(R.plurals.apps_selected, enabledApps.size, enabledApps.size),
                    onClick = {
                        context.startActivity(Intent(context, AppPickerActivity::class.java))
                    }
                )
            }
        }

        if (enabledApps.isNotEmpty()) {
            item { 
                SmallTitle(stringResource(R.string.section_enabled_apps, enabledApps.size)) 
                Card(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth()) {
                    enabledApps.forEach { app ->
                        BasicComponent(
                            title = app.name,
                            summary = app.packageName,
                            startAction = {
                                Image(
                                    painter = BitmapPainter(app.icon.toBitmap().asImageBitmap()),
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp)
                                )
                            },
                            onClick = {
                                val intent = Intent(context, AppConfigActivity::class.java).apply {
                                    putExtra("package_name", app.packageName)
                                }
                                context.startActivity(intent)
                            }
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun notificationSummary(notif: NotificationInfo): String {
    val kind = if (notif.isPromoted) {
        stringResource(R.string.pref_promoted).substringBefore("(").trim()
    } else {
        stringResource(R.string.notif_standard)
    }
    val base = stringResource(R.string.notif_summary, notif.id, kind)
    val chip = notif.statusChipText
    return if (chip.isNullOrEmpty()) base else stringResource(R.string.notif_summary_chip, base, chip)
}

// ── Helpers ──

private fun getIconRes(index: Int): Int = when (index) {
    0 -> R.drawable.ic_timer
    1 -> R.drawable.ic_call
    2 -> R.drawable.ic_alert
    else -> R.mipmap.ic_launcher_round
}

fun postNotification(
    context: Context, title: String, text: String, subtext: String,
    chipText: String, id: Int, iconRes: Int, isPromoted: Boolean, showProgress: Boolean
) {
    val intent = Intent(context, PlaygroundService::class.java).apply {
        action = PlaygroundService.ACTION_START
        putExtra("title", title)
        putExtra("text", text)
        putExtra("subtext", subtext)
        putExtra("status_chip_text", chipText)
        putExtra("id", id)
        putExtra("icon_res", iconRes)
        putExtra("is_promoted", isPromoted)
        putExtra("show_progress", showProgress)
        if (showProgress) {
            putExtra("progress", 50)
            putExtra("progress_max", 100)
        }
        putExtra("when", System.currentTimeMillis())
        putExtra("source_app", "Manual-Compose")
        putExtra("cast_mode", "live_updates")
    }
    if (Build.VERSION.SDK_INT >= 26) {
        context.startForegroundService(intent)
    } else {
        context.startService(intent)
    }
}

fun postHyperNotification(
    context: Context, title: String, text: String, subtext: String,
    leftText: String, mainText: String, rawJson: String, iconRes: Int
) {
    val intent = Intent(context, PlaygroundService::class.java).apply {
        action = PlaygroundService.ACTION_START
        putExtra("title", title)
        putExtra("text", text)
        putExtra("subtext", subtext)
        putExtra("hyper_left_text", leftText)
        putExtra("hyper_main_text", mainText)
        putExtra("raw_hyper_json", rawJson)
        putExtra("id", (System.currentTimeMillis() % 100000).toInt())
        putExtra("icon_res", iconRes)
        putExtra("is_promoted", true)
        putExtra("source_app", "Manual-Hyper-Compose")
        putExtra("cast_mode", "hyperisland")
    }
    if (Build.VERSION.SDK_INT >= 26) {
        context.startForegroundService(intent)
    } else {
        context.startService(intent)
    }
}

fun postOriginNotification(
    context: Context, title: String, text: String, subtext: String,
    leftText: String, mainText: String, rawJson: String, iconRes: Int
) {
    val intent = Intent(context, PlaygroundService::class.java).apply {
        action = PlaygroundService.ACTION_START
        putExtra("title", title)
        putExtra("text", text)
        putExtra("subtext", subtext)
        putExtra("origin_left_text", leftText)
        putExtra("origin_main_text", mainText)
        putExtra("raw_origin_json", rawJson)
        putExtra("id", (System.currentTimeMillis() % 100000).toInt())
        putExtra("icon_res", iconRes)
        putExtra("is_promoted", true)
        putExtra("source_app", "Manual-Origin-Compose")
        putExtra("cast_mode", "originisland")
    }
    if (Build.VERSION.SDK_INT >= 26) {
        context.startForegroundService(intent)
    } else {
        context.startService(intent)
    }
}

fun stopService(context: Context) {
    if (Build.VERSION.SDK_INT >= 26) {
        context.startForegroundService(Intent(context, PlaygroundService::class.java).apply { action = PlaygroundService.ACTION_STOP })
    } else {
        context.startService(Intent(context, PlaygroundService::class.java).apply { action = PlaygroundService.ACTION_STOP })
    }
}

fun isMiuiRegion(): Boolean {
    return try {
        val buildClass = Class.forName("android.os.SystemProperties")
        val method = buildClass.getMethod("get", String::class.java)
        val value = method.invoke(buildClass, "ro.miui.region") as String
        value.isNotEmpty()
    } catch (_: Exception) {
        false
    }
}

fun isVivoDevice(): Boolean {
    val manufacturer = Build.MANUFACTURER
    return manufacturer.equals("vivo", ignoreCase = true)
}

fun isMiuiCN(): Boolean {
    return try {
        val buildClass = Class.forName("android.os.SystemProperties")
        val method = buildClass.getMethod("get", String::class.java)
        val value = method.invoke(buildClass, "ro.miui.region") as String
        value == "CN"
    } catch (_: Exception) {
        false
    }
}
