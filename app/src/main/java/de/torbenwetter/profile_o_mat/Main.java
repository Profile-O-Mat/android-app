package de.torbenwetter.profile_o_mat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
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
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import es.dmoral.toasty.Toasty;

public class Main extends AppCompatActivity {

    static final Point size = new Point();

    private LinearLayout linearLayout;
    private ProgressBar progressBar;
    private BarChart barChart;
    private View topView;
    private RelativeLayout bottomLayout;
    private Button button;

    private static Boolean requesting = false;
    private static Boolean wasFirstTime = false;

    private static List<Integer> needsDie = null;

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

        linearLayout = findViewById(R.id.layout);

        final TextView headerView = findViewById(R.id.header);
        headerView.setTypeface(Typeface.createFromAsset(getAssets(), "roboto-thin.ttf"));

        final TextView infoView = findViewById(R.id.info);
        infoView.setTypeface(Typeface.createFromAsset(getAssets(), "roboto-thin.ttf"));

        final TextView infoView2 = findViewById(R.id.info2);
        infoView2.setTypeface(Typeface.createFromAsset(getAssets(), "roboto-thin.ttf"));

        final EditText input = findViewById(R.id.input);
        input.setTypeface(Typeface.createFromAsset(getAssets(), "roboto-thin.ttf"));

        topView = findViewById(R.id.topView);
        bottomLayout = findViewById(R.id.bottomLayout);

        final Button startButton = findViewById(R.id.startButton);
        startButton.setTypeface(Typeface.createFromAsset(getAssets(), "roboto-medium.ttf"));
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Tastatur schließen
                final View theView = getCurrentFocus();
                if (theView != null) {
                    final InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                    if (imm != null)
                        imm.hideSoftInputFromWindow(theView.getWindowToken(), 0);
                }

                final String username = input.getText().toString().trim();
                if (username.isEmpty()) {
                    input.setText("");
                    return;
                }

                if (!username.matches("^[A-Za-z0-9_]{1,15}$")) {
                    Toasty.warning(Main.this, getResources().getString(R.string.nameNotExist), Toast.LENGTH_SHORT, true).show();
                    return;
                }

