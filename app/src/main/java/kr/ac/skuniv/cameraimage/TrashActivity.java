package kr.ac.skuniv.cameraimage;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

public class TrashActivity extends AppCompatActivity {
    ImageButton back, add;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trash);

        back = findViewById(R.id.button15);
        add = findViewById(R.id.button16);

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(TrashActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });
    }
}
