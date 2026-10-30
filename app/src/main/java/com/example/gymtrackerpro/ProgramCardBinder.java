package com.example.gymtrackerpro;

import android.app.Activity;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

/** Builds a tappable program cover card (used on Home and Programs). */
public final class ProgramCardBinder {

    private ProgramCardBinder() {
    }

    public static View create(final Activity activity,
                              ViewGroup parent,
                              final ProgramCatalog.Program program) {

        View card = LayoutInflater.from(activity)
                .inflate(R.layout.item_program_card, parent, false);

        card.findViewById(R.id.viewCover)
                .setBackgroundResource(program.coverRes);

        ((ImageView) card.findViewById(R.id.imageDeco))
                .setImageResource(program.decoRes);

        ((TextView) card.findViewById(R.id.textProgramLevel))
                .setText(program.level);

        ((TextView) card.findViewById(R.id.textProgramTitle))
                .setText(program.title);

        ((TextView) card.findViewById(R.id.textProgramTagline))
                .setText(program.tagline);

        ((TextView) card.findViewById(R.id.textProgramMeta))
                .setText(program.scheduleSummary());

        card.setOnClickListener(v -> {
            Intent intent = new Intent(
                    activity, ProgramDetailActivity.class);

            intent.putExtra(
                    ProgramDetailActivity.EXTRA_PROGRAM_ID, program.id);

            activity.startActivity(intent);
        });

        UiFx.applyPressEffect(card);

        return card;
    }
}
