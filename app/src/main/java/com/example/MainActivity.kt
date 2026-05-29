package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.TimeFlowTab
import com.example.ui.TimeFlowViewModel
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: TimeFlowViewModel = viewModel()
            val profileState by viewModel.profile.collectAsStateWithLifecycle()
            val isDarkTheme = profileState?.isDarkTheme ?: isSystemInDarkTheme()

            MyApplicationTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val isAppReady by viewModel.isUserLoggedIn.collectAsStateWithLifecycle()
                    val onboardingCompleted by viewModel.isOnboardingCompleted.collectAsStateWithLifecycle()
                    val context = LocalContext.current

                    // Launch Toast messages for Repository events
                    LaunchedEffect(Unit) {
                        viewModel.uiEventMessage.collect { message ->
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        }
                    }

                    AnimatedContent(
                        targetState = Pair(onboardingCompleted, isAppReady),
                        transitionSpec = {
                            fadeIn() togetherWith fadeOut()
                        },
                        label = "MainAppTransition"
                    ) { (onboarded, loggedIn) ->
                        if (!onboarded) {
                            OnboardingScreen(onFinished = { viewModel.completeOnboarding() })
                        } else if (!loggedIn) {
                            AuthScreen(onLoginClick = { viewModel.loginWithGoogle() })
                        } else {
                            MainContainer(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AuthScreen(onLoginClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        Color(0xFF1E3A8A),
                        Color(0xFF0F172A)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(32.dp)
                .fillMaxWidth()
        ) {
            // Elegant High-Resolution Image Logo
            Image(
                painter = painterResource(id = R.drawable.timeflow_logo_1780081792143),
                contentDescription = "TimeFlow Logo",
                modifier = Modifier
                    .size(140.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .border(2.dp, Color(0x3360A5FA), RoundedCornerShape(24.dp))
                    .shadow(12.dp, RoundedCornerShape(24.dp))
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "TimeFlow",
                fontSize = 38.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )

            Text(
                text = "Smart Time Management App",
                fontSize = 15.sp,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Modern Google Sign-In Card Button
            Card(
                onClick = onLoginClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .shadow(16.dp, RoundedCornerShape(28.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(28.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Google icon symbol simulator
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEA4335)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "G",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Text(
                        text = "Continue with Google",
                        color = Color(0xFF1E293B),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "No password required • Quick 1-tap Google OAuth Sign In",
                fontSize = 11.sp,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun MainContainer(viewModel: TimeFlowViewModel) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val isPrivacyVisible by viewModel.isPrivacyPolicyVisible.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (!isPrivacyVisible) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    val tabsList = listOf(
                        Triple(TimeFlowTab.TODAY, "Today", Icons.Default.Today),
                        Triple(TimeFlowTab.HABITS, "Habits", Icons.Default.AllInclusive),
                        Triple(TimeFlowTab.FOCUS, "Focus", Icons.Default.Timer),
                        Triple(TimeFlowTab.ANALYTICS, "Reports", Icons.Default.BarChart),
                        Triple(TimeFlowTab.PROFILE, "Profile", Icons.Default.Person)
                    )

                    tabsList.forEach { (tab, label, icon) ->
                        NavigationBarItem(
                            selected = currentTab == tab,
                            onClick = { viewModel.selectTab(tab) },
                            icon = { Icon(imageVector = icon, contentDescription = label) },
                            label = { Text(text = label, fontWeight = FontWeight.Medium) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Screen switching with AnimatedContent transition
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "TabTransition"
            ) { tab ->
                when (tab) {
                    TimeFlowTab.TODAY -> TodayDashboardTab(viewModel = viewModel)
                    TimeFlowTab.HABITS -> HabitsTab(viewModel = viewModel)
                    TimeFlowTab.FOCUS -> FocusTab(viewModel = viewModel)
                    TimeFlowTab.ANALYTICS -> AnalyticsTab(viewModel = viewModel)
                    TimeFlowTab.PROFILE -> ProfileTab(viewModel = viewModel)
                }
            }

            // Dedicated Privacy Policy Page overlays fullscreen
            AnimatedVisibility(
                visible = isPrivacyVisible,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                PrivacyPolicyPage(onBackClick = { viewModel.setPrivacyPolicyVisible(false) })
            }
        }
    }
}

// ----------------- TODAY DASHBOARD -----------------
@Suppress("SimpleDateFormat")
@Composable
fun TodayDashboardTab(viewModel: TimeFlowViewModel) {
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val aiPlanText by viewModel.aiPlanText.collectAsStateWithLifecycle()
    val isAiGenerating by viewModel.isAiGenerating.collectAsStateWithLifecycle()
    val profile by viewModel.profile.collectAsStateWithLifecycle()

    var showAddTaskDialog by remember { mutableStateOf(false) }

    val todayDateFormatted = remember {
        val sdf = SimpleDateFormat("EEEE, MMM dd, yyyy", Locale.getDefault())
        sdf.format(Date())
    }

    // Projects list available for filtering or tagging
    val projects = listOf("All", "Inbox", "Work", "Personal", "Study")
    var selectedProjectFilter by remember { mutableStateOf("All") }

    val filteredTasks = remember(tasks, selectedProjectFilter) {
        if (selectedProjectFilter == "All") tasks else tasks.filter { it.project == selectedProjectFilter }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Card
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        val displayName = if (profile?.username.orEmpty().isBlank()) "Set Your Name" else profile?.username.orEmpty()
                        Text(
                            text = "Hello, $displayName 👋",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = todayDateFormatted,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }

                    // Calendar Sync Action Trigger
                    IconButton(
                        onClick = { viewModel.triggerGoogleCalendarSync() },
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                RoundedCornerShape(12.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Sync Google Calendar",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // AI Daily Planner glowing banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSystemInDarkTheme()) Color(0xFF1E3A8A) else Color(0xFFE3F2FD)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI Planner Logo",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "TimeFlow AI Daily Planner",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (aiPlanText == null) {
                            Text(
                                text = "Get an intelligent, AI-prioritized daily schedule generated instantly from your tasks.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { viewModel.generateAiPlan() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isAiGenerating,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                if (isAiGenerating) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text("Analyze & Plan My Day")
                                }
                            }
                        } else {
                            Text(
                                text = aiPlanText!!,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { viewModel.generateAiPlan() },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), contentColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Text("Regenerate")
                                }
                                Button(
                                    onClick = { viewModel.clearAiPlan() },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text("Dismiss")
                                }
                            }
                        }
                    }
                }
            }

            // Quick Filter Category Bar
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(projects) { proj ->
                        val isSelected = selectedProjectFilter == proj
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedProjectFilter = proj },
                            label = { Text(proj) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            // Tasks List Label
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tasks (${filteredTasks.size})",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            // Empty state helper
            if (filteredTasks.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Empty Tasks",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No pending tasks found",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                    }
                }
            } else {
                items(filteredTasks, key = { it.id }) { task ->
                    TaskRow(task = task, onCheckedChange = {
                        viewModel.toggleTaskComplete(task)
                    }, onDeleteClick = {
                        viewModel.deleteTask(task)
                    })
                }
            }

            // Extra padding at bottom to clear FAB
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // Add Task Floating Action Button (FAB)
        FloatingActionButton(
            onClick = { showAddTaskDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Task")
        }
    }

    if (showAddTaskDialog) {
        AddTaskDialog(
            projects = projects.filter { it != "All" },
            onDismiss = { showAddTaskDialog = false },
            onSave = { title, priority, notes, project ->
                viewModel.addTask(
                    title = title,
                    dueDate = System.currentTimeMillis(),
                    priority = priority,
                    notes = notes,
                    project = project
                )
                showAddTaskDialog = false
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskRow(task: Task, onCheckedChange: () -> Unit, onDeleteClick: () -> Unit) {
    val strikeThrough = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
    val opacity = if (task.isCompleted) 0.5f else 1.0f

    val priorityColor = when (task.priority) {
        "High" -> Color(0xFFEF4444)
        "Medium" -> Color(0xFFF59E0B)
        else -> Color(0xFF10B981)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onLongClick = onDeleteClick,
                onClick = onCheckedChange
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            if (task.isCompleted) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Priority vertical indicator bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(36.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(priorityColor)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = strikeThrough,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = opacity)
                )

                if (task.notes.isNotBlank()) {
                    Text(
                        text = task.notes,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = opacity * 0.6f),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Text(
                    text = task.project,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = opacity),
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = opacity * 0.3f),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            IconButton(onClick = onCheckedChange) {
                Icon(
                    imageVector = if (task.isCompleted) Icons.Default.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                    contentDescription = "Toggle Complete",
                    tint = if (task.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(
    projects: List<String>,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("Medium") }
    var selectedProject by remember { mutableStateOf("Inbox") }

    val priorityList = listOf("High", "Medium", "Low")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Add Today's Task",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Priority Selection row
                Text("Priority Level:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    priorityList.forEach { prio ->
                        val isSelected = priority == prio
                        val color = when (prio) {
                            "High" -> Color(0xFFEF4444)
                            "Medium" -> Color(0xFFF59E0B)
                            else -> Color(0xFF10B981)
                        }

                        ElevatedCard(
                            onClick = { priority = prio },
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = if (isSelected) color else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = prio,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Project Selector Row
                Text("Project List Tag:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(projects) { proj ->
                        val isSelected = selectedProject == proj
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedProject = proj },
                            label = { Text(proj) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                onSave(title, priority, notes, selectedProject)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Create")
                    }
                }
            }
        }
    }
}

// ----------------- HABITS TRACKER -----------------
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HabitsTab(viewModel: TimeFlowViewModel) {
    val habits by viewModel.habits.collectAsStateWithLifecycle()
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val isPremium = true

    var habitNameInput by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Habit Streaks Tracker",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Build deep streaks daily. Live a structured routine.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }

        // Add Habit Input Field
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = habitNameInput,
                        onValueChange = { habitNameInput = it },
                        label = { Text("What habit are you tracking?") },
                        placeholder = { Text("Read, Meditation, Code...") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = {
                            viewModel.addHabit(habitNameInput)
                            habitNameInput = ""
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add Habit")
                    }
                }
            }
        }



        if (habits.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No habits configured yet", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                }
            }
        } else {
            items(habits, key = { it.id }) { habit ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onLongClick = { viewModel.deleteHabit(habit) },
                            onClick = { viewModel.checkInHabit(habit) }
                        ),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = habit.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalFireDepartment,
                                    contentDescription = "streak icon",
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Current Streak: ${habit.streak} days 🔥",
                                    fontSize = 12.sp,
                                    color = Color(0xFFEF4444),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                        }

                        // Check-in action
                        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                        val alreadyCheckedIn = habit.historyDates.split(",").contains(todayDate)

                        Button(
                            onClick = { viewModel.checkInHabit(habit) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (alreadyCheckedIn) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(if (alreadyCheckedIn) "Checked In ✅" else "Check In")
                        }
                    }
                }
            }
        }
    }
}

// ----------------- POMODORO FOCUS TIMER -----------------
@Composable
fun FocusTab(viewModel: TimeFlowViewModel) {
    val mins by viewModel.timerMinutesLeft.collectAsStateWithLifecycle()
    val secs by viewModel.timerSecondsLeft.collectAsStateWithLifecycle()
    val isRunning by viewModel.isTimerRunning.collectAsStateWithLifecycle()
    val totalMins by viewModel.timerTotalDurationMinutes.collectAsStateWithLifecycle()
    val label by viewModel.timerLabel.collectAsStateWithLifecycle()
    val category by viewModel.timerCategory.collectAsStateWithLifecycle()
    val ambient by viewModel.ambientSound.collectAsStateWithLifecycle()

    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val isPremium = true

    var showTimerConfigDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Pomodoro Timer",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Giant Circular Progress Timer Node
        Box(
            modifier = Modifier.size(240.dp),
            contentAlignment = Alignment.Center
        ) {
            // Calculate progress percentage
            val totalSeconds = totalMins * 60f
            val currentSecondsLeft = (mins * 60) + secs
            val rawProgress = if (totalSeconds > 0) currentSecondsLeft / totalSeconds else 1f
            val progress by animateFloatAsState(targetValue = rawProgress, label = "TimerProgress")

            val primaryColor = MaterialTheme.colorScheme.primary
            val outlineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)

            Canvas(modifier = Modifier.fillMaxSize()) {
                // Background Track grey ring
                drawCircle(
                    color = outlineColor,
                    style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                )
                // Foreground Progress blue arc
                drawArc(
                    color = primaryColor,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = String.format("%02d:%02d", mins, secs),
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = label,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
                Text(
                    text = "Category: $category",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ElevatedCard(
                onClick = {
                    if (isRunning) viewModel.pauseTimer() else viewModel.startTimerJob()
                },
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Row(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = "Timer toggle", tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isRunning) "Pause" else "Focus Now", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            IconButton(
                onClick = { viewModel.resetTimer() },
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), CircleShape)
                    .size(48.dp)
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reset Timer", tint = MaterialTheme.colorScheme.onBackground)
            }
        }

        // Configuration card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Focus Config", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    IconButton(onClick = { showTimerConfigDialog = true }) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.primary)
                    }
                }

                Text(
                    text = "Standard intervals: 25 minutes pomodoro, 5 minutes break.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )


            }
        }

        // White Noise Ambient Selector
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Ambient Sound Space", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(
                    text = "Play relaxing white noise or rain environment sounds in focus session.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(10.dp))

                val ambientSounds = listOf("None", "White Noise", "Rain", "Forest")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    items(ambientSounds) { sound ->
                        val isSelected = ambient == sound
                        ElevatedFilterChip(
                            selected = isSelected,
                            onClick = { viewModel.changeAmbientSound(sound) },
                            label = { Text(sound) }
                        )
                    }
                }
            }
        }
    }

    if (showTimerConfigDialog) {
        TimerConfigDialog(
            isPremium = isPremium,
            currentMinutes = totalMins,
            currentLabel = label,
            currentCategory = category,
            onDismiss = { showTimerConfigDialog = false },
            onSave = { mins, lbl, cat ->
                viewModel.setTimerConfig(mins, lbl, cat)
                showTimerConfigDialog = false
            }
        )
    }
}

