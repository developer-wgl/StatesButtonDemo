package com.demo;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.appcompat.widget.AppCompatTextView;

import java.io.Serializable;
import java.util.HashMap;

/**
 * Create on 2019/12/11
 * email:forguangliang@gmail.com
 * <p>
 * 多状态可显示进度按钮
 */
public class StatesButton extends AppCompatTextView {
    private static final String TAG = StatesButton.class.getSimpleName();

    private int mState = 0;
    private HashMap<Integer, StateStyle> states;

    private Paint mBackgroundPaint;
    private Paint mBackgroundBorderPaint;

    private float mProgress = -1;
    private float mToProgress;
    private float mProgressPercent;
    private RectF mBackgroundBounds;
    private ValueAnimator mProgressAnimation;

    public StatesButton(Context context) {
        this(context, null);
    }

    public StatesButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StatesButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (!isInEditMode()) {
            initData();
            setupAnimations();
        }
    }

    public void setState(int state) {
        setState(state, null);
    }

    public void setState(int state, String text) {
        if (checkContainsState(state)) return;

        if (mState != state) {
            mState = state;
            if (!getStateStyleCurrent().canShowCover()
                    && !getStateStyleCurrent().isKeepPreviousProgress()) {
                mProgress = mToProgress = getStateStyleCurrent().getStartProgress();
            }
        }

        if (mProgressAnimation.getDuration() != getStateStyleCurrent().getProgressAnimDuration()) {
            mProgressAnimation.setDuration(getStateStyleCurrent().getProgressAnimDuration());
        }

        if (getText().equals(text)) return;

        if (!TextUtils.isEmpty(text)) {
            getStateStyleCurrent().setText(text);
            setText(text);
        } else {
            setText(getStateStyleCurrent().getText());
        }
    }

    public int getState() {
        return mState;
    }

    public boolean addStateStyle(int state, StateStyle stateStyle) {
        if (states.containsKey(state)) return false;
        states.put(state, stateStyle);
        return true;
    }

    public StateStyle getStateStyleCurrent() {
        return states.get(mState);
    }

    public StateStyle getStateStyle(int state) {
        return states.get(state);
    }

    public boolean containsStateStyle(int state) {
        return states.containsKey(state);
    }

    public void setProgress(float progress) {
        if (checkContainsState(mState)
                || progress < getStateStyleCurrent().getStartProgress()
                || progress < mToProgress) {
            Log.e(TAG, "Cannot show progress bar!");
            return;
        }
        mToProgress = Math.min(progress, getStateStyleCurrent().getEndProgress());
        if (mProgressAnimation.isRunning()) {
            mProgressAnimation.end();
            mProgressAnimation.start();
        } else {
            mProgressAnimation.start();
        }
    }

    public float getProgress() {
        return mProgress;
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        return new SavedState(superState, (int) mProgress, mState, getText(), states);
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        mState = ss.state;
        mProgress = ss.progress;
        setText(ss.currentText);
        states = ss.stateHolders;
    }

    @SuppressLint({"UseSparseArrays", "ObsoleteSdkInt"})
    private void initData() {
        states = new HashMap<>();
        mProgress = 0;
        mBackgroundPaint = new Paint();
        mBackgroundPaint.setAntiAlias(true);
        mBackgroundPaint.setStyle(Paint.Style.FILL);
        mBackgroundBorderPaint = new Paint();
        mBackgroundBorderPaint.setAntiAlias(true);
        mBackgroundBorderPaint.setStyle(Paint.Style.STROKE);
        getPaint().setAntiAlias(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            //解决文字有时候画不出问题
            setLayerType(LAYER_TYPE_SOFTWARE, getPaint());
        }
    }

    @SuppressLint("SetTextI18n")
    private void setupAnimations() {
        mProgressAnimation = ValueAnimator.ofFloat(0, 1).setDuration(new StateStyle().getProgressAnimDuration());
        mProgressAnimation.addUpdateListener(animation -> {
            float timePercent = (float) animation.getAnimatedValue();
            mProgress = ((mToProgress - mProgress) * timePercent + mProgress);

            if (StatesButton.this.checkContainsState(mState)
                    || !getStateStyleCurrent().canShowCover()) return;

            if (getStateStyleCurrent().getProgressUIListener() != null) {
                setText(getStateStyleCurrent().getProgressUIListener().getText(mProgress));
            } else if (getStateStyleCurrent().isAddTextProgress()) {
                setText((getStateStyleCurrent().getText() + (int) mProgress) + "%");
            } else {
                setText(getStateStyleCurrent().getText());
            }
        });

        mProgressAnimation.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                if (mToProgress < mProgress) {
                    mProgress = mToProgress;
                }
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                //  What might be done in the future?
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                //  What might be done in the future?
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
                //  What might be done in the future?
            }
        });
    }

    private boolean checkContainsState(int state) {
        if (!states.containsKey(state)) {
            Log.e(TAG, "State check: ", new Exception("Not such state: " + state));
            return true;
        }
        return false;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (!isInEditMode() && checkContainsState(mState)) return;

        StateStyle stateStyle = states.get(mState);
        assert stateStyle != null;
        if (stateStyle.canShowCover()) {
            drawBackground(canvas, stateStyle.getBgColor()
                    , stateStyle.getBgCoverColor(), stateStyle.getRadius(), stateStyle.getBorderWidth());
            drawTextAbove(canvas, stateStyle.getTextColor(),
                    stateStyle.getTextCoverColor(), stateStyle.getBorderWidth());
        } else {
            drawBackground(canvas, stateStyle.getBgColor()
                    , stateStyle.getBgColor(), stateStyle.getRadius(), stateStyle.getBorderWidth());
            drawTextAbove(canvas, stateStyle.getTextColor(),
                    stateStyle.getTextColor(), stateStyle.getBorderWidth());
        }

        if (stateStyle.getBorderColor() != Color.TRANSPARENT) {
            drawButtonBorder(canvas, stateStyle.getBorderColor()
                    , stateStyle.getRadius(), stateStyle.getBorderWidth());
        }
    }

    private void drawBackground(Canvas canvas, int rightColor, int leftColor, float mRadius, float mStrokeWidth) {
        if (mBackgroundBounds == null) {
            mBackgroundBounds = new RectF();
            if (mRadius == 0) {
                mRadius = getMeasuredHeight() >> 1;
            }
            mBackgroundBounds.left = mStrokeWidth;
            mBackgroundBounds.top = mStrokeWidth;
            mBackgroundBounds.right = getMeasuredWidth() - mStrokeWidth;
            mBackgroundBounds.bottom = getMeasuredHeight() - mStrokeWidth;
        }

        if (leftColor != rightColor) {
            mProgressPercent = mProgress / (getStateStyleCurrent().getEndProgress() + 0f);
            LinearGradient mProgressBgGradient = new LinearGradient(mStrokeWidth,
                    0, getMeasuredWidth() - mStrokeWidth, 0,
                    new int[]{leftColor, rightColor},
                    new float[]{mProgressPercent, mProgressPercent + 0.001f},
                    Shader.TileMode.CLAMP
            );
            mBackgroundPaint.setColor(leftColor);
            mBackgroundPaint.setShader(mProgressBgGradient);
            canvas.drawRoundRect(mBackgroundBounds, mRadius, mRadius, mBackgroundPaint);
        } else {
            mBackgroundPaint.setColor(rightColor);
            mBackgroundPaint.setShader(null);
            canvas.drawRoundRect(mBackgroundBounds, mRadius, mRadius, mBackgroundPaint);
        }
    }

    private void drawButtonBorder(Canvas canvas, int color, float mRadius, float mStrokeWidth) {
        mBackgroundBorderPaint.setColor(color);
        mBackgroundBorderPaint.setStrokeWidth(mStrokeWidth);
        canvas.drawRoundRect(mBackgroundBounds, mRadius, mRadius, mBackgroundBorderPaint);//绘制边框
    }

    private void drawTextAbove(Canvas canvas, int uncoverColor, int coverColor, float mStrokeWidth) {
        getPaint().setTextSize(getTextSize());
        final float y = (canvas.getHeight() >> 1) - (getPaint().descent() / 2 + getPaint().ascent() / 2);
        final float textWidth = getPaint().measureText(getText().toString());

        if (uncoverColor == coverColor) {
            getPaint().setShader(null);
            getPaint().setColor(uncoverColor);
            canvas.drawText(getText().toString(), (getMeasuredWidth() - textWidth) / 2, y, getPaint());
        } else {
            float w = getMeasuredWidth() - 2 * mStrokeWidth;
            //进度条压过距离
            float coverlength = w * mProgressPercent;
            //开始渐变指示器
            float indicator1 = w / 2 - textWidth / 2;
            //结束渐变指示器
            float indicator2 = w / 2 + textWidth / 2;
            //文字变色部分的距离
            float coverTextLength = textWidth / 2 - w / 2 + coverlength;
            float textProgress = coverTextLength / textWidth;
            if (coverlength <= indicator1) {
                getPaint().setShader(null);
                getPaint().setColor(uncoverColor);
            } else if (coverlength <= indicator2) {
                LinearGradient mProgressTextGradient = new LinearGradient((w - textWidth) / 2 + mStrokeWidth, 0,
                        (w + textWidth) / 2 + mStrokeWidth, 0,
                        new int[]{coverColor, uncoverColor},
                        new float[]{textProgress, textProgress + 0.001f},
                        Shader.TileMode.CLAMP);
                getPaint().setColor(uncoverColor);
                getPaint().setShader(mProgressTextGradient);
            } else {
                getPaint().setShader(null);
                getPaint().setColor(coverColor);
            }
            canvas.drawText(getText().toString(), (w - textWidth) / 2 + mStrokeWidth, y, getPaint());
        }
    }

    public static class SavedState extends View.BaseSavedState {
        private int progress;
        private int state;
        private CharSequence currentText;
        private HashMap<Integer, StateStyle> stateHolders;

        private SavedState(Parcelable parcel, int progress, int state,
                           CharSequence currentText, HashMap<Integer, StateStyle> stateHolders) {
            super(parcel);
            this.progress = progress;
            this.state = state;
            this.currentText = currentText;
            this.stateHolders = stateHolders;
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(progress);
            out.writeInt(state);
            out.writeString(currentText.toString());
            out.writeSerializable(stateHolders);
        }
    }

    public static class StateStyle implements Serializable {
        private int borderColor;
        private float borderWidth = 2F;
        private int bgColor;
        private String text;
        private int textColor;
        private float radius = 25;

        private int bgCoverColor;
        private int textCoverColor;
        private int startProgress = 0;
        private int endProgress = 100;
        private long progressAnimDuration = 500;
        private boolean isKeepPreviousProgress = false;
        private boolean isAddTextProgress = true;
        private TextByProgressUI textByProgress = null;

        public StateStyle() {
        }

        public StateStyle setBorderColor(int borderColor) {
            this.borderColor = borderColor;
            return this;
        }

        public StateStyle setBgColor(int bgColor) {
            this.bgColor = bgCoverColor = bgColor;
            return this;
        }

        public StateStyle setBgColor(int bgColor, int bgCoverColor) {
            this.bgColor = bgColor;
            this.bgCoverColor = bgCoverColor;
            return this;
        }

        public StateStyle setTextColor(int textColor) {
            this.textColor = textCoverColor = textColor;
            return this;
        }

        public StateStyle setTextColor(int textColor, int textCoverColor) {
            this.textColor = textColor;
            this.textCoverColor = textCoverColor;
            return this;
        }

        public StateStyle setText(String text) {
            this.text = text;
            this.textByProgress = null;
            this.isAddTextProgress = false;
            return this;
        }

        public StateStyle setText(String text, boolean isAddTextProgress) {
            this.text = text;
            this.isAddTextProgress = isAddTextProgress;
            this.textByProgress = null;
            return this;
        }

        public StateStyle setText(TextByProgressUI listener) {
            this.textByProgress = listener;
            return this;
        }

        public StateStyle setBorderWidth(float borderWidth) {
            this.borderWidth = borderWidth;
            return this;
        }

        public StateStyle setRadius(float radius) {
            this.radius = radius;
            return this;
        }

        public int getBgColor() {
            return bgColor;
        }

        public int getBgCoverColor() {
            return bgCoverColor;
        }

        public int getBorderColor() {
            return borderColor;
        }

        public int getTextColor() {
            return textColor;
        }

        public int getTextCoverColor() {
            return textCoverColor;
        }

        public String getText() {
            return text == null ? "" : text;
        }

        public float getBorderWidth() {
            return borderWidth;
        }

        public float getRadius() {
            return radius;
        }

        public int getEndProgress() {
            return endProgress;
        }

        public StateStyle setEndProgress(int endProgress) {
            this.endProgress = endProgress;
            return this;
        }

        public int getStartProgress() {
            return startProgress;
        }

        public StateStyle setStartProgress(int startProgress) {
            this.startProgress = startProgress;
            return this;
        }

        public long getProgressAnimDuration() {
            return progressAnimDuration;
        }

        public StateStyle setProgressAnimDuration(long progressAnimDuration) {
            this.progressAnimDuration = progressAnimDuration;
            return this;
        }

        public boolean isKeepPreviousProgress() {
            return isKeepPreviousProgress;
        }

        public StateStyle setKeepPreviousProgress(boolean keepPreviousProgress) {
            isKeepPreviousProgress = keepPreviousProgress;
            return this;
        }

        public boolean isAddTextProgress() {
            return isAddTextProgress;
        }

        public TextByProgressUI getProgressUIListener() {
            return textByProgress;
        }

        public boolean canShowCover() {
            return bgColor != bgCoverColor || textColor != textCoverColor;
        }

        public StateStyle cloneState() {
            return new StateStyle().setBorderColor(borderColor)
                    .setBgColor(bgColor, bgCoverColor)
                    .setText(text, isAddTextProgress).setText(textByProgress)
                    .setTextColor(textColor, textCoverColor)
                    .setRadius(radius).setBorderWidth(borderWidth).setStartProgress(startProgress)
                    .setEndProgress(endProgress).setKeepPreviousProgress(isKeepPreviousProgress)
                    .setProgressAnimDuration(progressAnimDuration);
        }
    }

    interface TextByProgressUI {
        String getText(float progress);
    }

}
