package kr.ac.skuniv.cameraimage;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

public class MetalActivity extends AppCompatActivity {
    ImageButton back, add;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_metal);

        back = findViewById(R.id.button11);
        add = findViewById(R.id.button12);

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MetalActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });

    }
}
