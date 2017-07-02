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
                    final Intent intent = new Intent(Main.this, SelectTwitter.class);
                    intent.putExtra("username", username);
                    intent.putExtra("direction", "forwards");
                    startActivity(intent);
                }
            }
        });

        final String direction = getDirection(savedInstanceState, this);
        if (direction.equals("forwards"))
            overridePendingTransition(R.anim.to_right_in, R.anim.to_right_out);
        else
            overridePendingTransition(R.anim.to_left_in, R.anim.to_left_out);
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
