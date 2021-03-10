package com.example.iperfv2;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

public class ChartHandler {

    private Context context;
    private Activity activity;
    private LineChart chart;

    public ChartHandler(Context context, Activity activity) {
        this.context = context;
        this.activity = activity;

        chart = (LineChart) activity.findViewById(R.id.chart1);
        chart.setBackgroundColor(Color.LTGRAY);
        chart.getDescription().setEnabled(false);
        chart.setNoDataText("Run an iPerf command to graph output");
    }

    public void addEntry(float down, int interval) {

        LineData data = chart.getData();
        chart.setData(data);

        if (data != null) {

            ILineDataSet set = data.getDataSetByIndex(0);

            if (set == null) {
                set = createSet(0);
                data.addDataSet(set);
            }
            data.addEntry(new Entry(set.getEntryCount() * interval, (float) down), 0);
            data.notifyDataChanged();
            chart.notifyDataSetChanged();
            chart.invalidate();
        }
    }

    public void addDualEntry(float up, float down, int interval) {

        LineData data = chart.getData();
        chart.setData(data);

        if (data != null) {

            ILineDataSet setDown = data.getDataSetByIndex(0);
            ILineDataSet setUp = data.getDataSetByIndex(1);

            if (setDown == null) {
                setDown = createSet(0);
                data.addDataSet(setDown);
            }
            if (setUp == null) {
                setUp = createSet(1);
                data.addDataSet(setUp);
            }

            setDown.addEntry(new Entry(setDown.getEntryCount() * interval, (float) down));
            setUp.addEntry(new Entry(setUp.getEntryCount() * interval, (float) up));
            data.notifyDataChanged();
            chart.notifyDataSetChanged();
            chart.invalidate();

        }
    }

    public LineDataSet createSet(int setType) {
        LineDataSet set = null;
        switch(setType) {
            case 0:
                set = new LineDataSet(null, "Download");
                set.setAxisDependency(YAxis.AxisDependency.LEFT);
                set.setColor(Color.BLUE);
                set.setCircleColor(Color.WHITE);
                set.setLineWidth(2f);
                set.setFillAlpha(65);
                set.setDrawValues(false);
                return set;

            case 1:
                set = new LineDataSet(null, "Upload");
                set.setAxisDependency(YAxis.AxisDependency.LEFT);
                set.setColor(Color.RED);
                set.setCircleColor(Color.WHITE);
                set.setLineWidth(2f);
                set.setFillAlpha(65);
                set.setDrawValues(false);
                return set;
        }
        return set;
    }

    public LineChart getChart() {
        return chart;
    }
}
