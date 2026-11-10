package pl.sportfamily.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import pl.sportfamily.app.ui.theme.Sf
import pl.sportfamily.app.ui.theme.ThemeMode
import pl.sportfamily.app.ui.theme.ThemeSettings
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import pl.sportfamily.app.data.Child
import pl.sportfamily.app.ui.MainViewModel
import pl.sportfamily.app.ui.calendar.CalendarScreen
import pl.sportfamily.app.ui.children.ChildrenScreen
import pl.sportfamily.app.ui.event.EditEventScreen
import pl.sportfamily.app.ui.event.EventDetailScreen
import pl.sportfamily.app.ui.more.MoreScreen
import pl.sportfamily.app.ui.tasks.TasksScreen
import pl.sportfamily.app.ui.theme.ChildPalette
import pl.sportfamily.app.ui.theme.SportFamilyTheme
import pl.sportfamily.app.ui.today.TodayScreen
import pl.sportfamily.app.update.UpdateBanner
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeSettings.load(this)
        enableEdgeToEdge()
        setContent {
            val mode by ThemeSettings.mode.collectAsState()
            val dark = when (mode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            // Jasne lub ciemne ikony paska stanu zgodnie z wybranym motywem, nie z systemem.
            DisposableEffect(dark) {
                val style = if (dark) SystemBarStyle.dark(Color.TRANSPARENT)
                else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose { }
            }
            SportFamilyTheme(dark = dark) { App() }
        }
    }
}

private data class Tab(val route: String, val label: String, val icon: ImageVector, val iconOn: ImageVector)

private val Tabs = listOf(
    Tab("today", "Dzisiaj", Icons.Outlined.Today, Icons.Rounded.Today),
    Tab("calendar", "Kalendarz", Icons.Outlined.CalendarMonth, Icons.Rounded.CalendarMonth),
    Tab("children", "Dzieci", Icons.Outlined.Groups, Icons.Rounded.Groups),
    Tab("tasks", "Zadania", Icons.Outlined.TaskAlt, Icons.Rounded.TaskAlt),
    Tab("more", "Więcej", Icons.Rounded.MoreHoriz, Icons.Rounded.MoreHoriz),
)

