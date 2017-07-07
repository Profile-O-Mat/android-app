package de.torbenwetter.profile_o_mat;

import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.formatter.IValueFormatter;
import com.github.mikephil.charting.utils.ViewPortHandler;

import java.text.DecimalFormat;

class PercentFormatter implements IValueFormatter, IAxisValueFormatter {

    private final DecimalFormat decimalFormat;

    PercentFormatter() {
        decimalFormat = new DecimalFormat("0");
    }

    // IValueFormatter
    @Override
    public String getFormattedValue(float value, Entry entry, int dataSetIndex, ViewPortHandler viewPortHandler) {
        return (int) entry.getX() == Main.highlightedIndex ? (decimalFormat.format(value) + "%") : "";
    }

    // IAxisValueFormatter
    @Override
    public String getFormattedValue(float value, AxisBase axis) {
        return decimalFormat.format(value).replaceFirst("^0+(?!$)", "") + "%";
    }
}