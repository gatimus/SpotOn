package io.github.gatimus.spoton;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class SpotOn extends Activity implements SpotOnView.SpotOnViewListener{

    private static final String HIGH_SCORE = "HIGH_SCORE";
    private SpotOnView view;
    private TextView highScoreTextView;
    private TextView currentScoreTextView;
    private TextView levelTextView;
    private SharedPreferences sharedPreferences;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        highScoreTextView = (TextView) findViewById(R.id.highScoreTextView);
        currentScoreTextView = (TextView) findViewById(R.id.scoreTextView);
        levelTextView = (TextView) findViewById(R.id.levelTextView);
        sharedPreferences = getPreferences(Context.MODE_PRIVATE);
        RelativeLayout layout = (RelativeLayout) findViewById(R.id.relativeLayout);
        view = new SpotOnView(this, layout);
        view.setZOrderOnTop(true);
        layout.addView(view, 0);
    } //onCreate

    @Override
    public void onPause() {
        super.onPause();
        view.pause();
    }

    @Override
    public void onResume() {
        super.onResume();
        view.resume(this);
    }


    @Override
    public void displayScores(int score, int level) {
        highScoreTextView.setText(getResources().getString(R.string.high_score) + " " + sharedPreferences.getInt(HIGH_SCORE, 0));
        currentScoreTextView.setText(getResources().getString(R.string.score) + " " + score);
        levelTextView.setText(getResources().getString(R.string.level) + " " + level);
    }

    @Override
    public void newHighScoreCheck(int score) {
        if(score > sharedPreferences.getInt(HIGH_SCORE, 0)){
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt(HIGH_SCORE, score);
            editor.apply();
        }
    }

}
