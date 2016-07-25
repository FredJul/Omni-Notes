package net.fred.taskgame.hero.logic;

import net.fred.taskgame.hero.models.Card;
import net.fred.taskgame.hero.utils.Dog;

import org.parceler.Parcel;

import java.util.ArrayList;
import java.util.List;

@Parcel
public class BattleManager {

    public enum BattleStep {APPLY_PLAYER_SUPPORT, APPLY_ENEMY_SUPPORT, FIGHT, APPLY_DAMAGES, PLAYER_DEATH, ENEMY_DEATH, END_TURN, PLAYER_WON, ENEMY_WON, DRAW}

    List<Card> mRemainingEnemyCards = new ArrayList<>();
    List<Card> mUsedEnemyCards = new ArrayList<>();
    List<Card> mRemainingPlayerCards = new ArrayList<>();
    List<Card> mUsedPlayerCards = new ArrayList<>();
    List<BattleStep> mSteps = new ArrayList<>();

    boolean mStunPlayer, mStunEnemy;

    public List<Card> getRemainingEnemyCards() {
        return mRemainingEnemyCards;
    }

    public List<Card> getRemainingPlayerCharacterCards() {
        List<Card> creatureCards = new ArrayList<>();
        for (Card card : mRemainingPlayerCards) {
            if (card.type == Card.Type.CREATURE) {
                creatureCards.add(card);
            }
        }
        return creatureCards;
    }

    public List<Card> getRemainingPlayerSupportCards() {
        List<Card> supportCards = new ArrayList<>();
        for (Card card : mRemainingPlayerCards) {
            if (card.type == Card.Type.SUPPORT) {
                supportCards.add(card);
            }
        }
        return supportCards;
    }

    public void addEnemyCards(List<Card> cards) {
        for (Card card : cards) {
            mRemainingEnemyCards.add(card.clone()); // We need to clone them to not modify the original data
        }
    }

    public void addPlayerCards(List<Card> cards) {
        for (Card card : cards) {
            mRemainingPlayerCards.add(card.clone()); // We need to clone them to not modify the original data
        }
    }

    public Card getNextPlayerCreatureCard() {
        for (Card card : mRemainingPlayerCards) {
            if (card.type == Card.Type.CREATURE) {
                return card;
            }
        }

        return null;
    }

    public Card getNextEnemyCreatureCard() {
        for (Card card : mRemainingEnemyCards) {
            if (card.type == Card.Type.CREATURE) {
                return card;
            }
        }

        return null;
    }

    public Card getLastUsedEnemySupportCard() {
        for (int i = mUsedEnemyCards.size() - 1; i >= 0; i--) {
            Card card = mUsedEnemyCards.get(i);
            if (card.type == Card.Type.SUPPORT) {
                return card;
            }
        }

        return null;
    }

    public Card getLastUsedEnemyCreatureCard() {
        for (int i = mUsedEnemyCards.size() - 1; i >= 0; i--) {
            Card card = mUsedEnemyCards.get(i);
            if (card.type == Card.Type.CREATURE) {
                return card;
            }
        }

        return null;
    }

    public Card getLastUsedEnemyCreatureCard(boolean fromEnemyPointOfView) {
        if (fromEnemyPointOfView) {
            return getLastUsedPlayerCreatureCard();
        } else {
            return getLastUsedEnemyCreatureCard();
        }
    }

    public Card getLastUsedPlayerCreatureCard() {
        for (int i = mUsedPlayerCards.size() - 1; i >= 0; i--) {
            Card card = mUsedPlayerCards.get(i);
            if (card.type == Card.Type.CREATURE) {
                return card;
            }
        }

        return null;
    }

    public Card getLastUsedPlayerCreatureCard(boolean fromEnemyPointOfView) {
        if (fromEnemyPointOfView) {
            return getLastUsedEnemyCreatureCard();
        } else {
            return getLastUsedPlayerCreatureCard();
        }
    }

    public Card getCurrentOrNextAliveEnemyCreatureCard() {
        if (isEnemyCreatureStillAlive()) {
            return getLastUsedEnemyCreatureCard();
        } else {
            return getNextEnemyCreatureCard();
        }
    }

    public boolean isPlayerCreatureStillAlive() {
        Card creatureCard = getLastUsedPlayerCreatureCard();
        return creatureCard != null && creatureCard.defense > 0;
    }

