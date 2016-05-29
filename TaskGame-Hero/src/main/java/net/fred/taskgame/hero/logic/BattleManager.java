package net.fred.taskgame.hero.logic;

import net.fred.taskgame.hero.model.Card;

import org.parceler.Parcel;

import java.util.ArrayList;
import java.util.List;

@Parcel
public class BattleManager {

    public enum BattleStatus {NOT_FINISHED, DRAW, ENEMY_WON, PLAYER_WON}

    private List<Card> mRemainingEnemyCards = new ArrayList<>();
    private List<Card> mUsedEnemyCards = new ArrayList<>();
    private List<Card> mRemainingPlayerCards = new ArrayList<>();
    private List<Card> mUsedPlayerCards = new ArrayList<>();

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

    public void addEnemyCard(Card card) {
        mRemainingEnemyCards.add(card);
    }

    public void addPlayerCard(Card card) {
        mRemainingPlayerCards.add(card);
    }

    public Card getNextEnemyCreatureCard() {
        for (Card card : mRemainingEnemyCards) {
            if (card.type == Card.Type.CREATURE) {
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

    public Card getLastUsedPlayerCreatureCard() {
        for (int i = mUsedPlayerCards.size() - 1; i >= 0; i--) {
            Card card = mUsedPlayerCards.get(i);
            if (card.type == Card.Type.CREATURE) {
                return card;
            }
        }

        return null;
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
        Card enemy;
        if (!isEnemyCreatureStillAlive()) {
            enemy = getNextEnemyCreatureCard();
            mUsedEnemyCards.add(enemy);
            mRemainingEnemyCards.remove(enemy);
        } else {
            enemy = getLastUsedEnemyCreatureCard();
        }

        Card player = getLastUsedPlayerCreatureCard();

        enemy.defense -= player.attack;
        enemy.defense = enemy.defense < 0 ? 0 : enemy.defense;
        player.defense -= enemy.attack;
        player.defense = player.defense < 0 ? 0 : player.defense;
    }

    public void play(Card card) {
        mRemainingPlayerCards.remove(card);
        mUsedPlayerCards.add(card);

        if (card.type == Card.Type.SUPPORT) {
            executeSupportCard(card);
        }

        play();
    }

    public BattleStatus getBattleStatus() {
        int enemyLife = 0;
        for (Card card : mUsedEnemyCards) {
            if (card.type == Card.Type.CREATURE) {
                enemyLife += card.defense;
            }
        }
        for (Card card : mRemainingEnemyCards) {
            if (card.type == Card.Type.CREATURE) {
                enemyLife += card.defense;
            }
        }

        int playerLife = 0;
        for (Card card : mUsedPlayerCards) {
            if (card.type == Card.Type.CREATURE) {
                playerLife += card.defense;
            }
        }
        for (Card card : mRemainingPlayerCards) {
            if (card.type == Card.Type.CREATURE) {
                playerLife += card.defense;
            }
        }

        if (enemyLife <= 0 && playerLife <= 0) {
            return BattleStatus.DRAW;
        } else if (enemyLife <= 0 && playerLife > 0) {
            return BattleStatus.PLAYER_WON;
        } else if (enemyLife > 0 && playerLife <= 0) {
            return BattleStatus.ENEMY_WON;
        } else {
            return BattleStatus.NOT_FINISHED;
        }
    }

    private void executeSupportCard(Card card) {
        switch (card.supportAction) {
            case PLAYER_ATTACK_MULT:
                getLastUsedPlayerCreatureCard().attack *= 2;
                getLastUsedPlayerCreatureCard().defense /= 1.3;
                break;
            case PLAYER_DEFENSE_MULT:
                getLastUsedPlayerCreatureCard().defense *= 2;
                break;
            case ENEMY_ATTACK_DIV:
                getCurrentOrNextAliveEnemyCreatureCard().attack /= 2;
                break;
        }
    }
}
