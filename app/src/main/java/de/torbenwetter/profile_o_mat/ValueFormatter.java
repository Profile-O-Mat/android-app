package de.torbenwetter.profile_o_mat;

import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;

class ValueFormatter implements IAxisValueFormatter {

    private final String[] values;

    ValueFormatter(String[] values) {
        this.values = values;
    }

    @Override
    public String getFormattedValue(float value, AxisBase axis) {
        final int theValue = (int) (value + 0.5);
        return theValue < values.length ? values[theValue].replace("Bündnis 90\\Die Grünen", "B’90/Grüne") : "Partei";
    }
}