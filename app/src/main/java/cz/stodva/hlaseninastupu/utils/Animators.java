package cz.stodva.hlaseninastupu.utils;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.view.View;
import android.view.animation.LinearInterpolator;


public class Animators {

    public static void animateButtonClick(View view, boolean alpha) {

        ObjectAnimator objectAnimatorAlpha = ObjectAnimator.ofFloat(view, View.ALPHA, 0.3f, 1f);
        objectAnimatorAlpha.setInterpolator(new LinearInterpolator());

        ObjectAnimator objectAnimatorScaleX = ObjectAnimator.ofFloat(view, View.SCALE_X, 0.98f, 1.02f, 1f);
        objectAnimatorScaleX.setInterpolator(new LinearInterpolator());
        objectAnimatorScaleX.setRepeatMode(ObjectAnimator.REVERSE);

        ObjectAnimator objectAnimatorScaleY = ObjectAnimator.ofFloat(view, View.SCALE_Y, 0.98f, 1.02f, 1f);
        objectAnimatorScaleY.setInterpolator(new LinearInterpolator());
        objectAnimatorScaleY.setStartDelay(50);
        objectAnimatorScaleY.setRepeatMode(ObjectAnimator.REVERSE);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.setDuration(200);

        if (alpha) animatorSet.playTogether(objectAnimatorAlpha, objectAnimatorScaleX, objectAnimatorScaleY);
        else animatorSet.playTogether(objectAnimatorScaleX, objectAnimatorScaleY);

        animatorSet.start();
    }

    public static void showViewSmoothly(final View view) {
        if (view == null) return;
        view.animate().alpha(1f).setDuration(400).start();
    }

    public static void hideViewSmoothly(final View view) {
        if (view == null) return;
        view.animate().alpha(0f).setDuration(400).start();
    }
}
