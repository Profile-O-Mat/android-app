package de.torbenwetter.profile_o_mat;

import android.app.ProgressDialog;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Typeface;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Main extends AppCompatActivity {

    static final Point size = new Point();

    private LinearLayout linearLayout;
    private ProgressDialog progressDialog;
    private BarChart barChart;
    private View topView;
    private View bottomView;

    private static Boolean requesting = false;
    private static Boolean wasFirstTime = false;

    static int highlightedIndex = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if (getSupportActionBar() != null)
            getSupportActionBar().hide();
        setContentView(R.layout.activity_main);

        final Display display = getWindowManager().getDefaultDisplay();
        display.getSize(size);

        linearLayout = (LinearLayout) findViewById(R.id.layout);

        final TextView headerView = (TextView) findViewById(R.id.header);
        headerView.setTypeface(Typeface.createFromAsset(getAssets(), "roboto-thin.ttf"));

        final TextView infoView = (TextView) findViewById(R.id.info);
        infoView.setTypeface(Typeface.createFromAsset(getAssets(), "roboto-thin.ttf"));

        final TextView infoView2 = (TextView) findViewById(R.id.info2);
        infoView2.setTypeface(Typeface.createFromAsset(getAssets(), "roboto-thin.ttf"));

        final EditText input = (EditText) findViewById(R.id.input);
        input.setTypeface(Typeface.createFromAsset(getAssets(), "roboto-thin.ttf"));

        topView = findViewById(R.id.topView);
        bottomView = findViewById(R.id.bottomView);

        final Button startButton = (Button) findViewById(R.id.startButton);
        startButton.setTypeface(Typeface.createFromAsset(getAssets(), "roboto-medium.ttf"));
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Tastatur schließen
                final View theView = getCurrentFocus();
                if (theView != null) {
                    final InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(theView.getWindowToken(), 0);
                }

                final String username = input.getText().toString().trim();
                if (username.isEmpty()) {
                    input.setText("");
                    return;
                }

                if (!username.matches("^[A-Za-z0-9_]{1,15}$")) {
                    Toast.makeText(Main.this, getResources().getString(R.string.nameNotExist), Toast.LENGTH_SHORT).show();
                    return;
                }

                if (requesting)
                    return;
                requesting = true;

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (!twitterNameExists(username)) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(Main.this, getResources().getString(R.string.nameNotExist), Toast.LENGTH_SHORT).show();
                                }
                            });
                            requesting = false;
                            return;
                        }

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (wasFirstTime) {
                                    final List<BarEntry> barEntries = new ArrayList<>();
                                    for (int i = 0; i < 7; i++) // 7 -> hardcoded
                                        barEntries.add(new BarEntry(i, 0));
                                    final BarDataSet barDataSet = new BarDataSet(barEntries, "Parteien");

                                    barDataSet.setColors(getBarColors());

                                    final BarData barData = new BarData(barDataSet);
                                    barChart.setData(barData);

                                    barChart.invalidate();
                                }

                                progressDialog = new ProgressDialog(Main.this);
                                progressDialog.setMessage(getResources().getString(R.string.loading));
                                progressDialog.setCancelable(false);
                                progressDialog.show();
                            }
                        });

                        final String content = Main.getPageContent("https://profile-o-mat.de:8080/predict?user=" + username);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progressDialog.dismiss();
                                requesting = false;

                                if (content == null) {
                                    Toast.makeText(Main.this, getResources().getString(R.string.internalServerError), Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                wasFirstTime = true;

                                if (barChart == null) {
                                    barChart = new BarChart(Main.this);
                                    final LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0);
                                    barParams.weight = 100;
                                    barChart.setLayoutParams(barParams);
                                    barChart.setBackground(ContextCompat.getDrawable(Main.this, R.drawable.graph_top_bottom_line));

                                    final LinearLayout.LayoutParams bottomViewParams = (LinearLayout.LayoutParams) bottomView.getLayoutParams();
                                    bottomViewParams.weight = 0;
                                    bottomView.setLayoutParams(bottomViewParams);

                                    linearLayout.removeView(bottomView);
                                    linearLayout.addView(barChart);
                                    linearLayout.addView(bottomView);

                                    animateGraph(true);
                                }

                                final JsonObject jsonObject = new JsonParser().parse(content).getAsJsonObject();

                                final String[] parties = jsonKeySet(jsonObject);
                                final float[] values = new float[parties.length];
                                for (int i = 0; i < parties.length; i++)
                                    values[i] = jsonObject.get(parties[i]).getAsFloat();

                                barChart.setDragEnabled(false);
                                barChart.setScaleEnabled(false);
                                barChart.setScaleXEnabled(false);
                                barChart.setScaleYEnabled(false);
                                barChart.setPinchZoom(false);
                                barChart.setDoubleTapToZoomEnabled(false);
                                barChart.setDragDecelerationEnabled(false);
                                barChart.setHighlightPerDragEnabled(false);
                                barChart.getDescription().setEnabled(false);
                                barChart.getLegend().setEnabled(false);
                                barChart.setExtraLeftOffset(5);
                                barChart.setExtraBottomOffset(10);
                                barChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
                                    @Override
                                    public void onValueSelected(Entry e, Highlight h) {
                                        highlightedIndex = (int) e.getX();
                                        barChart.invalidate();
                                    }

                                    @Override
                                    public void onNothingSelected() {
                                        highlightedIndex = -1;
                                        barChart.invalidate();
                                    }
                                });

                                final List<BarEntry> barEntries = new ArrayList<>();
                                for (int i = 0; i < values.length; i++)
                                    barEntries.add(new BarEntry(i, values[i] * 100));
                                final BarDataSet barDataSet = new BarDataSet(barEntries, "Parteien");

                                barDataSet.setColors(getBarColors());
                                barDataSet.setValueTextSize(14);
                                barDataSet.setValueTypeface(Typeface.createFromAsset(getAssets(), "roboto-medium.ttf"));
                                barDataSet.setValueTextColor(ContextCompat.getColor(Main.this, R.color.colorBlackLight));
                                barDataSet.setValueFormatter(new PercentFormatter());

                                final BarData barData = new BarData(barDataSet);
                                barChart.setData(barData);

                                final XAxis xAxis = barChart.getXAxis();
                                xAxis.setDrawAxisLine(false);
                                xAxis.setDrawGridLines(false);
                                xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
                                xAxis.setLabelCount(parties.length + 1, true);
                                xAxis.setTypeface(Typeface.createFromAsset(getAssets(), "roboto-medium.ttf"));
                                xAxis.setTextColor(ContextCompat.getColor(Main.this, R.color.colorPrimary));
                                xAxis.setValueFormatter(new ValueFormatter(parties));
                                xAxis.setCenterAxisLabels(true);

                                final YAxis leftYAxis = barChart.getAxisLeft();
                                leftYAxis.setDrawAxisLine(false);
                                leftYAxis.setDrawGridLines(false);
                                leftYAxis.setAxisMinimum(0);
                                final int maxPercent = 50; // am besten durch 10 teilbar
                                leftYAxis.setAxisMaximum(maxPercent);
                                leftYAxis.setLabelCount(maxPercent / 10 + 1, true);
                                leftYAxis.setTextSize(12);
                                leftYAxis.setTypeface(Typeface.createFromAsset(getAssets(), "roboto-medium.ttf"));
                                leftYAxis.setTextColor(ContextCompat.getColor(Main.this, R.color.colorPrimary));
                                leftYAxis.setValueFormatter(new PercentFormatter());

                                final YAxis rightYAxis = barChart.getAxisRight();
                                rightYAxis.setDrawLabels(false);
                                rightYAxis.setDrawAxisLine(false);
                                rightYAxis.setDrawGridLines(false);

                                barChart.animateY(3000, Easing.EasingOption.Linear);
                            }
                        });
                    }
                }).start();
            }
        });
    }

    private String[] jsonKeySet(JsonObject jsonObject) {
        final Set<Map.Entry<String, JsonElement>> entrySet = jsonObject.entrySet();
        final String[] keySet = new String[entrySet.size()];

        int count = 0;
        for (Map.Entry<String, JsonElement> entry : entrySet) {
            keySet[count] = entry.getKey();
            count++;
        }

        return keySet;
    }

    private Boolean twitterNameExists(String twitterName) {
        final String pageContent = getPageContent("https://twitter.com/" + twitterName.toLowerCase());
        if (pageContent != null) {
            final String title = pageContent.substring(pageContent.indexOf("<title>") + "<title>".length(), pageContent.indexOf("</title>"));
            return !title.equals("Twitter / ?");
        }
        return false;
    }

    static String getPageContent(String link) {
        try {
            final BufferedReader br = getBufferedReader(link);
            if (br != null) {
                String inputLine;
                final StringBuilder content = new StringBuilder();
                while ((inputLine = br.readLine()) != null)
                    content.append(inputLine).append("\n");
                br.close();
                return content.toString();
            } else
                return null;
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    private static BufferedReader getBufferedReader(String link) {
        try {
            final URL url = new URL(link);
            final HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();

            httpURLConnection.setRequestMethod("GET");
            httpURLConnection.setRequestProperty("User-Agent", "Mozilla/5.0");

            return new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream(), "UTF-8"));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    private void animateGraph(final Boolean up) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 1; i <= 100; i++) {
                    final int j = i;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            final LinearLayout.LayoutParams topViewParams = (LinearLayout.LayoutParams) topView.getLayoutParams();
                            topViewParams.weight = up ? 100 - j : j;
                            topView.setLayoutParams(topViewParams);

                            final LinearLayout.LayoutParams bottomViewParams = (LinearLayout.LayoutParams) bottomView.getLayoutParams();
                            bottomViewParams.weight = up ? j : 100 - j;
                            bottomView.setLayoutParams(bottomViewParams);

                            if (!up) {
                                barChart.setAlpha(1 / j);
                                if (j == 100) {
                                    bottomViewParams.weight = 100;
                                    bottomView.setLayoutParams(bottomViewParams);

                                    barChart.setAlpha(0);
                                    linearLayout.removeView(barChart);
                                    barChart = null;
                                }
                            }
                        }
                    });
                    try {
                        Thread.sleep(2);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }).start();
    }

    private int[] getBarColors() {
        final String[] colorCodes = new String[]{"009DE0", "1FAF12", "FB0F0C", "1B86BA", "DE0202", "E4332D", "CCCCCC"};
        final int[] colors = new int[colorCodes.length];
        for (int i = 0; i < colorCodes.length; i++)
            colors[i] = Color.parseColor("#" + colorCodes[i]);
        return colors;
    }

    @Override
    public void onBackPressed() {
        if (wasFirstTime) {
            wasFirstTime = false;
            animateGraph(false);
        } else
            super.onBackPressed();
    }
}
