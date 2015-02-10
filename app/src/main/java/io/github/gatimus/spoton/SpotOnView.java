package io.github.gatimus.spoton;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.PixelFormat;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SpotOnView extends SurfaceView {


    private SpotOnViewListener spotOnViewListener;

    private int spotsTouched;
    private int score;
    private int level;
    private int viewWidth;
    private int viewHeight;
    private long animationTime;
    private boolean gameOver;
    private boolean gamePaused;
    private boolean dialogDisplayed;

    private final Queue<ImageView> spots = new ConcurrentLinkedQueue<ImageView>();
    private final Queue<Animator> animators = new ConcurrentLinkedQueue<Animator>();

    private LinearLayout livesLinearLayout;
    private RelativeLayout relativeLayout;
    private LayoutInflater layoutInflater;

    private static final int INITIAL_ANIMATION_DURATION = 6000;
    private static final Random random = new Random();
    private static final int SPOT_DIAMETER = 100;
    private static final float SCALE_X = 0.25f;
    private static final float SCALE_Y = 0.25f;
    private static final int INITIAL_SPOTS = 5;
    private static final int SPOT_DELAY = 500;
    private static final int LIVES = 3;
    private static final int MAX_LIVES = 7;
    private static final int NEW_LEVEL = 10;
    private Handler spotHandler;

    private static final int HIT_SOUND_ID = 1;
    private static final int MISS_SOUND_ID = 2;
    private static final int DISAPPEAR_SOUND_ID = 3;
    private static final int SOUND_PRIORITY = 1;
    private static final int SOUND_QUALITY = 100;
    private static final int MAX_STREAMS = 4;
    private SoundPool soundPool;
    private int volume;
    private SparseIntArray soundMap;

    private SurfaceHolder holder;

    public SpotOnView(Activity activity, RelativeLayout parentLayout) {
        super(activity);
        try {
            spotOnViewListener = (SpotOnViewListener)activity;
        } catch (ClassCastException e) {
            Log.e(getClass().getSimpleName(), activity.toString() + " must implement SpotOnViewListener");
        }
        holder = getHolder();
        holder.addCallback(new SurfaceHolder.Callback(){
                               @Override
                               public void surfaceCreated(SurfaceHolder holder) {

                               }

                               @Override
                               public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                                   viewWidth = width;
                                   viewHeight = height;
                               }

                               @Override
                               public void surfaceDestroyed(SurfaceHolder holder) {

                               }
                           });
        holder.setFormat(PixelFormat.TRANSPARENT);
        layoutInflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        relativeLayout = parentLayout;
        livesLinearLayout = (LinearLayout) relativeLayout.findViewById(R.id.lifeLinearLayout);

        spotHandler = new Handler();
    }


    public void pause() {
        gamePaused = true;
        soundPool.release();
        soundPool = null;
        cancelAnimations();
    }


    private void cancelAnimations() {

        for (Animator animator : animators)
            animator.cancel();

        for (ImageView view : spots)
            relativeLayout.removeView(view);

        spotHandler.removeCallbacks(addSpotRunnable);
        animators.clear();
        spots.clear();
    }


    public void resume(Context context) {
        gamePaused = false;
        initializeSoundEffects(context);

        if (!dialogDisplayed)
            resetGame();
    }


    public void resetGame() {
        spots.clear();
        animators.clear();
        livesLinearLayout.removeAllViews();
        animationTime = INITIAL_ANIMATION_DURATION;
        spotsTouched = 0;
        score = 0;
        level = 1;
        gameOver = false;
        spotOnViewListener.displayScores(score, level);

        for (int i = 0; i < LIVES; i++) {

            livesLinearLayout.addView(
                    (ImageView) layoutInflater.inflate(R.layout.life, null));
        }

        for (int i = 1; i <= INITIAL_SPOTS; ++i)
            spotHandler.postDelayed(addSpotRunnable, i * SPOT_DELAY);
    }


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void initializeSoundEffects(Context context) {

        if(Build.VERSION.SDK_INT<Build.VERSION_CODES.LOLLIPOP){
            soundPool = new SoundPool(MAX_STREAMS, AudioManager.STREAM_MUSIC, SOUND_QUALITY);
        } else {
            SoundPool.Builder spBuilder = new SoundPool.Builder();
            spBuilder.setMaxStreams(MAX_STREAMS);
            spBuilder.setAudioAttributes(new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setFlags(AudioAttributes.FLAG_HW_AV_SYNC)
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .build());
            soundPool = spBuilder.build();
        }

        AudioManager manager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        volume = manager.getStreamVolume(AudioManager.STREAM_MUSIC);

        soundMap = new SparseIntArray();
        soundMap.put(HIT_SOUND_ID, soundPool.load(context, R.raw.hit, SOUND_PRIORITY));
        soundMap.put(MISS_SOUND_ID, soundPool.load(context, R.raw.miss, SOUND_PRIORITY));
        soundMap.put(DISAPPEAR_SOUND_ID, soundPool.load(context, R.raw.disappear, SOUND_PRIORITY));
    }

    private Runnable addSpotRunnable = new Runnable() {
        public void run() {
            addNewSpot();
        }
    };


    public void addNewSpot() {

        int x = random.nextInt(viewWidth - SPOT_DIAMETER);
        int y = random.nextInt(viewHeight - SPOT_DIAMETER);
        int x2 = random.nextInt(viewWidth - SPOT_DIAMETER);
        int y2 = random.nextInt(viewHeight - SPOT_DIAMETER);


        final ImageView spot = (ImageView) layoutInflater.inflate(R.layout.untouched, null);
        spots.add(spot);
        spot.setLayoutParams(new RelativeLayout.LayoutParams(SPOT_DIAMETER, SPOT_DIAMETER));
        spot.setImageResource(random.nextInt(2) == 0 ? R.drawable.green_spot : R.drawable.red_spot);
        spot.setX(x);
        spot.setY(y);
        spot.setOnClickListener(
                new OnClickListener() {
                    public void onClick(View v) {
                        touchedSpot(spot);
                    }
                }
        );
        relativeLayout.addView(spot);


        spot.animate().x(x2).y(y2).scaleX(SCALE_X).scaleY(SCALE_Y).setDuration(animationTime).setListener(
                new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        animators.add(animation);
                    }

                    public void onAnimationEnd(Animator animation) {
                        animators.remove(animation);

                        if (!gamePaused && spots.contains(spot)) {
                            missedSpot(spot);
                        }
                    }
                }
        );
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if (soundPool != null)
            soundPool.play(MISS_SOUND_ID, volume, volume, SOUND_PRIORITY, 0, 1f);

        score -= 15 * level;
        score = Math.max(score, 0);
        spotOnViewListener.displayScores(score, level);
        return true;
    }


    private void touchedSpot(ImageView spot) {
        relativeLayout.removeView(spot);
        spots.remove(spot);

        ++spotsTouched;
        score += 10 * level;


        if (soundPool != null)
            soundPool.play(HIT_SOUND_ID, volume, volume, SOUND_PRIORITY, 0, 1f);


        if (spotsTouched % NEW_LEVEL == 0) {
            ++level;
            animationTime *= 0.95;


            if (livesLinearLayout.getChildCount() < MAX_LIVES) {
                ImageView life =
                        (ImageView) layoutInflater.inflate(R.layout.life, null);
                livesLinearLayout.addView(life);
            }
        }

        spotOnViewListener.displayScores(score, level);

        if (!gameOver)
            addNewSpot();
    }


    public void missedSpot(ImageView spot) {
        spots.remove(spot);
        relativeLayout.removeView(spot);

        if (gameOver)
            return;


        if (soundPool != null)
            soundPool.play(DISAPPEAR_SOUND_ID, volume, volume, SOUND_PRIORITY, 0, 1f);


        if (livesLinearLayout.getChildCount() == 0) {
            gameOver = true;


            spotOnViewListener.newHighScoreCheck(score);

            cancelAnimations();


            Builder dialogBuilder = new AlertDialog.Builder(getContext());
            dialogBuilder.setTitle(R.string.game_over);
            dialogBuilder.setMessage(getResources().getString(R.string.score) +  " " + score);
            dialogBuilder.setCancelable(false);
            dialogBuilder.setPositiveButton(R.string.reset_game,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            spotOnViewListener.displayScores(score, level);
                            dialogDisplayed = false;
                            resetGame();
                        }
                    }
            );
            dialogDisplayed = true;
            dialogBuilder.show();
        } else {
            livesLinearLayout.removeViewAt(
                    livesLinearLayout.getChildCount() - 1);
            addNewSpot();
        }
    }

    public interface SpotOnViewListener {

        public void displayScores(int score, int level);

        public void newHighScoreCheck(int score);

    }

}
