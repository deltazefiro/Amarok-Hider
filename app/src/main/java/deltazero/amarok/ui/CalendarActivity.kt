package deltazero.amarok.ui

import android.os.Build
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.enableEdgeToEdge
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.daysOfWeek
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import deltazero.amarok.PrefMgr
import deltazero.amarok.R
import deltazero.amarok.utils.SecurityUtil
import deltazero.amarok.utils.SwitchLocaleUtil
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle as JTextStyle

// Calendar dark theme colors (from colors.xml)
private val CalendarBg = Color(0xFF3A284C)           // calendar_bg
private val CalendarBgLight = Color(0xFF433254)      // calendar_bg_light
private val CalendarWhite = Color(0xD9FFFFFF)        // calendar_white (87% opacity)
private val CalendarWhiteLight = Color(0x4DFFFFFF)   // calendar_white_light (30% opacity)
private val CalendarSelectionColor = Color(0xFFFCCA3E) // calendar_selection_color

class CalendarActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
        else
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { finishAffinity() }
        })

        setContent {
            CalendarScreen(
                onDismissDisguise = {
                    SecurityUtil.dismissDisguise()
                    finish()
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CalendarScreen(onDismissDisguise: () -> Unit) {
    val context = LocalContext.current
    val currentMonth = remember { YearMonth.now() }
    val startMonth = remember { currentMonth.minusMonths(100) }
    val endMonth = remember { currentMonth.plusMonths(100) }
    val firstDayOfWeek = remember { firstDayOfWeekFromLocale() }
    val daysOfWeek = remember { daysOfWeek(firstDayOfWeek) }
    val locale = remember { SwitchLocaleUtil.getActiveLocale(context) }

    val state = rememberCalendarState(
        startMonth = startMonth,
        endMonth = endMonth,
        firstVisibleMonth = currentMonth,
        firstDayOfWeek = firstDayOfWeek
    )

    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    val visibleMonth = state.firstVisibleMonth
    var showInstruction by remember {
        mutableStateOf(PrefMgr.getDoShowQuitDisguiseInstuct())
    }

    // Instruction dialog (replaces Spotlight overlay)
    if (showInstruction) {
        AlertDialog(
            onDismissRequest = { showInstruction = false },
            title = { Text(stringResource(R.string.app_name_calendar)) },
            text = { Text(stringResource(R.string.close_disguise_spotlight_tip)) },
            confirmButton = {
                TextButton(onClick = { showInstruction = false }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showInstruction = false
                    PrefMgr.setDoShowQuitDisguiseInstuct(false)
                }) {
                    Text(stringResource(R.string.spotlight_do_not_show_again))
                }
            },
            containerColor = CalendarBgLight,
            titleContentColor = CalendarWhite,
            textContentColor = CalendarWhiteLight
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CalendarBg)
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header: year + month + day-of-week labels
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CalendarBgLight)
                    .padding(14.dp)
            ) {
                Column {
                    // Year — long press to dismiss disguise
                    Text(
                        text = visibleMonth.yearMonth.year.toString(),
                        color = CalendarWhiteLight,
                        fontSize = 24.sp,
                        modifier = Modifier.combinedClickable(
                            onClick = {},
                            onLongClick = onDismissDisguise
                        )
                    )
                    // Month name
                    Text(
                        text = visibleMonth.yearMonth.month
                            .getDisplayName(JTextStyle.FULL, locale),
                        color = CalendarWhite,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Light
                    )
                    // Day-of-week header row
                    Spacer(Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        daysOfWeek.forEach { dayOfWeek ->
                            Text(
                                text = dayOfWeek.getDisplayName(JTextStyle.SHORT, locale),
                                color = CalendarWhite,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            // Calendar grid
            HorizontalCalendar(
                state = state,
                dayContent = { day ->
                    DayCell(
                        day = day,
                        isSelected = day.date == selectedDate,
                        isToday = day.date == LocalDate.now(),
                        onClick = { selectedDate = day.date }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            // Bottom caption
            Text(
                text = stringResource(R.string.calendar_about),
                color = CalendarWhiteLight,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DayCell(
    day: CalendarDay,
    isSelected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit
) {
    val textColor = when {
        day.position != DayPosition.MonthDate -> CalendarWhiteLight
        isSelected -> CalendarBg
        else -> CalendarWhite
    }
    val bgColor = when {
        isSelected -> CalendarSelectionColor
        isToday -> Color(0x33FFFFFF) // subtle white highlight for today
        else -> Color.Transparent
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(5.dp)
            .clip(CircleShape)
            .background(bgColor)
            .then(
                if (day.position == DayPosition.MonthDate)
                    Modifier.combinedClickable(onClick = onClick)
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = day.date.dayOfMonth.toString(),
            color = textColor,
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
    }
}
