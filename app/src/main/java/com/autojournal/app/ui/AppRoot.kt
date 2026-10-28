package com.autojournal.app.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.autojournal.app.data.Car
import com.autojournal.app.notify.Notifications
import com.autojournal.app.ui.components.CarAvatar
import com.autojournal.app.ui.screens.CarEditScreen
import com.autojournal.app.ui.screens.ContactEditScreen
import com.autojournal.app.ui.screens.ContactsScreen
import com.autojournal.app.ui.screens.FuelScreen
import com.autojournal.app.ui.screens.GarageScreen
import com.autojournal.app.ui.screens.HomeScreen
import com.autojournal.app.ui.screens.RefuelEditScreen
import com.autojournal.app.ui.screens.ReminderEditScreen
import com.autojournal.app.ui.screens.RemindersScreen
import com.autojournal.app.ui.screens.ServiceEditScreen
import com.autojournal.app.ui.screens.ServiceScreen
import com.autojournal.app.ui.screens.SettingsScreen
import com.autojournal.app.ui.screens.StatsScreen
import com.autojournal.app.ui.screens.WelcomeScreen
import com.autojournal.app.util.Fmt
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.dialog
import com.autojournal.app.MainActivity
import com.autojournal.app.ui.components.appBackground
import com.autojournal.app.ui.screens.DocumentEditScreen
import com.autojournal.app.ui.screens.DocumentsScreen
import com.autojournal.app.ui.screens.OdometerDialog
import com.autojournal.app.ui.screens.ShoppingScreen
import com.autojournal.app.ui.screens.TireEditScreen
import com.autojournal.app.ui.screens.TiresScreen
import com.autojournal.app.ui.theme.LocalExtras

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("home", "Главная", Icons.Filled.Home),
    Tab("fuel", "Заправки", Icons.Filled.LocalGasStation),
    Tab("service", "Сервис", Icons.Filled.Build),
    Tab("reminders", "Замены", Icons.Filled.NotificationsActive),
    Tab("contacts", "Контакты", Icons.Filled.Contacts),
)

/** Маршруты для навигации. id = 0 — новая запись. */
object Routes {
    fun car(id: Long) = "car/$id"
    fun refuel(id: Long) = "refuel/$id"
    fun service(id: Long, category: String? = null) =
        "service/$id" + if (category != null) "?cat=" + android.net.Uri.encode(category) else ""
    fun reminder(id: Long) = "reminder/$id"
    fun contact(id: Long) = "contact/$id"
}

@Composable
fun AppRoot(command: String?, onCommandHandled: () -> Unit, vm: MainViewModel = viewModel()) {
    val cars by vm.cars.collectAsStateWithLifecycle()
    val ctx = LocalContext.current

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 && !Notifications.canNotify(ctx)) {
            permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val list = cars
    when {
        list == null -> Box(Modifier.fillMaxSize())
        list.isEmpty() -> WelcomeFlow(vm)
        else -> MainNav(vm, list, command, onCommandHandled)
    }
}

@Composable
private fun WelcomeFlow(vm: MainViewModel) {
    var adding by remember { mutableStateOf(false) }
    if (adding) {
        CarEditScreen(vm = vm, id = 0L, onBack = { adding = false })
    } else {
        WelcomeScreen(vm = vm, onAddCar = { adding = true })
    }
}

