package de.torbenwetter.profile_o_mat;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Toast;

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

        if (direction.equals("forwards"))
            overridePendingTransition(R.anim.to_right_in, R.anim.to_right_out);
        else
            overridePendingTransition(R.anim.to_left_in, R.anim.to_left_out);

        // Anfrage mit Username an Socket
        Toast.makeText(this, username, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBackPressed() {
        final Intent intent = new Intent(SelectTwitter.this, Main.class);
        intent.putExtra("direction", "backwards");
        startActivity(intent);
    }
}
