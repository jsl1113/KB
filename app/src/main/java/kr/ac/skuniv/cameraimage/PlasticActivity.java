package kr.ac.skuniv.cameraimage;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

public class PlasticActivity extends AppCompatActivity {
    ImageButton back, add;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plastic);

        back = findViewById(R.id.button13);
        add = findViewById(R.id.button14);

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(PlasticActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });
    }
}
