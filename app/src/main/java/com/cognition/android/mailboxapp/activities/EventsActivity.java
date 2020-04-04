package com.cognition.android.mailboxapp.activities;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.CalendarView;
import android.widget.Toast;

import com.cognition.android.mailboxapp.R;
import com.github.sundeepk.compactcalendarview.CompactCalendarView;
import com.github.sundeepk.compactcalendarview.domain.Event;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class EventsActivity extends AppCompatActivity {

    CompactCalendarView compactCalendarView;
    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MMMM-yyyy", Locale.getDefault());
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_events);

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(false);
        actionBar.setTitle(null);

        compactCalendarView = (CompactCalendarView)findViewById(R.id.compactcalendar_view);
        compactCalendarView.setUseThreeLetterAbbreviation(true);

        /* Event parameters :
        * 1. Color to be shown as dot in calender
        * 2. Epoch Date and Time format(Date and Time both should be passed to this function)
        * 3. Event title.*/
        //event set on 5th April 2020 at 11:51:30. L is for 'Long'
        Event event = new Event(Color.RED, 1586067690000L, "Teachers' Professional Day");
        compactCalendarView.addEvent(event);
        epochToDate(1586067690000L);

        compactCalendarView.setListener(new CompactCalendarView.CompactCalendarViewListener() {
            @Override
            public void onDayClick(Date dateClicked) {
                Context context = getApplicationContext();

                if (dateClicked.toString().compareTo("Sun Apr 05 00:00:00 GMT+05:30 2020") == 0) {
                    Toast.makeText(context, "Teachers' Professional Day", Toast.LENGTH_SHORT).show();

                    /*
                dateClicked.toString() returns "Sun(Day) Apr(Month) 5(Date) 00:00:00(Time) GMT+05:30(Zone) 2020(Year)"
                We have to add events in database with above format and fetch them when date is clicked.
                When no time is in the event, we can set time as 00:00:00.
                dateClicked.toString() does not return an event. It just returns date clicked with 00:00:00 time.

                I think the event table should be as follows:
                1. Epoch DateTime(Time as 00:00:00)  2.Title of Event    3.Time of Event    4.Venue
                Whenever there we click a date , we convert it to Epoch format.
                Check for that epoch value in our database.
                No primary key for the database.
                If matches, get all other values and display maybe on another activity.
                */
                }
            }

            //function to display month and year in the title bar.
            @Override
            public void onMonthScroll(Date firstDayOfNewMonth) {
                actionBar.setTitle(simpleDateFormat.format(firstDayOfNewMonth));
            }
        });
    }

    void epochToDate(long epochTime){
        Date date = new Date(epochTime);
        DateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");  //Can set the date-time format here
        format.setTimeZone(TimeZone.getTimeZone("IST"));
        String formatted = format.format(date);
        System.out.println("THE DATETIME IS:"+formatted);
    }

    void dateToEpoch(String dateStr) throws ParseException {
        SimpleDateFormat df = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy"); //should not be changed. Same format is returned by date onclick
        Date date = df.parse(dateStr);
        long epoch = date.getTime();
        System.out.println("EPOCH TIME: "+epoch);
    }
}