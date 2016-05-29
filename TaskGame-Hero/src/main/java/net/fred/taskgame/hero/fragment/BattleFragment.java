/*
 * Copyright (C) 2015 Federico Iosue (federico.iosue@gmail.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.fred.taskgame.hero.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import net.fred.taskgame.hero.R;
import net.fred.taskgame.hero.activity.MainActivity;
import net.fred.taskgame.hero.logic.BattleManager;
import net.fred.taskgame.hero.model.Card;
import net.fred.taskgame.hero.utils.UiUtils;
import net.fred.taskgame.hero.view.GameCardView;

import org.parceler.Parcels;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class BattleFragment extends Fragment {

    public static final String STATE_BATTLE_MANAGER = "STATE_BATTLE_MANAGER";
    @BindView(R.id.player_card)
    GameCardView mPlayerCardView;

    @BindView(R.id.enemy_card)
    GameCardView mEnemyCardView;

    @BindView(R.id.play_button)
    Button mPlayButton;

    @BindView(R.id.card_list)
    LinearLayout mCardListLayout;

    private BattleManager mBattleManager = new BattleManager();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_detail, container, false);
        ButterKnife.bind(this, layout);

        if (savedInstanceState != null) {
            mBattleManager = Parcels.unwrap(savedInstanceState.getParcelable(STATE_BATTLE_MANAGER));
        } else {
            mBattleManager.addEnemyCard(Card.getAllCardsMap().get(Card.CREATURE_ORC_ARCHER).clone());
            mBattleManager.addEnemyCard(Card.getAllCardsMap().get(Card.CREATURE_SKELETON_ARCHER).clone());

            mBattleManager.addPlayerCard(Card.getAllCardsMap().get(Card.CREATURE_SKELETON_ARCHER).clone());
            mBattleManager.addPlayerCard(Card.getAllCardsMap().get(Card.CREATURE_ORC_ARCHER).clone());
            mBattleManager.addPlayerCard(Card.getAllCardsMap().get(Card.SUPPORT_WEAPON_EROSION).clone());
            mBattleManager.addPlayerCard(Card.getAllCardsMap().get(Card.SUPPORT_POWER_POTION).clone());
        }

        updateUI();

        return layout;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(STATE_BATTLE_MANAGER, Parcels.wrap(mBattleManager));
        super.onSaveInstanceState(outState);
    }

    @OnClick(R.id.play_button)
    public void onPlayClicked() {
        mBattleManager.play();

        updateUI();
    }

    private void updateUI() {
        boolean isPlayerCreatureStillAlive = mBattleManager.isPlayerCreatureStillAlive();
        mPlayButton.setVisibility(isPlayerCreatureStillAlive ? View.VISIBLE : View.GONE);
        mPlayerCardView.setCard(mBattleManager.getLastUsedPlayerCreatureCard());
        mEnemyCardView.setCard(mBattleManager.getCurrentOrNextAliveEnemyCreatureCard());

        mCardListLayout.removeAllViews();
        for (final Card card : isPlayerCreatureStillAlive ? mBattleManager.getRemainingPlayerSupportCards() : mBattleManager.getRemainingPlayerCharacterCards()) {
            final GameCardView cardView = new GameCardView(getContext());
            cardView.setCard(card);
            cardView.setPadding(10, 10, 10, 10);
            cardView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mCardListLayout.removeView(cardView);
                    mBattleManager.play(card);
                    updateUI();
                }
            });
            mCardListLayout.addView(cardView);
        }

        if (mBattleManager.getBattleStatus() == BattleManager.BattleStatus.DRAW) {
            UiUtils.showMessage(getActivity(), "Draw!");
        } else if (mBattleManager.getBattleStatus() == BattleManager.BattleStatus.ENEMY_WON) {
            UiUtils.showMessage(getActivity(), "Enemy won!");
        } else if (mBattleManager.getBattleStatus() == BattleManager.BattleStatus.PLAYER_WON) {
            UiUtils.showMessage(getActivity(), "Player won!");
        }
    }

    private MainActivity getMainActivity() {
        return (MainActivity) getActivity();
    }
}
