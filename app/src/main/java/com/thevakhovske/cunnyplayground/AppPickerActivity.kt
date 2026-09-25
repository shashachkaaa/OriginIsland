package com.thevakhovske.cunnyplayground

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.core.graphics.drawable.toBitmap
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppPickerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val controller = remember { ThemeController(ColorSchemeMode.System) }
            MiuixTheme(controller = controller) {
                AppPickerScreen(onBack = { finish() })
            }
        }
    }
}

object AppCache {
    var cachedApps: List<AppInfo>? = null
}

data class AppInfo(val name: String, val packageName: String, val icon: ImageBitmap)

@Composable
fun AppPickerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val pm = context.packageManager
    val prefs = context.getSharedPreferences("experimental_prefs", Context.MODE_PRIVATE)

    var searchQuery by remember { mutableStateOf("") }
    var debouncedQuery by remember { mutableStateOf("") }

    LaunchedEffect(searchQuery) {
        kotlinx.coroutines.delay(300)
        debouncedQuery = searchQuery
    }

    val selectedApps = remember {
        mutableStateListOf<String>().apply {
            addAll(prefs.getStringSet("cast_enabled_apps", emptySet()) ?: emptySet())
        }
    }

    var allApps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(AppCache.cachedApps == null) }

    LaunchedEffect(Unit) {
        if (AppCache.cachedApps != null) {
            allApps = AppCache.cachedApps!!
        } else {
            withContext(Dispatchers.IO) {
                val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                    .map { 
                        AppInfo(
                            it.loadLabel(pm).toString(), 
                            it.packageName, 
                            it.loadIcon(pm).toBitmap().asImageBitmap()
                        ) 
                    }
                    .sortedBy { it.name.lowercase() }
                
                AppCache.cachedApps = apps
                allApps = apps
                isLoading = false
            }
        }
    }

    val displayApps = remember(debouncedQuery, allApps) {
        if (debouncedQuery.isEmpty()) allApps
        else allApps.filter {
            it.name.contains(debouncedQuery, ignoreCase = true) ||
            it.packageName.contains(debouncedQuery, ignoreCase = true)
        }
    }

    fun persistSelection() {
        prefs.edit().putStringSet("cast_enabled_apps", selectedApps.toSet()).apply()
        context.sendBroadcast(android.content.Intent("com.thevakhovske.cunnyplayground.RELOAD_NOTIFICATIONS"))
    }

    val topAppBarScrollBehavior = MiuixScrollBehavior(rememberTopAppBarState())

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = stringResource(R.string.title_select_apps),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(MiuixIcons.Back, contentDescription = stringResource(R.string.back))
                    }
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
            item {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = stringResource(R.string.label_search_apps),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            if (!isLoading) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                displayApps.forEach { app ->
                                    if (!selectedApps.contains(app.packageName)) selectedApps.add(app.packageName)
                                }
                                persistSelection()
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text(stringResource(R.string.btn_select_all)) }
                        Button(
                            onClick = {
                                selectedApps.removeAll(displayApps.map { it.packageName })
                                persistSelection()
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text(stringResource(R.string.btn_deselect_all)) }
                    }
                }
            }

            if (isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.msg_loading_apps))
                    }
                }
            } else {
                items(
                    items = displayApps,
                    key = { it.packageName }
                ) { app ->
                    val isSelected = selectedApps.contains(app.packageName)
                    Card(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth()) {
                        BasicComponent(
                            title = app.name,
                            summary = app.packageName,
                            startAction = {
                                Image(
                                    bitmap = app.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp)
                                )
                            },
                            endActions = {
                                Checkbox(
                                    state = if (isSelected) ToggleableState.On else ToggleableState.Off,
                                    onClick = {
                                        if (isSelected) {
                                            selectedApps.remove(app.packageName)
                                        } else {
                                            selectedApps.add(app.packageName)
                                        }
                                        persistSelection()
                                    }
                                )
                            },
                            onClick = {
                                if (isSelected) {
                                    selectedApps.remove(app.packageName)
                                } else {
                                    selectedApps.add(app.packageName)
                                }
                                persistSelection()
                            }
                        )
                    }
                }
            }
        }
    }
}
