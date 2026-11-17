package com.cui.vocab.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

private enum class Tab(val title: String, val subtitle: String) {
    ADD("添加单词", "生词 + 释义，存进本机词库"),
    LIST("词库", "按 a→z 排列 · 搜索 / 编辑 / 删除"),
    QUIZ("单词检测", "随机抽词，看看记住了多少"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(vm: MainViewModel = viewModel()) {
    var tab by rememberSaveable { mutableStateOf(Tab.ADD) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(vm.message) {
        val msg = vm.message
        if (msg != null) {
            snackbar.showSnackbar(msg)
            vm.message = null
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = tab.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = tab.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
            ) {
                NavigationBarItem(
                    selected = tab == Tab.ADD,
                    onClick = { tab = Tab.ADD },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    label = { Text("添加") },
                    colors = navColors(),
                )
                NavigationBarItem(
                    selected = tab == Tab.LIST,
                    onClick = { tab = Tab.LIST },
                    icon = { Icon(Icons.Filled.List, contentDescription = null) },
                    label = { Text("词库") },
                    colors = navColors(),
                )
                NavigationBarItem(
                    selected = tab == Tab.QUIZ,
                    onClick = { tab = Tab.QUIZ },
                    icon = { Icon(Icons.Filled.PlayArrow, contentDescription = null) },
                    label = { Text("检测") },
                    colors = navColors(),
                )
            }
        },
    ) { inner ->
        Box(Modifier.fillMaxSize().padding(inner)) {
            when (tab) {
                Tab.ADD -> AddScreen(vm)
                Tab.LIST -> WordListScreen(vm)
                Tab.QUIZ -> QuizScreen(vm)
            }
        }
    }

    vm.editing?.let { word ->
        EditWordDialog(
            word = word,
            onDismiss = { vm.dismissEdit() },
            onSave = { w, t -> vm.saveEdit(word.id, w, t) },
            onDelete = { vm.deleteWord(word) },
        )
    }
}

@Composable
private fun navColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
    selectedTextColor = MaterialTheme.colorScheme.primary,
    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
)
