package com.betomaluje.android.miband.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;

import com.betomaluje.android.miband.DateUtils;
import com.betomaluje.android.miband.R;
import com.betomaluje.android.miband.models.ActivityAmount;
import com.betomaluje.android.miband.models.ActivityAmounts;
import com.betomaluje.android.miband.models.ActivityAnalysis;
import com.betomaluje.android.miband.models.ActivityData;
import com.betomaluje.android.miband.models.ActivityKind;
import com.betomaluje.android.miband.sqlite.ActivitySQLite;
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Created by betomaluje on 7/16/15.
 */
public class SleepChartActivity extends BaseActivity {

    private final String TAG = getClass().getSimpleName();
    private BarChart mChart;
    private PieChart pieChart;

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_sleepchart;
    }

    @Override
    protected boolean getDisplayHomeAsUpEnabled() {
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        createGraph();
        createChartLegend();

        populateChart();
    }

    private void createGraph() {
        mChart = (BarChart) findViewById(R.id.chart1);
        mChart.setDescription("");

        // scaling can now only be done on x- and y-axis separately
        mChart.setPinchZoom(false);

        mChart.setDrawBarShadow(false);

        mChart.setDrawGridBackground(false);

        pieChart = (PieChart) findViewById(R.id.pieChart);
    }

    private void createChartLegend() {
        /*
        Legend l = mChart.getLegend();
        l.setPosition(Legend.LegendPosition.RIGHT_OF_CHART_INSIDE);
        l.setYOffset(0f);
        l.setYEntrySpace(0f);
        l.setTextSize(8f);
        */

        XAxis x = mChart.getXAxis();
        x.setDrawLabels(true);
        x.setDrawGridLines(false);
        x.setEnabled(true);
        x.setDrawLimitLinesBehindData(true);

        YAxis y = mChart.getAxisLeft();
        y.setAxisMaxValue(1f);
        y.setDrawTopYLabelEntry(false);
        y.setEnabled(true);

        mChart.getAxisRight().setEnabled(false);
    }

    private void populateChart() {
        Calendar before = Calendar.getInstance();
        before.add(Calendar.DAY_OF_WEEK, -7);
        long temp = before.getTimeInMillis() / 1000;
        before.setTimeInMillis(temp);

        Calendar today = Calendar.getInstance();
        today.setTimeInMillis(System.currentTimeMillis() / 1000);

        Log.i(TAG, "data from " + DateUtils.convertString(before) + " to " + DateUtils.convertString(today));

        ArrayList<ActivityData> allActivities = ActivitySQLite.getInstance(SleepChartActivity.this)
                .getSleepSamples((int) before.getTimeInMillis(), (int) today.getTimeInMillis());
        //.getAllActivities();

        refreshSleepAmounts(allActivities);

        ArrayList<String> xVals = new ArrayList<String>();

        ArrayList<BarEntry> deep = new ArrayList<BarEntry>();
        ArrayList<BarEntry> light = new ArrayList<BarEntry>();
        ArrayList<BarEntry> unknown = new ArrayList<BarEntry>();

        int i = 0;

        float movement_divisor = 180.0f;

        float value;

        String dateString = "";
        for (ActivityData ad : allActivities) {

            Calendar date = Calendar.getInstance();
            date.setTimeInMillis(ad.getTimestamp() * 1000L);

            dateString = DateUtils.convertString(date);

            xVals.add(dateString);

            Log.i(TAG, "date " + dateString);
            Log.i(TAG, "steps " + ad.getSteps());

            value = ((float) ad.getIntensity()) / movement_divisor;

            switch (ad.getType()) {
                case ActivityData.TYPE_DEEP_SLEEP:
                    value += ActivityData.Y_VALUE_DEEP_SLEEP;
                    deep.add(new BarEntry(value, i));
                    break;
                case ActivityData.TYPE_LIGHT_SLEEP:
                    light.add(new BarEntry(value, i));
                    break;
                default:
                    unknown.add(new BarEntry(value, i));
                    break;
            }

            i++;
        }

        BarDataSet set1 = new BarDataSet(deep, "Deep Sleep");
        set1.setColor(Color.BLUE);
        BarDataSet set2 = new BarDataSet(light, "Light Sleep");
        set2.setColor(Color.CYAN);
        //BarDataSet set3 = new BarDataSet(unknown, "Unknown");
        //set3.setColor(Color.RED);

        ArrayList<BarDataSet> dataSets = new ArrayList<BarDataSet>();
        dataSets.add(set1);
        dataSets.add(set2);
        //dataSets.add(set3);

        BarData data = new BarData(xVals, dataSets);
        data.setGroupSpace(0f);

        mChart.setData(data);
        mChart.invalidate();

        mChart.animateY(2000, Easing.EasingOption.EaseInOutQuart);
    }

    private void refreshSleepAmounts(List<ActivityData> samples) {
        ActivityAnalysis analysis = new ActivityAnalysis();
        ActivityAmounts amounts = analysis.calculateActivityAmounts(samples);
        float hoursOfSleep = amounts.getTotalSeconds() / (float) (60 * 60);
        pieChart.setCenterText((int) hoursOfSleep + "h"); // FIXME
        PieData data = new PieData();
        List<Entry> entries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        int index = 0;
        for (ActivityAmount amount : amounts.getAmounts()) {
            entries.add(new Entry(amount.getTotalSeconds(), index++));
            colors.add(getColorFor(amount.getActivityKind()));
            data.addXValue(amount.getName());
        }
        PieDataSet set = new PieDataSet(entries, "Sleep comparison");
        set.setColors(colors);
        data.setDataSet(set);
        pieChart.setData(data);

        //setupLegend(pieChart);

        pieChart.invalidate();
    }

    private Integer getColorFor(int activityKind) {
        switch (activityKind) {
            case ActivityKind.TYPE_DEEP_SLEEP:
                return Color.rgb(76, 90, 255);
            case ActivityKind.TYPE_LIGHT_SLEEP:
                return Color.rgb(182, 191, 255);
        }
        return Color.rgb(89, 178, 44);
    }
}
