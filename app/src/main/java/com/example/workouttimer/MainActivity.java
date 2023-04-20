package com.example.workouttimer;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.TaskStackBuilder;

public class MainActivity extends AppCompatActivity {

    private EditText workoutDurationEditText, restDurationEditText, numberOfSets;
    private TextView timerTextView, timerLabel;
    private Button startStopButton;
    private int setCount;
    private ProgressBar progressBar;
    private CountDownTimer workoutTimer, restTimer;
    private boolean isWorkoutRunning = false, isRestRunning = false;
    private long workoutTimeRemaining, restTimeRemaining;
    private Vibrator vibrator;
    private MediaPlayer toner;


    @SuppressLint({"MissingInflatedId", "CutPasteId"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        workoutDurationEditText = findViewById(R.id.workout_duration_edit_text);
        restDurationEditText = findViewById(R.id.rest_duration_edit_text);
        numberOfSets = findViewById(R.id.number_of_sets);
        timerTextView = findViewById(R.id.time_remaining_label);
        timerLabel = findViewById(R.id.timer_label);
        startStopButton = findViewById(R.id.start_stop_button);
        progressBar = findViewById(R.id.progress_bar);
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        toner = MediaPlayer.create(this, R.raw.beep);

        NotificationChannel channel = new NotificationChannel("MyNotificationChannel", "Workout Timer", NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("Notifications for the Workout Timer app");
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);

        Intent intent = getIntent();
        if (intent != null && "STOP_TIMER".equals(intent.getAction())) {
            stopCurrentPhase();
        }

        startStopButton.setOnClickListener(v -> {
            if (numberOfSets.getText().toString().equals(""))
                numberOfSets.setError("Please enter number of sets");
            else if (workoutDurationEditText.getText().toString().equals(""))
                workoutDurationEditText.setError("Please enter workout time");
            else if (restDurationEditText.getText().toString().equals(""))
                restDurationEditText.setError("Please enter rest time");
            else if (!isWorkoutRunning && !isRestRunning) {
                startWorkout();
            } else stopCurrentPhase();
        });
    }

    @SuppressLint("SetTextI18n")
    private void startWorkout() {
        long workoutDuration = Long.parseLong(workoutDurationEditText.getText().toString());
        timerLabel.setText("Workout is in progress");
        workoutTimer = new CountDownTimer(workoutDuration * 1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                workoutTimeRemaining = millisUntilFinished;
                updateTimerUI();
                updateProgressBar();
            }

            @Override
            public void onFinish() {
                toner.start();
                vibrator.vibrate(VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE));
                startRest();
            }
        }.start();
        isWorkoutRunning = true;
        isRestRunning = false;
        startStopButton.setText("Stop");
    }

    @SuppressLint("SetTextI18n")
    private void startRest() {
        long restDuration = Long.parseLong(restDurationEditText.getText().toString());
        showNotification("Workout time is over. Time for some rest before the next set!");
        restTimer = new CountDownTimer(restDuration * 1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                restTimeRemaining = millisUntilFinished;
                updateTimerUI();
                updateProgressBar();
            }

            @Override
            public void onFinish() {
                toner.start();
                vibrator.vibrate(VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE));
                setCount = setCount + 1;
                if (setCount < Integer.parseInt(numberOfSets.getText().toString())) {
                    showNotification("Rest time is over. Time for the next set!");
                    startWorkout();
                } else {
                    showNotification("Workout Session has been completed!");
                    startStopButton.setText("Start");
                    timerLabel.setText("Workout Session has been completed");
                }
            }
        }.start();
        isRestRunning = true;
        isWorkoutRunning = false;
        startStopButton.setText("Stop");
    }

    @SuppressLint("SetTextI18n")
    private void stopCurrentPhase() {
        if (isWorkoutRunning) {
            workoutTimer.cancel();
            isWorkoutRunning = false;
        } else if (isRestRunning) {
            restTimer.cancel();
            isRestRunning = false;
        }

        workoutTimeRemaining = 0;
        restTimeRemaining = 0;
        updateTimerUI();
        updateProgressBar();
        startStopButton.setText("Start");
    }

    @SuppressLint("DefaultLocale")
    private void updateTimerUI() {
        long timeRemaining = 0;
        if (isWorkoutRunning) timeRemaining = workoutTimeRemaining;
        else if (isRestRunning)
            timeRemaining = restTimeRemaining;
        int minutes = (int) (timeRemaining / 1000) / 60;
        int seconds = (int) (timeRemaining / 1000) % 60;
        timerTextView.setText(String.format("%02d:%02d", minutes, seconds));
    }

    private void updateProgressBar() {
        long totalTime = isWorkoutRunning ? Long.parseLong(workoutDurationEditText.getText().toString())
                : Long.parseLong(restDurationEditText.getText().toString());
        long currentTimeRemaining = isWorkoutRunning ? workoutTimeRemaining / 1000 : restTimeRemaining / 1000;
        int progress = (int) (((totalTime - currentTimeRemaining) * 100) / totalTime);
        progressBar.setProgress(progress);
    }

    @SuppressLint("MissingPermission")
    private void showNotification(String message) {
        showNotification(message, "Stop Timer");
    }

    @SuppressLint("MissingPermission")
    private void showNotification(String message,String title ) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "MyNotificationChannel")
                .setSmallIcon(R.drawable.my_app_logo)
                .setContentTitle("Workout Timer")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(PendingIntent.getActivity(this, 0, new Intent(), PendingIntent.FLAG_UPDATE_CURRENT))
                .setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_ALL);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(1, builder.build());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isWorkoutRunning) {
            workoutTimer.cancel();
        }
        if (isRestRunning) {
            restTimer.cancel();
        }
        showNotification("Make sure to do other sets after rest!", "Open App");
        vibrator.cancel();
    }
}