@Composable
private fun App(vm: MainViewModel = viewModel()) {
    val nav = rememberNavController()
    val data by vm.data.collectAsState()
    val now by vm.now.collectAsState()
    val today = now.toLocalDate()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val onTab = Tabs.any { it.route == route }

    var calendarDay by rememberSaveable { mutableStateOf(LocalDate.now()) }
    var childRequest by rememberSaveable { mutableStateOf(false) }
    var taskRequest by rememberSaveable { mutableStateOf(false) }

    fun go(r: String) = nav.navigate(r) {
        popUpTo("today") { saveState = true }
        launchSingleTop = true
        restoreState = true
    }

    val d = data
    if (d == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            // Tło pod paskiem stanu, żeby przewijana treść nie wchodziła pod godzinę i ikony.
            Column(Modifier.background(MaterialTheme.colorScheme.background)) {
                Spacer(Modifier.fillMaxWidth().windowInsetsTopHeight(WindowInsets.statusBars))
                if (onTab) UpdateBanner()
            }
        },
        bottomBar = {
            if (onTab) NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                Tabs.forEach { t ->
                    val sel = route == t.route
                    NavigationBarItem(
                        selected = sel,
                        onClick = { go(t.route) },
                        icon = { Icon(if (sel) t.iconOn else t.icon, null) },
                        label = { Text(t.label) },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onSurface,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                }
            }
        },
        floatingActionButton = {
            when (route) {
                "today", "calendar" -> ExtendedFloatingActionButton(
                    onClick = { nav.navigate("edit?id=-1&day=${(if (route == "calendar") calendarDay else today).toEpochDay()}") },
                    icon = { Icon(Icons.Rounded.Add, null) }, text = { Text("Dodaj") },
                    containerColor = Sf.colors.accent, contentColor = Sf.colors.onAccent,
                )
                "children" -> if (d.children.isNotEmpty()) ExtendedFloatingActionButton(
                    onClick = { childRequest = true }, icon = { Icon(Icons.Rounded.Add, null) }, text = { Text("Dziecko") },
                    containerColor = Sf.colors.accent, contentColor = Sf.colors.onAccent,
                )
                "tasks" -> ExtendedFloatingActionButton(
                    onClick = { taskRequest = true }, icon = { Icon(Icons.Rounded.Add, null) }, text = { Text("Zadanie") },
                    containerColor = Sf.colors.accent, contentColor = Sf.colors.onAccent,
                )
            }
        },
    ) { pad ->
        NavHost(
            nav, startDestination = "today",
            enterTransition = { fadeIn() }, exitTransition = { fadeOut() },
        ) {
            composable("today") {
                TodayScreen(
                    data = d, now = now, contentPadding = pad,
                    onOpen = { o -> nav.navigate("event/${o.event.id}/${o.date.toEpochDay()}") },
                    onToggleCheck = { id, day -> vm.toggleCheck(id, day) },
                    onToggleTask = vm::toggleTask,
                    onSeedDemo = vm::seedDemo,
                    onAddChild = { go("children"); childRequest = true },
                    onOpenCalendar = { go("calendar") },
                    onOpenTasks = { go("tasks") },
                )
            }
            composable("calendar") {
                CalendarScreen(
                    data = d, today = today, contentPadding = pad,
                    selected = calendarDay, onSelect = { calendarDay = it },
                    onOpen = { o -> nav.navigate("event/${o.event.id}/${o.date.toEpochDay()}") },
                )
            }
            composable("children") {
                ChildrenScreen(
                    data = d, now = now, contentPadding = pad,
                    editRequest = if (childRequest) Child(name = "", colorArgb = ChildPalette[d.children.size % ChildPalette.size]) else null,
                    onEditRequestHandled = { childRequest = false },
                    onSave = vm::saveChild, onDelete = vm::deleteChild,
                )
            }
            composable("tasks") {
                TasksScreen(
                    data = d, today = today, contentPadding = pad,
                    addRequest = taskRequest, onAddRequestHandled = { taskRequest = false },
                    onToggle = vm::toggleTask, onSave = vm::saveTask, onDelete = vm::deleteTask,
                )
            }
            composable("more") {
                MoreScreen(d, pad, onSavePerson = vm::savePerson, onDeletePerson = vm::deletePerson, onSeedDemo = vm::seedDemo)
            }
            composable(
                "event/{id}/{day}",
                arguments = listOf(navArgument("id") { type = NavType.LongType }, navArgument("day") { type = NavType.LongType }),
                enterTransition = { slideInHorizontally { it / 4 } + fadeIn() },
                popExitTransition = { slideOutHorizontally { it / 4 } + fadeOut() },
            ) { entry ->
                val id = entry.arguments!!.getLong("id")
                val day = LocalDate.ofEpochDay(entry.arguments!!.getLong("day"))
                EventDetailScreen(
                    data = d, eventId = id, day = day,
                    onBack = { nav.popBackStack() },
                    onEdit = { nav.navigate("edit?id=$id&day=${day.toEpochDay()}") },
                    onToggleCheck = { item, dd -> vm.toggleCheck(item, dd) },
                    onCancelOccurrence = { vm.cancelOccurrence(id, day); nav.popBackStack() },
                    onDelete = { vm.deleteEvent(id); nav.popBackStack() },
                )
            }
            composable(
                "edit?id={id}&day={day}",
                arguments = listOf(
                    navArgument("id") { type = NavType.LongType; defaultValue = -1L },
                    navArgument("day") { type = NavType.LongType; defaultValue = LocalDate.now().toEpochDay() },
                ),
            ) { entry ->
                val id = entry.arguments!!.getLong("id").takeIf { it > 0 }
                val day = LocalDate.ofEpochDay(entry.arguments!!.getLong("day"))
                EditEventScreen(
                    data = d, eventId = id, initialDay = day,
                    load = vm::loadEvent,
                    onSave = { e, kids, list -> vm.saveEvent(e, kids, list); nav.popBackStack() },
                    onClose = { nav.popBackStack() },
                )
            }
        }
    }
}