                if (requesting)
                    return;
                requesting = true;

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        final String pageTitle = getPageTitle(username);
                        if (pageTitle.equals("Twitter / ?")) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toasty.warning(Main.this, getResources().getString(R.string.nameNotExist), Toast.LENGTH_SHORT, true).show();
                                }
                            });
                            requesting = false;
                            return;
                        }

                        final String realUserName = getRealUserName(pageTitle);

                        /*final String partiesContent = "{\n" +
                                "  \"AfD\": {\n" +
                                "    \"color\": \"009DE0\",\n" +
                                "    \"die\": true\n" +
                                "  },\n" +
                                "  \"Bündnis 90\\\\Die Grünen\": {\n" +
                                "    \"color\": \"1FAF12\",\n" +
                                "    \"die\": false\n" +
                                "  },\n" +
                                "  \"CDU\": {\n" +
                                "    \"color\": \"FB0F0C\",\n" +
                                "    \"die\": true\n" +
                                "  },\n" +
                                "  \"CSU\": {\n" +
                                "    \"color\": \"1B86BA\",\n" +
                                "    \"die\": true\n" +
                                "  },\n" +
                                "  \"Die Linke\": {\n" +
                                "    \"color\": \"DE0202\",\n" +
                                "    \"die\": false\n" +
                                "  },\n" +
                                "  \"SPD\": {\n" +
                                "    \"color\": \"E4332D\",\n" +
                                "    \"die\": true\n" +
                                "  },\n" +
                                "  \"piraten\": {\n" +
                                "    \"color\": \"F68920\",\n" +
                                "    \"die\": true\n" +
                                "  }\n" +
                                "}";*/
                        final String partiesContent = Main.getPageContent("https://wetter.codes/Profile-O-Mat/parties.php");
                        final JsonObject partiesWholeObject = new JsonParser().parse(partiesContent).getAsJsonObject();
                        final String[] partiesNames = jsonKeySet(partiesWholeObject);
                        final int partiesAmount = partiesNames.length;

                        final String[] partiesColors = new String[partiesAmount];
                        needsDie = new ArrayList<>();
                        for (int i = 0; i < partiesAmount; i++) {
                            final JsonObject partiesObject = partiesWholeObject.get(partiesNames[i]).getAsJsonObject();
                            partiesColors[i] = partiesObject.get("color").getAsString();
                            if (partiesObject.get("die").getAsBoolean())
                                needsDie.add(i);
                        }

                        final RelativeLayout progressLayout = findViewById(R.id.progressLayout);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (!username.equals(realUserName))
                                    input.setText(realUserName);

                                if (wasFirstTime) {
                                    final List<BarEntry> barEntries = new ArrayList<>();
                                    for (int i = 0; i < partiesAmount; i++)
                                        barEntries.add(new BarEntry(i, 0));
                                    barChart.setData(getBarData(barEntries, partiesColors));
                                    barChart.invalidate();
                                }

                                progressBar = new ProgressBar(Main.this, null, android.R.attr.progressBarStyleLarge);
                                final RelativeLayout.LayoutParams progressParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                                progressBar.setLayoutParams(progressParams);
                                progressLayout.addView(progressBar);
                                progressBar.setVisibility(View.VISIBLE);
                            }
                        });

                        /*final String content = "{\n" +
                                "\"data\": {\n" +
                                "\"AfD\": 0.00008642391301104517,\n" +
                                "\"Bündnis 90\\\\Die Grünen\": 0.40265306010028773,\n" +
                                "\"CDU\": 0.001353983167633387,\n" +
                                "\"CSU\": 1.4326372462715368e-9,\n" +
                                "\"Die Linke\": 0.20239931964089777,\n" +
                                "\"SPD\": 0.39350669876541483,\n" +
                                "\"piraten\": 5.129801179535662e-7\n" +
                                "},\n" +
                                "\"error\": {},\n" +
                                "\"success\": true\n" +
                                "}";*/
                        final String content = Main.getPageContent("http://www.profile-o-mat.de:8080/predict?user=" + realUserName);
                        //final String content = Main.getPageContent("https://profile-o-mat.de:8080/predict?user=" + realUserName);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progressBar.setVisibility(View.INVISIBLE);
                                progressLayout.removeView(progressBar);
                                requesting = false;

                                if (content == null) {
                                    Toasty.error(Main.this, getResources().getString(R.string.internalServerError), Toast.LENGTH_SHORT, true).show();
                                    return;
                                }

                                final JsonObject jsonObjectWhole = new JsonParser().parse(content).getAsJsonObject();
                                final boolean success = jsonObjectWhole.get("success").getAsBoolean();
                                if (!success) {
                                    Toasty.error(Main.this, getResources().getString(R.string.internalServerError), Toast.LENGTH_SHORT, true).show();
                                    return;
                                }
                                final JsonObject jsonObject = jsonObjectWhole.get("data").getAsJsonObject();
                                final String[] parties = jsonKeySet(jsonObject);
                                final float[] values = new float[parties.length];
                                for (int i = 0; i < parties.length; i++)
                                    values[i] = jsonObject.get(parties[i]).getAsFloat();

                                wasFirstTime = true;

                                if (barChart == null) {
                                    barChart = new BarChart(Main.this);
                                    final LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0);
                                    barParams.weight = 100;
                                    barChart.setLayoutParams(barParams);
                                    barChart.setBackground(ContextCompat.getDrawable(Main.this, R.drawable.graph_top_bottom_line));

                                    final LinearLayout.LayoutParams bottomLayoutParams = (LinearLayout.LayoutParams) bottomLayout.getLayoutParams();
                                    bottomLayoutParams.weight = 0;
                                    bottomLayout.setLayoutParams(bottomLayoutParams);

                                    linearLayout.removeView(bottomLayout);
                                    linearLayout.addView(barChart);
                                    linearLayout.addView(bottomLayout);

                                    animateGraph(true, jsonObject, parties, realUserName);
                                } else {
                                    final int highestValueIndex = getHighestValueIndex(jsonObject);
                                    button.setOnClickListener(getOnClickListener(realUserName, partyNeedsArticle(highestValueIndex, needsDie) ? "die " : "", parties[highestValueIndex]));
                                }

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
                                barChart.setExtraLeftOffset(dpToPixel(1.125f));
                                barChart.setExtraBottomOffset(dpToPixel(2.375f));
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
                                barChart.setData(getBarData(barEntries, partiesColors));

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
                                final int maxPercent = 50;
                                leftYAxis.setAxisMaximum(maxPercent);
                                leftYAxis.setLabelCount(maxPercent / 10 + 1, true);
                                leftYAxis.setTextSize(dpToPixel(2.875f));
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

        if (!hasStoragePermissions())
            ActivityCompat.requestPermissions(Main.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1 /* REQUEST_EXTERNAL_STORAGE */);
    }

    private boolean partyNeedsArticle(int partyIndex, List<Integer> needsDie) {
        return needsDie.contains(partyIndex);
    }

    private int getHighestValueIndex(JsonObject jsonObject) {
        final float[] valueSet = jsonValueSet(jsonObject);

        int highestIndex = -1;
        if (valueSet.length != 0) {
            float highest = valueSet[0];
            for (int i = 1; i < valueSet.length; i++) {
                if (valueSet[i] > highest) {
                    highest = valueSet[i];
                    highestIndex = i;
                }
            }
        }

        return highestIndex;
    }

    private float[] jsonValueSet(JsonObject jsonObject) {
        final Set<Map.Entry<String, JsonElement>> entrySet = jsonObject.entrySet();
        final float[] valueSet = new float[entrySet.size()];

        int count = 0;
        for (Map.Entry<String, JsonElement> entry : entrySet) {
            valueSet[count] = entry.getValue().getAsFloat();
            count++;
        }

        return valueSet;
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

    private String getRealUserName(String pageTitle) {
        return pageTitle.substring(pageTitle.indexOf("(") + "(".length(), pageTitle.indexOf(")")).replace("@", "");
    }

    private String getPageTitle(String twitterName) {
        final String pageContent = getPageContent("https://twitter.com/" + twitterName.toLowerCase());
        return pageContent != null ? pageContent.substring(pageContent.indexOf("<title>") + "<title>".length(), pageContent.indexOf("</title>")) : "Twitter / ?";
    }

    private static String getPageContent(String link) {
        try {
            final BufferedReader br = getBufferedReader(link);
            if (br != null) {
                String inputLine;
                final StringBuilder content = new StringBuilder();
                while ((inputLine = br.readLine()) != null)
                    content.append(inputLine).append("\n");
                br.close();
                return content.toString().trim();
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

    private void animateGraph(final Boolean up, final JsonObject jsonObject, final String[] parties, final String realUserName) {
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

                            final LinearLayout.LayoutParams bottomLayoutParams = (LinearLayout.LayoutParams) bottomLayout.getLayoutParams();
                            bottomLayoutParams.weight = up ? j : 100 - j;
                            bottomLayout.setLayoutParams(bottomLayoutParams);

                            if (up && j == 100) {
                                if (button == null) {
                                    button = new Button(Main.this);
                                    button.setAllCaps(false);
                                    button.setTypeface(Typeface.createFromAsset(getAssets(), "roboto-medium.ttf"));
                                    button.setText(getResources().getString(R.string.share_button));
                                    button.setTextColor(ContextCompat.getColor(Main.this, R.color.colorGreyDark));
                                    button.setTextSize(dpToPixel(8));
                                    button.setPadding((int) dpToPixel(10), 0, (int) dpToPixel(2), 0);
                                    button.setBackground(ContextCompat.getDrawable(Main.this, R.drawable.edittext_bottom_line));
                                    if (Build.VERSION.SDK_INT >= 21)
                                        button.setStateListAnimator(null); // In Versionen unter 21 bleibt Schatten bei Button
                                    button.setCompoundDrawablesWithIntrinsicBounds(null, null, ContextCompat.getDrawable(Main.this, R.mipmap.share_result_icon), null);
                                }

                                final int highestValueIndex = getHighestValueIndex(jsonObject);
                                button.setOnClickListener(getOnClickListener(realUserName, partyNeedsArticle(highestValueIndex, needsDie) ? "die " : "", parties[highestValueIndex]));
                                bottomLayout.addView(button);
                            }

                            if (!up) {
                                barChart.setAlpha(1 / j);
                                if (j == 100) {
                                    bottomLayoutParams.weight = 100;
                                    bottomLayout.setLayoutParams(bottomLayoutParams);

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

    private View.OnClickListener getOnClickListener(final String realUserName, final String partyArticle, final String party) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final boolean storagePermissions = hasStoragePermissions();

                final Intent intent = new Intent();
                intent.setAction(Intent.ACTION_SEND);
                intent.putExtra(Intent.EXTRA_TEXT, getResources().getString(R.string.share_message,
                        realUserName, partyArticle + party, System.getProperty("line.separator")));
                if (storagePermissions)
                    intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(getScreenshotImage()));
                intent.setType(storagePermissions ? "*/*" : "text/plain");
                startActivity(Intent.createChooser(intent, getResources().getText(R.string.send_to)));
            }
        };
    }

    private File getScreenshotImage() {
        try {
            final Date date = new Date();
            DateFormat.format("yyyy-MM-dd_hh:mm:ss", date);
            final String path = Environment.getExternalStorageDirectory().toString() + "/" + date + ".jpg";

            final View view = getWindow().getDecorView().getRootView();
            view.setDrawingCacheEnabled(true);
            final Bitmap bitmap = getMiddleOfBitmap(view.getDrawingCache());
            view.setDrawingCacheEnabled(false);

            final File screenshotImage = new File(path);
            final FileOutputStream fos = new FileOutputStream(screenshotImage);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();

            return screenshotImage;
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    private Bitmap getMiddleOfBitmap(Bitmap bitmap) {
        final int lineWidth = (int) dpToPixel(1);
        return Bitmap.createBitmap(bitmap, 0, barChart.getTop() + lineWidth, bitmap.getWidth(), barChart.getHeight() - (2 * lineWidth));
    }

    private BarData getBarData(List<BarEntry> barEntries, String[] colorCodes) {
        final BarDataSet barDataSet = new BarDataSet(barEntries, "Parteien");

        barDataSet.setColors(getBarColors(colorCodes));
        barDataSet.setValueTextSize(dpToPixel(3.375f));
        barDataSet.setValueTypeface(Typeface.createFromAsset(getAssets(), "roboto-medium.ttf"));
        barDataSet.setValueTextColor(ContextCompat.getColor(Main.this, R.color.colorBlackLight));
        barDataSet.setValueFormatter(new PercentFormatter());

        return new BarData(barDataSet);
    }

    private int[] getBarColors(String[] colorCodes) {
        final int[] colors = new int[colorCodes.length];
        for (int i = 0; i < colorCodes.length; i++)
            colors[i] = Color.parseColor("#" + colorCodes[i]);
        return colors;
    }

    private float dpToPixel(float dp) {
        final float density = getResources().getDisplayMetrics().density;
        return dp * (density == 1.0f || density == 1.5f || density == 2.0f ? 3.0f : density) + 0.5f;
    }

    @Override
    public void onBackPressed() {
        if (wasFirstTime) {
            wasFirstTime = false;
            bottomLayout.removeView(button);
            animateGraph(false, null, null, null);
        } else
            super.onBackPressed();
    }

    private boolean hasStoragePermissions() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }
}
