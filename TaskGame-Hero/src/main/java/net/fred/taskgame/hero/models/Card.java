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

    public interface FightAction {
        void applyDamageFromOpponent(Card current, Card opponent);
    }

    public interface SupportAction {
        void executeSupportAction(BattleManager manager, boolean fromEnemyPointOfView);
    }

    public enum Type {CREATURE, SUPPORT}

    public final static int CREATURE_TROLL = 0;
    public final static int CREATURE_TROLL_2 = 1;
    public final static int CREATURE_TROLL_3 = 2;
    public final static int CREATURE_TROLL_4 = 3;
    public final static int CREATURE_SKELETON = 10;
    public final static int CREATURE_SKELETON_2 = 11;
    public final static int CREATURE_SKELETON_3 = 12;
    public final static int CREATURE_ENCHANTED_TREE = 20;
    public final static int CREATURE_ENCHANTED_TREE_2 = 21;
    public final static int CREATURE_SYLPH = 30;
    public final static int CREATURE_SYLPH_2 = 31;
    public final static int CREATURE_SYLPH_3 = 32;
    public final static int CREATURE_LIZARD = 40;
    public final static int CREATURE_LIZARD_2 = 41;
    public final static int CREATURE_ZOMBIE = 50;
    public final static int CREATURE_ZOMBIE_2 = 51;
    public final static int CREATURE_MERMAN = 60;
    public final static int CREATURE_MERMAN_2 = 61;
    public final static int CREATURE_MERMAN_3 = 62;
    public final static int CREATURE_MERMAN_4 = 63;
    public final static int CREATURE_EMPTY_ARMOR = 70;
    public final static int CREATURE_EMPTY_ARMOR_2 = 71;
    public final static int CREATURE_EMPTY_ARMOR_3 = 72;
    public final static int CREATURE_EMPTY_ARMOR_4 = 73;
    public final static int CREATURE_GRUNT = 80;
    public final static int CREATURE_GRUNT_2 = 81;
    public final static int CREATURE_GRUNT_3 = 82;
    public final static int CREATURE_GRUNT_4 = 83;
    public final static int CREATURE_LICH = 90;
    public final static int CREATURE_LICH_2 = 91;
    public final static int CREATURE_LICH_3 = 92;
    public final static int CREATURE_LICH_4 = 93;
    public final static int CREATURE_SPECTRE = 100;
    public final static int CREATURE_SPECTRE_2 = 101;
    public final static int CREATURE_SPECTRE_3 = 102;
    public final static int CREATURE_SPECTRE_4 = 103;
    public final static int CREATURE_SPECTRE_5 = 104;

    public final static int SUPPORT_POWER_POTION = 10000;
    public final static int SUPPORT_ADD_WEAPON = 10001;
    public final static int SUPPORT_WEAPON_EROSION = 10002;
    public final static int SUPPORT_FREEDOM = 10003;
    public final static int SUPPORT_CONFUSION = 10004;
    public final static int SUPPORT_SURPRISE = 10005;
    public final static int SUPPORT_MEDICAL_ATTENTION = 10006;

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
    public transient boolean useWeapon;
    public transient boolean useMagic;
    public transient int iconResId = INVALID_ID;
    public transient int price;
    public transient FightAction fightAction;
    public transient SupportAction supportAction;

    public Card() {
        fightAction = new FightAction() {
            @Override
            public void applyDamageFromOpponent(Card current, Card opponent) {
                current.defense -= opponent.attack;
            }
        };
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
        card.useWeapon = useWeapon;
        card.useMagic = useMagic;
        card.iconResId = iconResId;
        card.price = price;
        card.isObtained = isObtained;
        card.isInDeck = isInDeck;
        card.fightAction = fightAction;
        card.supportAction = supportAction;
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
        card.useWeapon = false;
        card.useMagic = false;
        card.iconResId = R.drawable.troll;
        card.desc = "It's fascinated to see what we can do with some little piece of rocks\n ● Receive x1.5 damage against magic";
        card.fightAction = new FightAction() {
            @Override
            public void applyDamageFromOpponent(Card current, Card opponent) {
                if (opponent.useMagic) {
                    current.defense -= opponent.attack * 1.5;
                } else {
                    current.defense -= opponent.attack;
                }
            }
        };
        checkCreatureCard(card);
        ALL_CARDS_MAP.append(card.id, card);

        card = new Card();
        card.id = CREATURE_SKELETON;
        card.isObtained = obtainedList.get(card.id);
        card.isInDeck = inDeckList.get(card.id);
        card.neededSlots = 1;
        card.price = 0; // First one is free
        card.name = "Skeleton Archer";
        card.attack = 1;
        card.defense = 3;
        card.useWeapon = true;
        card.useMagic = false;
        card.iconResId = R.drawable.skeleton;
        card.desc = "Deads are not totally dead, and they strangely know how to send arrows in your face";
        checkCreatureCard(card);
        ALL_CARDS_MAP.append(card.id, card);

        card = new Card();
        card.id = CREATURE_ENCHANTED_TREE;
        card.isObtained = obtainedList.get(card.id);
        card.isInDeck = inDeckList.get(card.id);
        card.neededSlots = 2;
        card.price = card.neededSlots * 50;
        card.name = "Enchanted Tree";
        card.attack = 2;
        card.defense = 5;
        card.useWeapon = false;
        card.useMagic = false;
        card.iconResId = R.drawable.enchanted_tree;
        card.desc = "Nature is beautiful, except maybe when it tries to kill you\n ● Receive x1.5 damage against weapons";
        card.fightAction = new FightAction() {
            @Override
            public void applyDamageFromOpponent(Card current, Card opponent) {
                if (opponent.useWeapon) {
                    current.defense -= opponent.attack * 1.5;
                } else {
                    current.defense -= opponent.attack;
                }
            }
        };
        checkCreatureCard(card);
        ALL_CARDS_MAP.append(card.id, card);

        card = new Card();
        card.id = CREATURE_SPECTRE;
        card.isObtained = obtainedList.get(card.id);
        card.isInDeck = inDeckList.get(card.id);
        card.neededSlots = 3;
        card.price = card.neededSlots * 50;
        card.name = "Spectre";
        card.attack = 3;
        card.defense = 4;
        card.useWeapon = false;
        card.useMagic = true;
        card.iconResId = R.drawable.spectre;
        card.desc = "It's always better to have that kind of creature on your side than on the opposite one";
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
        card.desc = "Ok it's a summoned creature, but does that means you should be heartless?\n ● Regain defense by 4 if wounded";
        card.supportAction = new SupportAction() {
            @Override
            public void executeSupportAction(BattleManager manager, boolean fromEnemyPointOfView) {
                Card player = manager.getLastUsedPlayerCreatureCard(fromEnemyPointOfView);
                //TODO: does not take in account the previous defense increase
                int defenseDiff = ALL_CARDS_MAP.get(player.id).defense - player.defense;
                if (defenseDiff > 0) {
                    player.defense += Math.min(4, defenseDiff);
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
        card.id = SUPPORT_ADD_WEAPON;
        card.isObtained = obtainedList.get(card.id);
        card.isInDeck = inDeckList.get(card.id);
        card.neededSlots = 3;
        card.price = card.neededSlots * 50;
        card.type = Card.Type.SUPPORT;
        card.name = "Battle axe";
        card.iconResId = R.drawable.axe;
        card.desc = "The best way to gain respect from your enemy is by putting an axe in his face\n ● Increase attack by 4 if the creature doesn't already use a weapon";
        card.supportAction = new SupportAction() {
            @Override
            public void executeSupportAction(BattleManager manager, boolean fromEnemyPointOfView) {
                Card player = manager.getLastUsedPlayerCreatureCard(fromEnemyPointOfView);
                if (!player.useWeapon) {
                    player.attack += 4;
                }
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
        card.desc = "Your enemy weapon starts to run into pieces. Serves him damned right!\n ● Lower enemy attack by 4 if he uses a weapon";
        card.supportAction = new SupportAction() {
            @Override
            public void executeSupportAction(BattleManager manager, boolean fromEnemyPointOfView) {
                Card enemy = manager.getLastUsedEnemyCreatureCard(fromEnemyPointOfView);
                if (enemy.useWeapon) {
                    enemy.attack -= 4;
                    if (enemy.attack < 0) {
                        enemy.attack = 0;
                    }
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