@Composable
fun TimerConfigDialog(
    isPremium: Boolean,
    currentMinutes: Int,
    currentLabel: String,
    currentCategory: String,
    onDismiss: () -> Unit,
    onSave: (Int, String, String) -> Unit
) {
    var minutes by remember { mutableFloatStateOf(currentMinutes.toFloat()) }
    var label by remember { mutableStateOf(currentLabel) }
    var category by remember { mutableStateOf(currentCategory) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Timer Configurations", fontWeight = FontWeight.Bold, fontSize = 18.sp)

                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Activity Label") },
                    singleLine = true
                )

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category (e.g., Work, Study)") },
                    singleLine = true
                )

                // Slider handles custom times - completely free for everyone
                Text("Duration: ${minutes.toInt()} minutes", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Slider(
                    value = minutes,
                    onValueChange = { minutes = it },
                    valueRange = 10f..60f,
                    steps = 10
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Close") }
                    Button(
                        onClick = {
                            val finalMins = minutes.toInt()
                            onSave(finalMins, label, category)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Apply Settings")
                    }
                }
            }
        }
    }
}

// ----------------- ANALYTICS & REPORTS -----------------
@Composable
fun AnalyticsTab(viewModel: TimeFlowViewModel) {
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val habits by viewModel.habits.collectAsStateWithLifecycle()

    val totalHoursFocus = remember(sessions) {
        val totalMins = sessions.sumOf { it.durationMinutes }
        String.format("%.1f", totalMins / 60f)
    }

    val chartBarsData = remember(sessions) {
        val groups = sessions.groupBy { it.category }
        groups.map { (cat, list) ->
            Pair(cat, list.sumOf { it.durationMinutes })
        }.sortedByDescending { it.second }.take(4)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Productivity Reports",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Log and review time parameters spent across items.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }

        // Summary Metric Highlight Widgets
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Tracked Hours", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Text("$totalHoursFocus hrs", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Active Habits", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
                        Text("${habits.size} tracked", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Focus analytics custom graphical bar chart
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Time Block Allocation (Minutes)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    if (chartBarsData.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No focus logs captured yet", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                    } else {
                        // Custom Drawn Canvas bar graphs
                        chartBarsData.forEach { (cat, mins) ->
                            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(cat, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("$mins mins", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .background(Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(fraction = (mins.toFloat() / 200f).coerceAtMost(1f))
                                            .height(8.dp)
                                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Focus History List Logs
        item {
            Text("Focus Session Audit History", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        if (sessions.isEmpty()) {
            item {
                Text("No sessions found in system DB", fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
            }
        } else {
            items(sessions) { log ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(log.activityName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text(
                                text = "Spent ${log.durationMinutes} minutes in Category ${log.category}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.DoneOutline,
                            contentDescription = "Done",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

// ----------------- PROFILE PAGE (REPLACES SETTINGS) -----------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileTab(viewModel: TimeFlowViewModel) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val isPremium = true
    val context = LocalContext.current

    var isEditingName by remember { mutableStateOf(false) }
    var editableNameState by remember { mutableStateOf(profile?.username.orEmpty()) }

    // Synchronize text state if profile model updates in db
    LaunchedEffect(profile) {
        if (profile != null) {
            editableNameState = profile!!.username
        }
    }

    // Avatar selections List
    val avatarIcons = listOf(
        Icons.Default.AccountCircle,
        Icons.Default.Face,
        Icons.Default.EmojiPeople,
        Icons.Default.SupportAgent,
        Icons.Default.SmartToy
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App header text
        Text(
            text = "My Profile",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Center Profile Picture
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            val idx = (profile?.avatarId ?: 1).coerceIn(0, avatarIcons.lastIndex)
            Icon(
                imageVector = avatarIcons[idx],
                contentDescription = "User avatar logo",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp)
            )
        }

        // Horizontal selections for updating avatar profile picture
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Choose Avatar Picture:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
            LazyRow(
                modifier = Modifier.padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(avatarIcons.size) { index ->
                    val isSelected = (profile?.avatarId ?: 1) == index
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
                            .clickable {
                                viewModel.updateProfile(
                                    newName = profile?.username.orEmpty(),
                                    avatarId = index
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = avatarIcons[index],
                            contentDescription = "avatar $index",
                            tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // Profile Details containing name text editor field
        if (isEditingName) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = editableNameState,
                    onValueChange = { editableNameState = it },
                    label = { Text("Display Name") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = {
                        viewModel.updateProfile(editableNameState, profile?.avatarId ?: 1)
                        isEditingName = false
                    },
                    modifier = Modifier.background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                ) {
                    Icon(imageVector = Icons.Default.Save, contentDescription = "Save Profile Name", tint = Color.White)
                }
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val displayName = if (profile?.username.orEmpty().isBlank()) "Set Your Name" else profile?.username.orEmpty()
                Text(
                    text = displayName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "dpallab224@gmail.com",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { isEditingName = true },
                    modifier = Modifier.height(36.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), contentColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Edit Profile", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Unlimited Activation Banner - 100% Free Forever
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            ),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = "Verified Status",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "🎉 All Features Unlocked – 100% Free Forever!",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "You have full, lifetime access to custom pomodoro timers, unlimited daily habits, advanced reports, and AI daily schedule planner without any restrictions or paywalls.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        // Instantly toggled Light Mode / Dark Mode Settings row
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Theme Preference", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)

                Row {
                    val isDark = profile?.isDarkTheme ?: false
                    
                    ElevatedFilterChip(
                        selected = !isDark,
                        onClick = { viewModel.toggleThemeSetting(false) },
                        label = { Text("Light Mode") }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    ElevatedFilterChip(
                        selected = isDark,
                        onClick = { viewModel.toggleThemeSetting(true) },
                        label = { Text("Dark Mode") }
                    )
                }
            }
        }

        // Privacy Policy Page direct row button links
        Card(
            onClick = { viewModel.setPrivacyPolicyVisible(true) },
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.PrivacyTip, contentDescription = "Privacy icon", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Read Privacy Policy", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
                Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Go", tint = MaterialTheme.colorScheme.outline)
            }
        }

        // Backup and Sync Mock Info
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Offline Mode & Synchronization", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(
                    text = "All your data is saved locally on device. Synced automatically via safe Google tokens encrypting databases.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // Active Sign Out Action
        Card(
            onClick = { viewModel.signOut() },
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Default.ExitToApp, contentDescription = "Exit to login", tint = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.width(10.dp))
                Text("Sign out Google Account", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
            }
        }

        // App Version label Text
        Text(
            text = "TimeFlow • Version 1.0.0",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            modifier = Modifier.padding(top = 12.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// ----------------- PRIVACY POLICY PAGE -----------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyPage(onBackClick: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy Policy", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "TimeFlow App Privacy Policy",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Last updated: May 2026",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

            Text(
                text = "1. Information We Collect",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Text(
                text = "TimeFlow leverages local databases to record tasks, habits, and focus metrics. When you log in using your Google Gmail account, we retrieve your email (dpallab224@gmail.com), profile avatar selection, and username info strictly to personalize your local layout. We do NOT harvest or share database entities.",
                fontSize = 13.sp,
                lineHeight = 20.sp
            )

            Text(
                text = "2. Offline Security",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Text(
                text = "Because TimeFlow respects the Offline-First philosophy, your day schedules are kept safe inside the SQLite sandbox. They never leak or stream onto unsecured ad servers. This ensures compliance with global privacy protection protocols.",
                fontSize = 13.sp,
                lineHeight = 20.sp
            )

            Text(
                text = "3. Google Client Integrations",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Text(
                text = "We allow users to sync details directly with Google Calendar parameters. Read data is used purely on-device to build the schedule plan.",
                fontSize = 13.sp,
                lineHeight = 20.sp
            )

            Text(
                text = "4. Developer Customization & Updates",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Text(
                text = "As a developer of TimeFlow, this policy is easily adjustable and modifiable inside the dedicated Composable functions. Change policy content blocks synchronously as features update.",
                fontSize = 13.sp,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onBackClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("I Understand & Agree")
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}
