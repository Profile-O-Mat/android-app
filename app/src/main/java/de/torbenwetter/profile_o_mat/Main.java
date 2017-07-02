package de.torbenwetter.profile_o_mat;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class Main extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if (getSupportActionBar() != null)
            getSupportActionBar().hide();
        setContentView(R.layout.activity_main);

        final TextView headerView = (TextView) findViewById(R.id.header);
        headerView.setTypeface(Typeface.createFromAsset(getAssets(), "roboto-thin.ttf"));

        final TextView infoView = (TextView) findViewById(R.id.info);
        infoView.setTypeface(Typeface.createFromAsset(getAssets(), "roboto-thin.ttf"));

        final EditText input = (EditText) findViewById(R.id.input);
        input.setTypeface(Typeface.createFromAsset(getAssets(), "roboto-thin.ttf"));

        final Button startButton = (Button) findViewById(R.id.startButton);
        startButton.setTypeface(Typeface.createFromAsset(getAssets(), "roboto-thin.ttf"));
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String username = input.getText().toString();
                if (!username.isEmpty()) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            if (twitterNameExists(username)) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        final Intent intent = new Intent(Main.this, SelectTwitter.class);
                                        intent.putExtra("username", username);
                                        intent.putExtra("direction", "forwards");
                                        startActivity(intent);
                                    }
                                });
                            } else {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(Main.this, "The username doesn't exist.", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        }
                    }).start();
                }
            }
        });

        final String direction = getDirection(savedInstanceState, this);
        if (direction.equals("forwards"))
            overridePendingTransition(R.anim.to_right_in, R.anim.to_right_out);
        else
            overridePendingTransition(R.anim.to_left_in, R.anim.to_left_out);
    }

    private Boolean twitterNameExists(String twitterName) {
        final String pageContent = getPageContent("https://twitter.com/" + twitterName);
        if (pageContent != null) {
            final String title = pageContent.substring(pageContent.indexOf("<title>") + "<title>".length(), pageContent.indexOf("</title>"));
            return !title.equals("Twitter / ?");
        }
        return false;
    }

    private String getPageContent(String link) {
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

    private BufferedReader getBufferedReader(String link) {
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

    private String getDirection(Bundle savedInstanceState, Activity activity) {
        String direction;
        if (savedInstanceState == null) {
            final Bundle extras = activity.getIntent().getExtras();
            direction = extras == null ? null : extras.getString("direction");
        } else
            direction = (String) savedInstanceState.getSerializable("direction");
        return direction == null ? "forwards" : direction;
    }
}
