package net.fred.taskgame.hero.logic;

import net.fred.taskgame.hero.models.Card;
import net.fred.taskgame.hero.utils.Dog;

import org.parceler.Parcel;

import java.util.ArrayList;
import java.util.List;

@Parcel
public class BattleManager {

    public enum BattleStep {APPLY_PLAYER_SUPPORT, SELECT_STRATEGY, APPLY_ENEMY_SUPPORT, FIGHT, APPLY_DAMAGES, PLAYER_DEATH, ENEMY_DEATH, END_TURN, PLAYER_WON, ENEMY_WON, DRAW}

    public enum BattleStrategy {ATTACK, DEFENSE, ALEATORY}

    public enum AleatoryAffectedField {ATTACK, DEFENSE}

    public class AleatoryResult {
        public AleatoryAffectedField affectedField;
        public int bonusOrPenalty;
    }

    List<Card> mRemainingEnemyCards = new ArrayList<>();
    List<Card> mUsedEnemyCards = new ArrayList<>();
    List<Card> mRemainingPlayerCards = new ArrayList<>();
    List<Card> mUsedPlayerCards = new ArrayList<>();
    BattleStep mCurrentStep = BattleStep.APPLY_PLAYER_SUPPORT;
    List<BattleStep> mNextSteps = new ArrayList<>();

    BattleStrategy mCurrentStrategy = BattleStrategy.ATTACK;

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
        mNextSteps.add(BattleStep.SELECT_STRATEGY);
        if (!isEnemyCreatureStillAlive()) {
            Card enemy = getNextEnemyCreatureCard();
            mUsedEnemyCards.add(enemy);
            mRemainingEnemyCards.remove(enemy);
        } else {
            if (mRemainingEnemyCards.size() > 0 && mRemainingEnemyCards.get(0).type == Card.Type.SUPPORT) {
                mUsedEnemyCards.add(mRemainingEnemyCards.get(0));
                mRemainingEnemyCards.remove(0);
                mNextSteps.add(BattleStep.APPLY_ENEMY_SUPPORT);
            }
        }

        mNextSteps.add(BattleStep.FIGHT);
        mNextSteps.add(BattleStep.APPLY_DAMAGES);
    }

    public void play(Card card) {
        mRemainingPlayerCards.remove(card);
        mUsedPlayerCards.add(card);

        if (card.type == Card.Type.SUPPORT) {
            mNextSteps.add(BattleStep.APPLY_PLAYER_SUPPORT);
        }

        play();
    }

    public AleatoryResult applyNewStrategy(BattleStrategy currentStrategy) {
        mCurrentStrategy = currentStrategy;
        if (currentStrategy == BattleStrategy.ALEATORY) {
            AleatoryResult result = new AleatoryResult();

            // 1 chance on top of 2
            result.affectedField = Math.random() > 0.555d ? AleatoryAffectedField.ATTACK : AleatoryAffectedField.DEFENSE;

            Card player = getLastUsedPlayerCreatureCard();
            if (result.affectedField == AleatoryAffectedField.ATTACK) {
                result.bonusOrPenalty = getRandomIntBetween(1, Math.round(player.attack / 2.0f)); // up to 50% bonus or penalty
                if (Math.random() > 0.555d) { // 1 chance on 2 to get a negative value
                    result.bonusOrPenalty *= -1;
                }
                player.attack += result.bonusOrPenalty;
                if (player.attack <= 0) {
                    player.attack = 1;
                }
            } else {
                result.bonusOrPenalty = getRandomIntBetween(1, Math.round(player.defense / 2.0f)); // up to 50% bonus or penalty
                if (Math.random() > 0.555d) { // 1 chance on 2 to get a negative value
                    result.bonusOrPenalty *= -1;
                }
                player.defense += result.bonusOrPenalty;
                if (player.defense <= 0) {
                    player.defense = 1;
                }
            }

            return result;
        }

        return null;
    }

    public BattleStep getCurrentStep() {
        return mCurrentStep;
    }

    public BattleStep executeNextStep() {
        mCurrentStep = mNextSteps.remove(0);
        switch (mCurrentStep) {
            case APPLY_PLAYER_SUPPORT: {
                if (!mStunPlayer) {
                    Card.getAllCardsMap().get(mUsedPlayerCards.get(mUsedPlayerCards.size() - 1).id).supportAction.executeSupportAction(this, false);
                }
                break;
            }
            case APPLY_ENEMY_SUPPORT: {
                if (!mStunEnemy) {
                    Card.getAllCardsMap().get(mUsedEnemyCards.get(mUsedEnemyCards.size() - 1).id).supportAction.executeSupportAction(this, true);
                }
                break;
            }
            case APPLY_DAMAGES: {
                Card enemy = getLastUsedEnemyCreatureCard();
                Card player = getLastUsedPlayerCreatureCard();

                // Change the defense points
                if (!mStunPlayer && mCurrentStrategy != BattleStrategy.DEFENSE) {
                    Card.getAllCardsMap().get(enemy.id).fightAction.applyDamageFromOpponent(enemy, player);
                    enemy.defense = enemy.defense < 0 ? 0 : enemy.defense;
                }
                if (!mStunEnemy) {
                    if (mCurrentStrategy == BattleStrategy.DEFENSE) {
                        // We reduce damage from 0 to 100% depending of luck
                        int previousDefense = player.defense;
                        Card.getAllCardsMap().get(player.id).fightAction.applyDamageFromOpponent(player, enemy);
                        int realDamage = (int) Math.round((previousDefense - player.defense) * Math.random());
                        player.defense = previousDefense - realDamage;
                    } else {
                        Card.getAllCardsMap().get(player.id).fightAction.applyDamageFromOpponent(player, enemy);
                    }

                    player.defense = player.defense < 0 ? 0 : player.defense;
                }
                mStunPlayer = false;
                mStunEnemy = false;

                // Death computation
                if (enemy.defense <= 0) {
                    mNextSteps.add(BattleStep.ENEMY_DEATH);
                }
                if (player.defense <= 0) {
                    mNextSteps.add(BattleStep.PLAYER_DEATH);
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
                    mNextSteps.add(BattleStep.DRAW);
                } else if (enemyRemainingLife <= 0 && playerRemainingLife > 0) {
                    mNextSteps.add(BattleStep.PLAYER_WON);
                } else if (enemyRemainingLife > 0 && playerRemainingLife <= 0) {
                    mNextSteps.add(BattleStep.ENEMY_WON);
                } else {
                    mNextSteps.add(BattleStep.END_TURN);
                }
                break;
            }
        }

        Dog.i(mCurrentStep.name());
        return mCurrentStep;
    }

    public void stunEnemy(boolean fromEnemyPointOfView) {
        if (fromEnemyPointOfView) {
            mStunPlayer = true;
        } else {
            mStunEnemy = true;
        }
    }

    private int getRandomIntBetween(int lower, int higher) {
        if (higher <= lower) {
            return lower;
        }
        return (int) (Math.random() * (higher + 1 - lower)) + lower;
    }
}
