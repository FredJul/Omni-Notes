package net.fred.taskgame.hero.views;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.Property;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import net.fred.taskgame.hero.R;
import net.fred.taskgame.hero.databinding.ViewCreatureCardBinding;
import net.fred.taskgame.hero.databinding.ViewCreatureCardLargeBinding;
import net.fred.taskgame.hero.databinding.ViewSupportCardBinding;
import net.fred.taskgame.hero.databinding.ViewSupportCardLargeBinding;
import net.fred.taskgame.hero.models.Card;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class GameCardView extends FrameLayout {

    private ViewCreatureCardBinding mCreatureDataBinding;
    private ViewSupportCardBinding mSupportDataBinding;
    private ViewCreatureCardLargeBinding mCreatureLargeDataBinding;
    private ViewSupportCardLargeBinding mSupportLargeDataBinding;

    private Card mCard;

    @BindView(R.id.attack)
    TextView mAttackTextView;
    @BindView(R.id.defense)
    TextView mDefenseTextView;

    public GameCardView(Context context) {
        super(context);
        inflateView(context, null, 0);
    }

    public GameCardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        inflateView(context, attrs, 0);
    }

    public GameCardView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        inflateView(context, attrs, defStyle);
    }

    private void inflateView(Context context, AttributeSet attrs, int defStyle) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.GameCardView, defStyle, 0);
        boolean useLargeLayout = a.getBoolean(R.styleable.GameCardView_useLargeLayout, false);
        a.recycle();

        if (!isInEditMode()) {
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            if (!useLargeLayout) {
                mCreatureDataBinding = DataBindingUtil.inflate(inflater, R.layout.view_creature_card, this, true);
                mSupportDataBinding = DataBindingUtil.inflate(inflater, R.layout.view_support_card, this, true);

                ButterKnife.bind(this, mCreatureDataBinding.getRoot());
            } else {
                mCreatureLargeDataBinding = DataBindingUtil.inflate(inflater, R.layout.view_creature_card_large, this, true);
                mSupportLargeDataBinding = DataBindingUtil.inflate(inflater, R.layout.view_support_card_large, this, true);

                ButterKnife.bind(this, mCreatureLargeDataBinding.getRoot());
            }
            getChildAt(1).setVisibility(View.GONE);
        }
    }

    public void setCard(Card card) {
        mCard = card;
        if (card == null) {
            setVisibility(INVISIBLE);
        } else if (card.type == Card.Type.CREATURE) {
            setVisibility(VISIBLE);
            getChildAt(0).setVisibility(VISIBLE);
            getChildAt(1).setVisibility(GONE);
            if (mCreatureDataBinding != null) {
                mCreatureDataBinding.setCard(card);
            } else {
                mCreatureLargeDataBinding.setCard(card);
            }
        } else {
            setVisibility(VISIBLE);
            getChildAt(0).setVisibility(GONE);
            getChildAt(1).setVisibility(VISIBLE);
            if (mSupportDataBinding != null) {
                mSupportDataBinding.setCard(card);
            } else {
                mSupportLargeDataBinding.setCard(card);
            }
        }
    }

    public Card getCard() {
        return mCard;
    }

    public boolean animateValueChange(final Runnable endAction) {
        final Property<TextView, Integer> property = new Property<TextView, Integer>(int.class, "textColor") {
            @Override
            public Integer get(TextView object) {
                return object.getCurrentTextColor();
            }

            @Override
            public void set(TextView object, Integer value) {
                object.setTextColor(value);
            }
        };

        List<Animator> animators = new ArrayList<>();

        int previousAttack = Integer.valueOf(mAttackTextView.getText().toString());
        if (previousAttack > mCard.attack || previousAttack < mCard.attack) {
            mAttackTextView.setText(String.valueOf(mCard.attack));
            ObjectAnimator colorAnim = ObjectAnimator.ofInt(mAttackTextView, property, previousAttack > mCard.attack ? Color.RED : Color.GREEN);
            colorAnim.setEvaluator(new ArgbEvaluator());
            colorAnim.setRepeatCount(1);
            colorAnim.setRepeatMode(ObjectAnimator.REVERSE);
            ObjectAnimator scaleXAnim = ObjectAnimator.ofFloat(mAttackTextView, "scaleX", 1.5f);
            scaleXAnim.setRepeatCount(1);
            scaleXAnim.setRepeatMode(ObjectAnimator.REVERSE);
            ObjectAnimator scaleYAnim = ObjectAnimator.ofFloat(mAttackTextView, "scaleY", 1.5f);
            scaleYAnim.setRepeatCount(1);
            scaleYAnim.setRepeatMode(ObjectAnimator.REVERSE);

            animators.add(colorAnim);
            animators.add(scaleXAnim);
            animators.add(scaleYAnim);
        }

        int previousDefense = Integer.valueOf(mDefenseTextView.getText().toString());
        if (previousDefense > mCard.defense || previousDefense < mCard.defense) {
            mDefenseTextView.setText(String.valueOf(mCard.defense));
            ObjectAnimator colorAnim = ObjectAnimator.ofInt(mDefenseTextView, property, previousDefense > mCard.defense ? Color.RED : Color.GREEN);
            colorAnim.setEvaluator(new ArgbEvaluator());
            colorAnim.setRepeatCount(1);
            colorAnim.setRepeatMode(ObjectAnimator.REVERSE);
            ObjectAnimator scaleXAnim = ObjectAnimator.ofFloat(mDefenseTextView, "scaleX", 1.5f);
            scaleXAnim.setRepeatCount(1);
            scaleXAnim.setRepeatMode(ObjectAnimator.REVERSE);
            ObjectAnimator scaleYAnim = ObjectAnimator.ofFloat(mDefenseTextView, "scaleY", 1.5f);
            scaleYAnim.setRepeatCount(1);
            scaleYAnim.setRepeatMode(ObjectAnimator.REVERSE);

            animators.add(colorAnim);
            animators.add(scaleXAnim);
            animators.add(scaleYAnim);
        }

        if (!animators.isEmpty()) {
            AnimatorSet animSet = new AnimatorSet();
            if (endAction != null) {
                animSet.addListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animator) {
                    }

                    @Override
                    public void onAnimationEnd(Animator animator) {
                        endAction.run();
                    }

                    @Override
                    public void onAnimationCancel(Animator animator) {
                    }

                    @Override
                    public void onAnimationRepeat(Animator animator) {
                    }
                });
            }
            animSet.playTogether(animators);
            animSet.start();
            return true;
        }
        return false;
    }
}
