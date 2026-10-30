package com.example.gymtrackerpro;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class ProgramsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_programs);
        UiFx.applyEdgeToEdge(this);

        LinearLayout container = findViewById(R.id.containerProgramsList);
        List<View> cards = new ArrayList<>();

        for (ProgramCatalog.Program program : ProgramCatalog.all()) {
            View card = ProgramCardBinder.create(this, container, program);

            ViewGroup.LayoutParams params = card.getLayoutParams();
            if (params instanceof ViewGroup.MarginLayoutParams) {
                ((ViewGroup.MarginLayoutParams) params).bottomMargin =
                        UiFx.dp(card, 16);
            }

            container.addView(card);
            cards.add(card);
        }

        UiFx.staggerIn(cards.toArray(new View[0]));

        findViewById(R.id.buttonBackHome)
                .setOnClickListener(v -> finish());
    }
}
