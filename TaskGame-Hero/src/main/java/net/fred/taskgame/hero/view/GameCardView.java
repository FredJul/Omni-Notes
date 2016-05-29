package net.fred.taskgame.hero.view;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import net.fred.taskgame.hero.R;
import net.fred.taskgame.hero.databinding.CreatureCardViewBinding;
import net.fred.taskgame.hero.databinding.SupportCardViewBinding;
import net.fred.taskgame.hero.model.Card;

public class GameCardView extends FrameLayout {

    private CreatureCardViewBinding mCreatureDataBinding;
    private SupportCardViewBinding mSupportDataBinding;

    public GameCardView(Context context) {
        super(context);
        inflateView();
    }

    public GameCardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        inflateView();
    }

    public GameCardView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        inflateView();
    }

    private void inflateView() {
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (!isInEditMode()) {
            mCreatureDataBinding = DataBindingUtil.inflate(inflater, R.layout.creature_card_view, this, true);
            mSupportDataBinding = DataBindingUtil.inflate(inflater, R.layout.support_card_view, this, true);
            getChildAt(1).setVisibility(View.GONE);
        }
    }

    public void setCard(Card card) {
        if (card == null) {
            setVisibility(INVISIBLE);
        } else if (card.type == Card.Type.CREATURE) {
            setVisibility(VISIBLE);
            getChildAt(0).setVisibility(VISIBLE);
            getChildAt(1).setVisibility(GONE);
            mCreatureDataBinding.setCard(card);
        } else {
            setVisibility(VISIBLE);
            getChildAt(0).setVisibility(GONE);
            getChildAt(1).setVisibility(VISIBLE);
            mSupportDataBinding.setCard(card);
        }
    }
}
