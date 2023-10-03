package kr.ac.skuniv.cameraimage;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

public class CardboardPaperActivity extends AppCompatActivity {
    ImageButton back, add;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cardboard_paper);

        back = findViewById(R.id.button7);
        add = findViewById(R.id.button8);

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(CardboardPaperActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });

    }
}