@Composable
private fun MainNav(vm: MainViewModel, cars: List<Car>, command: String?, onCommandHandled: () -> Unit) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val isTab = tabs.any { it.route == route }
    val active by vm.activeCar.collectAsStateWithLifecycle()
    val km by vm.currentKm.collectAsStateWithLifecycle()

    LaunchedEffect(command) {
        when (command) {
            null, "home" -> {}
            MainActivity.CMD_REFUEL -> nav.navigate(Routes.refuel(0))
            MainActivity.CMD_SERVICE -> nav.navigate(Routes.service(0))
            MainActivity.CMD_ODOMETER -> nav.navigate("odometer")
            "documents" -> nav.navigate("documents") { launchSingleTop = true }
            else -> if (tabs.any { it.route == command }) nav.navigate(command) { launchSingleTop = true }
        }
        if (command != null) onCommandHandled()
    }

    val extras = LocalExtras.current
    Scaffold(
        modifier = Modifier.appBackground(extras.backgroundBrush, MaterialTheme.colorScheme.background),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { if (isTab) MainTopBar(vm, nav, cars, active, km) },
        bottomBar = {
            if (isTab) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(
                        alpha = if (LocalExtras.current.glow) 0.85f else 1f
                    ),
                ) {
                    tabs.forEach { t ->
                        NavigationBarItem(
                            selected = route == t.route,
                            onClick = {
                                nav.navigate(t.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(t.icon, contentDescription = t.label) },
                            label = { Text(t.label, maxLines = 1) },
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            when (route) {
                "fuel" -> Fab("Заправка") { nav.navigate(Routes.refuel(0)) }
                "service" -> Fab("Запись") { nav.navigate(Routes.service(0)) }
                "reminders" -> Fab("Напоминание") { nav.navigate(Routes.reminder(0)) }
                "contacts" -> Fab("Контакт") { nav.navigate(Routes.contact(0)) }
                else -> {}
            }
        },
    ) { pad ->
        NavHost(nav, startDestination = "home", modifier = Modifier.padding(pad)) {
            composable("home") { HomeScreen(vm, nav) }
            composable("fuel") { FuelScreen(vm, nav) }
            composable("service") { ServiceScreen(vm, nav) }
            composable("reminders") { RemindersScreen(vm, nav) }
            composable("contacts") { ContactsScreen(vm, nav) }
            composable("stats") { StatsScreen(vm, onBack = { nav.popBackStack() }) }
            composable("garage") { GarageScreen(vm, nav) }
            composable("settings") { SettingsScreen(vm, onBack = { nav.popBackStack() }) }
            composable("car/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
                CarEditScreen(vm, e.arguments?.getLong("id") ?: 0L, onBack = { nav.popBackStack() })
            }
            composable("refuel/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
                RefuelEditScreen(vm, e.arguments?.getLong("id") ?: 0L, onBack = { nav.popBackStack() })
            }
            composable(
                "service/{id}?cat={cat}",
                arguments = listOf(
                    navArgument("id") { type = NavType.LongType },
                    navArgument("cat") { type = NavType.StringType; nullable = true; defaultValue = null },
                ),
            ) { e ->
                ServiceEditScreen(
                    vm, e.arguments?.getLong("id") ?: 0L, e.arguments?.getString("cat"),
                    onBack = { nav.popBackStack() },
                )
            }
            composable("reminder/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
                ReminderEditScreen(vm, e.arguments?.getLong("id") ?: 0L, onBack = { nav.popBackStack() })
            }
            composable("contact/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
                ContactEditScreen(vm, nav, e.arguments?.getLong("id") ?: 0L, onBack = { nav.popBackStack() })
            }
            composable("documents") { DocumentsScreen(vm, nav) }
            composable("document/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
                DocumentEditScreen(vm, e.arguments?.getLong("id") ?: 0L, onBack = { nav.popBackStack() })
            }
            composable("tires") { TiresScreen(vm, nav) }
            composable("tire/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
                TireEditScreen(vm, e.arguments?.getLong("id") ?: 0L, onBack = { nav.popBackStack() })
            }
            composable("shopping") { ShoppingScreen(vm, nav) }
            dialog("odometer") {
                val km by vm.currentKm.collectAsStateWithLifecycle()
                val car by vm.activeCar.collectAsStateWithLifecycle()
                OdometerDialog(
                    current = km,
                    onDismiss = { nav.popBackStack() },
                    onSave = { v ->
                        car?.let { c -> vm.io { addOdometer(c.id, v) } }
                        nav.popBackStack()
                    },
                )
            }
        }
    }
}

@Composable
private fun Fab(text: String, onClick: () -> Unit) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        icon = { Icon(Icons.Filled.Add, contentDescription = null) },
        text = { Text(text) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainTopBar(
    vm: MainViewModel,
    nav: NavHostController,
    cars: List<Car>,
    active: Car?,
    km: Int,
) {
    var menu by remember { mutableStateOf(false) }
    TopAppBar(
        title = {
            Box {
                Row(
                    Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { menu = true }
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CarAvatar(active, 38.dp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f, fill = false)) {
                        Text(
                            active?.name ?: "", style = MaterialTheme.typography.titleMedium,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            Fmt.km(km), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = "Сменить авто")
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    cars.forEach { c ->
                        DropdownMenuItem(
                            text = { Text(c.name) },
                            leadingIcon = { CarAvatar(c, 30.dp) },
                            trailingIcon = if (c.id == active?.id) {
                                { Icon(Icons.Filled.Check, contentDescription = null) }
                            } else null,
                            onClick = { vm.repo.setActiveCar(c.id); menu = false },
                        )
                    }
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Гараж") },
                        leadingIcon = { Icon(Icons.Filled.DirectionsCar, contentDescription = null) },
                        onClick = { menu = false; nav.navigate("garage") },
                    )
                    DropdownMenuItem(
                        text = { Text("Добавить автомобиль") },
                        leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null) },
                        onClick = { menu = false; nav.navigate(Routes.car(0)) },
                    )
                }
            }
        },
        actions = {
            IconButton(onClick = { nav.navigate("stats") }) {
                Icon(Icons.Filled.BarChart, contentDescription = "Статистика")
            }
            IconButton(onClick = { nav.navigate("settings") }) {
                Icon(Icons.Filled.Settings, contentDescription = "Настройки")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
    )
}
