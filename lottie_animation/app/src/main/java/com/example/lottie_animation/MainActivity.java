package com.example.lottie_animation;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.airbnb.lottie.LottieAnimationView;


public class MainActivity extends AppCompatActivity {

    LottieAnimationView animationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        animationView = findViewById(R.id.lottie_view);

//        animationView.setRepeatCount(LottieAnimationView.INFINITE);// Optional: plays automatically if lottie:autoPlay="true"
        animationView.playAnimation();
    }
}
