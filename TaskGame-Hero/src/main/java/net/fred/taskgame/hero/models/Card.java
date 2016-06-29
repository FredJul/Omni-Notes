package net.fred.taskgame.hero.models;

import android.databinding.BindingAdapter;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.widget.ImageView;

import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.sql.language.Select;
import com.raizlabs.android.dbflow.structure.BaseModel;

import net.fred.taskgame.hero.R;
import net.fred.taskgame.hero.logic.BattleManager;

import org.parceler.Parcel;

import java.util.ArrayList;
import java.util.List;

@Parcel
@Table(database = AppDatabase.class)
public class Card extends BaseModel implements Cloneable {

    public interface SupportAction {
        void executeSupportAction(BattleManager manager, boolean fromEnemyPointOfView);
    }

    public enum Type {CREATURE, SUPPORT}

    public final static int CREATURE_TROLL = 0;
    public final static int CREATURE_SKELETON_ARCHER = 1;
    public final static int CREATURE_TREE = 2;
    public final static int CREATURE_GHOST = 3;

    public final static int SUPPORT_POWER_POTION = 1000;
    public final static int SUPPORT_WEAPON_EROSION = 1001;
    public final static int SUPPORT_FREEDOM = 1002;
    public final static int SUPPORT_CONFUSION = 1003;
    public final static int SUPPORT_SURPRISE = 1004;
    public final static int SUPPORT_MEDICAL_ATTENTION = 1005;

    public final static int INVALID_ID = 0;

    private final static SparseArray<Card> ALL_CARDS_MAP = new SparseArray<>();

    @PrimaryKey
    public int id = INVALID_ID;

    @Column
    public boolean isObtained;

    @Column
    public boolean isInDeck;

    public transient Type type = Type.CREATURE;
    public transient String name = "";
    public transient String desc = "";
    public transient int neededSlots;
    public transient int attack;
    public transient int defense;
    public transient int iconResId = INVALID_ID;
    public transient int price;
    public transient SupportAction supportAction;

    public Card() {
    }

    @Override
    public Card clone() {
        Card card = new Card();
        card.id = id;
        card.type = type;
        card.name = name;
        card.desc = desc;
        card.neededSlots = neededSlots;
        card.attack = attack;
        card.defense = defense;
        card.iconResId = iconResId;
        card.price = price;
        card.isObtained = isObtained;
        card.isInDeck = isInDeck;
        return card;
    }

    @BindingAdapter("android:src")
    public static void setSrc(ImageView view, int res) {
        view.setImageResource(res);
    }

    public static SparseArray<Card> getAllCardsMap() {
        return ALL_CARDS_MAP;
    }

    public static List<Card> getObtainedCardList() {
        SparseArray<Card> allCards = Card.getAllCardsMap();
        ArrayList<Card> obtainedList = new ArrayList<>();
        for (int i = 0; i < allCards.size(); i++) {
            Card card = allCards.valueAt(i);
            if (card.isObtained) {
                obtainedList.add(card);
            }
        }
        return obtainedList;
    }

    public static List<Card> getNonObtainedCardList() {
        return getNonObtainedCardList(Level.getCorrespondingDeckSlots());
    }

    public static List<Card> getNonObtainedCardList(int totalDeckSlots) {
        SparseArray<Card> allCards = Card.getAllCardsMap();
        ArrayList<Card> nonObtainedList = new ArrayList<>();
        for (int i = 0; i < allCards.size(); i++) {
            Card card = allCards.valueAt(i);
            // Do not display all card immediately
            if (!card.isObtained && card.neededSlots <= totalDeckSlots / 2) {
                nonObtainedList.add(card);
            }
        }
        return nonObtainedList;
    }

    public static List<Card> getDeckCardList() {
        SparseArray<Card> allCards = Card.getAllCardsMap();
        ArrayList<Card> deckCardList = new ArrayList<>();
        for (int i = 0; i < allCards.size(); i++) {
            Card card = allCards.valueAt(i);
            if (card.isInDeck) {
                deckCardList.add(card);
            }
        }
        return deckCardList;
    }