    public boolean isEnemyCreatureStillAlive() {
        Card creatureCard = getLastUsedEnemyCreatureCard();
        return creatureCard != null && creatureCard.defense > 0;
    }

    public void play() {
        if (!isEnemyCreatureStillAlive()) {
            Card enemy = getNextEnemyCreatureCard();
            mUsedEnemyCards.add(enemy);
            mRemainingEnemyCards.remove(enemy);
        } else {
            if (mRemainingEnemyCards.size() > 0 && mRemainingEnemyCards.get(0).type == Card.Type.SUPPORT) {
                mUsedEnemyCards.add(mRemainingEnemyCards.get(0));
                mRemainingEnemyCards.remove(0);
                mSteps.add(BattleStep.APPLY_ENEMY_SUPPORT);
            }
        }

        mSteps.add(BattleStep.FIGHT);
        mSteps.add(BattleStep.APPLY_DAMAGES);
    }

    public void play(Card card) {
        mRemainingPlayerCards.remove(card);
        mUsedPlayerCards.add(card);

        if (card.type == Card.Type.SUPPORT) {
            mSteps.add(BattleStep.APPLY_PLAYER_SUPPORT);
        }

        play();
    }

    public BattleStep getNextStep() {
        BattleStep step = mSteps.remove(0);
        switch (step) {
            case APPLY_PLAYER_SUPPORT: {
                Card.getAllCardsMap().get(mUsedPlayerCards.get(mUsedPlayerCards.size() - 1).id).supportAction.executeSupportAction(this, false);
                break;
            }
            case APPLY_ENEMY_SUPPORT: {
                Card.getAllCardsMap().get(mUsedEnemyCards.get(mUsedEnemyCards.size() - 1).id).supportAction.executeSupportAction(this, true);
                break;
            }
            case APPLY_DAMAGES: {
                Card enemy = getLastUsedEnemyCreatureCard();
                Card player = getLastUsedPlayerCreatureCard();

                // Change the defense points
                if (!mStunPlayer) {
                    Card.getAllCardsMap().get(enemy.id).fightAction.applyDamageFromOpponent(enemy, player);
                    enemy.defense = enemy.defense < 0 ? 0 : enemy.defense;
                }
                if (!mStunEnemy) {
                    Card.getAllCardsMap().get(player.id).fightAction.applyDamageFromOpponent(player, enemy);
                    player.defense = player.defense < 0 ? 0 : player.defense;
                }
                mStunPlayer = false;
                mStunEnemy = false;

                // Death computation
                if (enemy.defense <= 0) {
                    mSteps.add(BattleStep.ENEMY_DEATH);
                }
                if (player.defense <= 0) {
                    mSteps.add(BattleStep.PLAYER_DEATH);
                }

                // End of battle
                int enemyRemainingLife = 0;
                for (Card card : mUsedEnemyCards) {
                    if (card.type == Card.Type.CREATURE) {
                        enemyRemainingLife += card.defense;
                    }
                }
                for (Card card : mRemainingEnemyCards) {
                    if (card.type == Card.Type.CREATURE) {
                        enemyRemainingLife += card.defense;
                    }
                }

                int playerRemainingLife = 0;
                for (Card card : mUsedPlayerCards) {
                    if (card.type == Card.Type.CREATURE) {
                        playerRemainingLife += card.defense;
                    }
                }
                for (Card card : mRemainingPlayerCards) {
                    if (card.type == Card.Type.CREATURE) {
                        playerRemainingLife += card.defense;
                    }
                }

                if (enemyRemainingLife <= 0 && playerRemainingLife <= 0) {
                    mSteps.add(BattleStep.DRAW);
                } else if (enemyRemainingLife <= 0 && playerRemainingLife > 0) {
                    mSteps.add(BattleStep.PLAYER_WON);
                } else if (enemyRemainingLife > 0 && playerRemainingLife <= 0) {
                    mSteps.add(BattleStep.ENEMY_WON);
                } else {
                    mSteps.add(BattleStep.END_TURN);
                }
                break;
            }
        }

        Dog.i(step.name());
        return step;
    }

    public void stunEnemy(boolean fromEnemyPointOfView) {
        if (fromEnemyPointOfView) {
            mStunPlayer = true;
        } else {
            mStunEnemy = true;
        }
    }
}
