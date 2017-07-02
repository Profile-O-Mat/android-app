package de.torbenwetter.profile_o_mat;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.Map;
import java.util.Set;

public class SelectTwitter extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if (getSupportActionBar() != null)
            getSupportActionBar().hide();
        setContentView(R.layout.activity_select_twitter);

        String username;
        String direction;
        if (savedInstanceState == null) {
            final Bundle extras = getIntent().getExtras();
            username = extras == null ? null : extras.getString("username");
            direction = extras == null ? null : extras.getString("direction");
        } else {
            username = (String) savedInstanceState.getSerializable("username");
            direction = (String) savedInstanceState.getSerializable("direction");
        }

        if (direction != null) {
            if (direction.equals("forwards"))
                overridePendingTransition(R.anim.to_right_in, R.anim.to_right_out);
            else
                overridePendingTransition(R.anim.to_left_in, R.anim.to_left_out);
        }

        final String twittername = username;

        final ProgressDialog progressDialog = new ProgressDialog(SelectTwitter.this);
        progressDialog.setMessage("Loading...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        new Thread(new Runnable() {
            @Override
            public void run() {
                final String content = Main.getPageContent("http://saturn.maschinen.space:8080/predict?user=" + twittername);
                final String colorsContent = Main.getPageContent("https://styx.me/Profile-O-Mat");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.dismiss();

                        if (content == null || colorsContent == null) {
                            Toast.makeText(SelectTwitter.this, "internal server error.", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        final JsonObject jsonObject = new JsonParser().parse(content).getAsJsonObject();

                        final String[] parties = jsonKeySet(jsonObject);
                        final float[] values = new float[parties.length];
                        for (int i = 0; i < parties.length; i++)
                            values[i] = jsonObject.get(parties[i]).getAsFloat();

                        final String[] colorCodes = colorsContent.split(",");
                        final int[] colors = new int[colorCodes.length];
                        for (int i = 0; i < colorCodes.length; i++)
                            colors[i] = Color.parseColor(colorCodes[i]);

                        final MyPieGraph myPieGraph = new MyPieGraph(SelectTwitter.this, calculateDatas(values), colors);
                        final LinearLayout linearLayout = (LinearLayout) findViewById(R.id.layout);
                        linearLayout.addView(myPieGraph);
                    }
                });
            }
        }).start();
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

    private float[] calculateDatas(float[] datas) {
        float total = 0;
        for (float data : datas)
            total += data;
        for (Integer i = 0; i < datas.length; i++)
            datas[i] = datas[i] / total * 360;
        return datas;
    }

    private class MyPieGraph extends View {

        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final float[] endValues;
        private final int[] colors;
        private final RectF rectF;
        private float temp = 0;
        private boolean drawn = false;

        public MyPieGraph(Context context, float[] values, int[] colors) {
            super(context);

            this.colors = colors;

            endValues = new float[values.length];
            System.arraycopy(values, 0, endValues, 0, values.length);

            final Integer diameter = Main.size.x * 5 / 6;
            final Integer marginToStart = Main.size.x / 12;
            rectF = new RectF(marginToStart, marginToStart, marginToStart + diameter, marginToStart + diameter);
        }

        @Override
        protected void onDraw(final Canvas canvas) {
            super.onDraw(canvas);

            if (drawn)
                return;
            drawn = true;

            drawDiagram(canvas);
        }

        public void drawDiagram(final Canvas canvas) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    for (int i = 0; i < endValues.length; i++) {
                        paint.setColor(colors[i]);
                        temp += i != 0 ? endValues[i - 1] : 0;
                        canvas.drawArc(rectF, temp, endValues[i], true, paint);
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }).start();
        }
    }

    @Override
    public void onBackPressed() {
        final Intent intent = new Intent(SelectTwitter.this, Main.class);
        intent.putExtra("direction", "backwards");
        startActivity(intent);
    }
}
