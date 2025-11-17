package deltazero.amarok.ui;

import static com.kizitonwose.calendar.core.ExtensionsKt.firstDayOfWeekFromLocale;
import static deltazero.amarok.utils.SwitchLocaleUtil.getActiveLocale;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.kizitonwose.calendar.core.CalendarDay;
import com.kizitonwose.calendar.core.CalendarMonth;
import com.kizitonwose.calendar.core.DayPosition;
import com.kizitonwose.calendar.core.ExtensionsKt;
import com.kizitonwose.calendar.view.CalendarView;
import com.kizitonwose.calendar.view.MonthDayBinder;
import com.kizitonwose.calendar.view.ViewContainer;
import com.takusemba.spotlight.Spotlight;
import com.takusemba.spotlight.Target;
import com.takusemba.spotlight.shape.Circle;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;

import deltazero.amarok.PrefMgr;
import deltazero.amarok.R;
import deltazero.amarok.utils.SecurityUtil;

public class CalendarActivity extends AppCompatActivity {

    CalendarView calendarView;
    TextView tvYear, tvMonth;

    LocalDate selectedDate = null;
    CalendarDay selectedDateDay = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0);
        else
            overridePendingTransition(0, 0);

        super.onCreate(savedInstanceState);
        
        // Enable edge-to-edge (CalendarActivity doesn't extend AmarokActivity)
        WindowCompat.enableEdgeToEdge(getWindow());
        
        setContentView(R.layout.activity_calendar);

        calendarView = findViewById(R.id.calendar_view);
        tvMonth = findViewById(R.id.calendar_tv_month_text);
        tvYear = findViewById(R.id.calendar_tv_year_text);

        YearMonth currentMonth = YearMonth.now();
        YearMonth startMonth = currentMonth.minusMonths(100);
        YearMonth endMonth = currentMonth.plusMonths(100);

        calendarView.setup(startMonth, endMonth, firstDayOfWeekFromLocale());

        setupDayBinder(calendarView, currentMonth);
        setupMonthHeader();
        updateTitle();

        calendarView.setMonthScrollListener(calendarMonth -> {
            updateTitle();
            return null;
        });

        tvYear.setOnLongClickListener(v -> {
            SecurityUtil.dismissDisguise();
            finish();
            return true;
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finishAffinity();
            }
        });

        if (PrefMgr.getDoShowQuitDisguiseInstuct()) {
            showInstruction();
        }
    }

    @SuppressLint("MissingInflatedId")
    private void showInstruction() {

        float circleRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 180, getResources().getDisplayMetrics());

        var spotlightLayout = getLayoutInflater().inflate(R.layout.spotlight_layout, new FrameLayout(this));
        var target = new Target.Builder()
                .setAnchor(tvYear)
                .setShape(new Circle(circleRadius))
                .setOverlay(spotlightLayout)
                .build();

        var spotlight = new Spotlight.Builder(this)
                .setTargets(target)
                .setBackgroundColor(getColor(R.color.dark_grey))
                .setDuration(800L)
                .build();

        spotlightLayout.setOnClickListener(v -> spotlight.finish());
        spotlightLayout.findViewById(R.id.calendar_bt_spotlight_do_not_show_again).setOnClickListener(v -> {
            spotlight.finish();
            PrefMgr.setDoShowQuitDisguiseInstuct(false);
        });
        spotlight.start();
    }

    private void updateTitle() {
        CalendarMonth calendarMonth = calendarView.findFirstVisibleMonth();
        if (calendarMonth != null) {
            tvMonth.setText(calendarMonth.getYearMonth().getMonth()
                    .getDisplayName(TextStyle.FULL, getActiveLocale(this)));
            tvYear.setText(String.valueOf(calendarMonth.getYearMonth().getYear()));
        }
    }

    private void setupMonthHeader() {
        ViewGroup titlesContainer = findViewById(R.id.titlesContainer);
        List<DayOfWeek> daysOfWeek = ExtensionsKt.daysOfWeek();
        for (int i = 0; i < titlesContainer.getChildCount(); i++) {
            TextView textView = (TextView) titlesContainer.getChildAt(i);
            DayOfWeek dayOfWeek = daysOfWeek.get(i);
            String title = dayOfWeek.getDisplayName(TextStyle.SHORT, getActiveLocale(this));
            textView.setText(title);
        }
    }

    private void setupDayBinder(CalendarView calendarView, YearMonth currentMonth) {
        calendarView.setDayBinder(new MonthDayBinder<DayViewContainer>() {
            @NonNull
            @Override
            public DayViewContainer create(@NonNull View view) {
                return new DayViewContainer(view);
            }

            @Override
            public void bind(@NonNull DayViewContainer container, CalendarDay day) {

                container.day = day;
                var tvDate = container.textView;

                tvDate.setText(String.valueOf(day.getDate().getDayOfMonth()));
                if (day.getPosition() != DayPosition.MonthDate) {
                    tvDate.setTextColor(getColor(R.color.calendar_white_light));
                    tvDate.setBackgroundResource(0);
                } else if (day.getDate().equals(selectedDate)) {
                    tvDate.setTextColor(getColor(R.color.calendar_bg));
                    tvDate.setBackgroundResource(R.drawable.calendar_selected_bg);
                } else if (day.getDate().equals(LocalDate.now())) {
                    tvDate.setTextColor(getColor(R.color.calendar_white));
                    tvDate.setBackgroundResource(R.drawable.calendar_today_bg);
                } else {
                    tvDate.setTextColor(getColor(R.color.calendar_white));
                    tvDate.setBackgroundResource(0);
                }
            }
        });

        calendarView.scrollToMonth(currentMonth);
    }

    private class DayViewContainer extends ViewContainer {

        CalendarDay day;
        TextView textView;

        public DayViewContainer(View view) {
            super(view);
            textView = view.findViewById(R.id.calendar_tv_day_text);
            view.setOnClickListener(v -> {
                selectedDate = day.getDate();
                if (selectedDateDay != null) {
                    calendarView.notifyDayChanged(selectedDateDay);
                }
                selectedDateDay = day;
                calendarView.notifyDayChanged(day);
            });
        }
    }
}