    public static void populate() {
        ALL_CARDS_MAP.clear();

        SparseBooleanArray obtainedList = new SparseBooleanArray();
        SparseBooleanArray inDeckList = new SparseBooleanArray();
        for (Card card : new Select().from(Card.class).queryList()) {
            obtainedList.append(card.id, card.isObtained);
            inDeckList.append(card.id, card.isInDeck);
        }

        /****** CREATURE CARDS *******/

        Card card = new Card();
        card.id = CREATURE_TROLL;
        card.isObtained = true; // the only card you get for free at the beginning
        card.isInDeck = inDeckList.size() == 0 || inDeckList.get(card.id); // by default, add it
        card.neededSlots = 2;
        card.price = 0;
        card.name = "Troll";
        card.attack = 2;
        card.defense = 4;
        card.iconResId = R.drawable.troll;
        card.desc = "It's fascinated to see what we can do with some little piece of rocks";
        checkCreatureCard(card);
        ALL_CARDS_MAP.append(card.id, card);

        card = new Card();
        card.id = CREATURE_SKELETON_ARCHER;
        card.isObtained = obtainedList.get(card.id);
        card.isInDeck = inDeckList.get(card.id);
        card.neededSlots = 1;
        card.price = 0; // First one is free
        card.name = "Skeleton Archer";
        card.attack = 1;
        card.defense = 3;
        card.iconResId = R.drawable.skeleton_archer;
        card.desc = "Deads are not totally dead, and they strangely know how to send arrows in your face";
        checkCreatureCard(card);
        ALL_CARDS_MAP.append(card.id, card);

        card = new Card();
        card.id = CREATURE_TREE;
        card.isObtained = obtainedList.get(card.id);
        card.isInDeck = inDeckList.get(card.id);
        card.neededSlots = 2;
        card.price = card.neededSlots * 50;
        card.name = "Enchanted Tree";
        card.attack = 2;
        card.defense = 5;
        card.iconResId = R.drawable.enchanted_tree;
        card.desc = "Nature is beautiful, except maybe when it tries to kill you";
        checkCreatureCard(card);
        ALL_CARDS_MAP.append(card.id, card);

        card = new Card();
        card.id = CREATURE_GHOST;
        card.isObtained = obtainedList.get(card.id);
        card.isInDeck = inDeckList.get(card.id);
        card.neededSlots = 3;
        card.price = card.neededSlots * 50;
        card.name = "Ghost";
        card.attack = 3;
        card.defense = 4;
        card.iconResId = R.drawable.ghost;
        card.desc = "It is real enough to be able to punch you in the face";
        checkCreatureCard(card);
        ALL_CARDS_MAP.append(card.id, card);

        /****** SUPPORT CARDS *******/

        card = new Card();
        card.id = SUPPORT_MEDICAL_ATTENTION;
        card.isObtained = obtainedList.get(card.id);
        card.isInDeck = inDeckList.get(card.id);
        card.neededSlots = 2;
        card.price = card.neededSlots * 50;
        card.type = Card.Type.SUPPORT;
        card.name = "Medical Attention";
        card.iconResId = R.drawable.medical_attention;
        card.desc = "Ok it's a summoned creature, but does that means you should be heartless?\n ● Regain defense by 3 if wounded";
        card.supportAction = new SupportAction() {
            @Override
            public void executeSupportAction(BattleManager manager, boolean fromEnemyPointOfView) {
                Card player = manager.getLastUsedPlayerCreatureCard(fromEnemyPointOfView);
                //TODO: does not take in account the previous defense increase
                int defenseDiff = ALL_CARDS_MAP.get(player.id).defense - player.defense;
                if (defenseDiff > 0) {
                    player.defense += Math.min(3, defenseDiff);
                }
            }
        };
        ALL_CARDS_MAP.append(card.id, card);

        card = new Card();
        card.id = SUPPORT_POWER_POTION;
        card.isObtained = obtainedList.get(card.id);
        card.isInDeck = inDeckList.get(card.id);
        card.neededSlots = 4;
        card.price = card.neededSlots * 50;
        card.type = Card.Type.SUPPORT;
        card.name = "(Fake) Potion of invincibility";
        card.iconResId = R.drawable.red_potion;
        card.desc = "It's only syrup, but placebo effect makes your creature feel invincible\n ● Multiply attack by 2\n ● Divide defense by 1.3";
        card.supportAction = new SupportAction() {
            @Override
            public void executeSupportAction(BattleManager manager, boolean fromEnemyPointOfView) {
                Card player = manager.getLastUsedPlayerCreatureCard(fromEnemyPointOfView);
                player.attack *= 2;
                player.defense /= 1.3;
            }
        };
        ALL_CARDS_MAP.append(card.id, card);

        card = new Card();
        card.id = SUPPORT_WEAPON_EROSION;
        card.isObtained = obtainedList.get(card.id);
        card.isInDeck = inDeckList.get(card.id);
        card.neededSlots = 3;
        card.price = card.neededSlots * 50;
        card.type = Card.Type.SUPPORT;
        card.name = "Weapon erosion";
        card.iconResId = R.drawable.erode_weapon;
        card.desc = "Your enemy weapon starts to run into pieces. Serves him damned right!\n ● Lower enemy attack by 4";
        card.supportAction = new SupportAction() {
            @Override
            public void executeSupportAction(BattleManager manager, boolean fromEnemyPointOfView) {
                Card enemy = manager.getLastUsedEnemyCreatureCard(fromEnemyPointOfView);
                enemy.attack -= 4;
                if (enemy.attack < 0) {
                    enemy.attack = 0;
                }
            }
        };
        ALL_CARDS_MAP.append(card.id, card);

        card = new Card();
        card.id = SUPPORT_FREEDOM;
        card.isObtained = obtainedList.get(card.id);
        card.isInDeck = inDeckList.get(card.id);
        card.neededSlots = 6;
        card.price = card.neededSlots * 50;
        card.type = Card.Type.SUPPORT;
        card.name = "Freedom";
        card.iconResId = R.drawable.unshackled;
        card.desc = "Free your creature from your control. It will charge the enemy with all his forces, and profit of the breach to run away.\n ● Multiply attack by 3\n ● Your creature run away after 1 round";
        card.supportAction = new SupportAction() {
            @Override
            public void executeSupportAction(BattleManager manager, boolean fromEnemyPointOfView) {
                Card player = manager.getLastUsedPlayerCreatureCard(fromEnemyPointOfView);
                player.defense = 0; //TODO: not true, but does the job for now
                player.attack *= 3;
            }
        };
        ALL_CARDS_MAP.append(card.id, card);

        card = new Card();
        card.id = SUPPORT_CONFUSION;
        card.isObtained = obtainedList.get(card.id);
        card.isInDeck = inDeckList.get(card.id);
        card.neededSlots = 4;
        card.price = card.neededSlots * 50;
        card.type = Card.Type.SUPPORT;
        card.name = "Confusion";
        card.iconResId = R.drawable.confusion;
        card.desc = "We say that the pen is mightier than the sword, but there is something even more efficient: a sneaky confusing lie\n ● Enemy is confused and skip this turn";
        card.supportAction = new SupportAction() {
            @Override
            public void executeSupportAction(BattleManager manager, boolean fromEnemyPointOfView) {
                manager.stunEnemy(fromEnemyPointOfView);
            }
        };
        ALL_CARDS_MAP.append(card.id, card);

        card = new Card();
        card.id = SUPPORT_SURPRISE;
        card.isObtained = obtainedList.get(card.id);
        card.isInDeck = inDeckList.get(card.id);
        card.neededSlots = 4;
        card.price = card.neededSlots * 50;
        card.type = Card.Type.SUPPORT;
        card.name = "Surprise!";
        card.iconResId = R.drawable.surprise;
        card.desc = "Forget honor and attack the enemy from behind, it's more effective\n ● If you kill the enemy this turn, you'll not receive any damage";
        card.supportAction = new SupportAction() {
            @Override
            public void executeSupportAction(BattleManager manager, boolean fromEnemyPointOfView) {
                Card player = manager.getLastUsedPlayerCreatureCard(fromEnemyPointOfView);
                Card enemy = manager.getLastUsedEnemyCreatureCard(fromEnemyPointOfView);
                if (enemy.defense <= player.attack) {
                    player.defense += enemy.attack;
                }
            }
        };
        ALL_CARDS_MAP.append(card.id, card);
    }

    private static void checkCreatureCard(Card card) {
        int acceptableSum = card.neededSlots * 3;
        float margin = acceptableSum / 100f * 35f;
        if (card.attack + card.defense > acceptableSum + margin || card.attack + card.defense < acceptableSum - margin) {
            throw new IllegalStateException();
        }
    }
}
