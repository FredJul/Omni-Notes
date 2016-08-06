package net.fred.taskgame.hero.models;

import android.databinding.BindingAdapter;
import android.support.annotation.DrawableRes;
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
import java.util.LinkedHashMap;
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
    public final static int CREATURE_SNAKE = 40;
    public final static int CREATURE_SNAKE_2 = 41;
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
    public final static int SUPPORT_SWITCH_POTION = 10007;

    public final static int INVALID_ID = 0;

    private final static LinkedHashMap<Integer, Card> ALL_CARDS_MAP = new LinkedHashMap<>();

    @PrimaryKey
    public int id = INVALID_ID;

    @Column
    public boolean isObtained;

    @Column
    public boolean isInDeck;

    public Type type = Type.CREATURE;
    public String name = "";
    public String desc = "";
    public int neededSlots;
    public int attack;
    public int defense;
    public boolean useWeapon;
    public boolean useMagic;
    @DrawableRes
    public int iconResId = INVALID_ID;
    public int price;
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

    public static LinkedHashMap<Integer, Card> getAllCardsMap() {
        return ALL_CARDS_MAP;
    }

    public static List<Card> getObtainedCardList() {
        ArrayList<Card> obtainedList = new ArrayList<>();
        for (Card card : Card.getAllCardsMap().values()) {
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
        ArrayList<Card> nonObtainedList = new ArrayList<>();
        for (Card card : Card.getAllCardsMap().values()) {
            // Do not display all card immediately
            if (!card.isObtained && card.neededSlots <= totalDeckSlots / 2) {
                nonObtainedList.add(card);
            }
        }
        return nonObtainedList;
    }

    public static List<Card> getDeckCardList() {
        ArrayList<Card> deckCardList = new ArrayList<>();
        for (Card card : Card.getAllCardsMap().values()) {
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


        /****************************/
        /****** CREATURE CARDS ******/
        /****************************/

        Card card = generateDefaultCreatureCard(CREATURE_MERMAN, 1, obtainedList, inDeckList);
        card.price = 0; // First one is free
        card.name = "Merman";
        card.attack = 2;
        card.defense = 1;
        card.iconResId = R.drawable.merman;
        card.desc = "Mermans are famous for their courage, even if it's not always enough to save their lives";
        checkCreatureCard(card);

        card = generateDefaultCreatureCard(CREATURE_SYLPH, 1, obtainedList, inDeckList);
        card.name = "Sylph";
        card.attack = 1;
        card.defense = 2;
        card.useMagic = true;
        card.iconResId = R.drawable.sylph;
        card.desc = "Looks kind and peaceful, but her basic wind magic can surprise you\n ● Weak against weapons: +1 received damage";
        card.fightAction = new FightAction() {
            @Override
            public void applyDamageFromOpponent(Card current, Card opponent) {
                if (opponent.useWeapon) {
                    current.defense -= opponent.attack + 1;
                } else {
                    current.defense -= opponent.attack;
                }
            }
        };
        checkCreatureCard(card);

        card = generateDefaultCreatureCard(CREATURE_TROLL, 2, obtainedList, inDeckList);
        card.isObtained = true; // the only card you get for free at the beginning
        card.isInDeck = inDeckList.size() == 0 || inDeckList.get(card.id); // by default, add it
        card.name = "Baby Troll";
        card.attack = 2;
        card.defense = 4;
        card.iconResId = R.drawable.troll;
        card.desc = "Troll babies always try to eat everything and doesn't really care if it's human or not\n ● Weak against magic: +1 received damage";
        card.fightAction = new FightAction() {
            @Override
            public void applyDamageFromOpponent(Card current, Card opponent) {
                if (opponent.useMagic) {
                    current.defense -= opponent.attack + 1;
                } else {
                    current.defense -= opponent.attack;
                }
            }
        };
        checkCreatureCard(card);

        card = generateDefaultCreatureCard(CREATURE_SKELETON, 2, obtainedList, inDeckList);
        card.name = "Skeleton Archer";
        card.attack = 3;
        card.defense = 3;
        card.useWeapon = true;
        card.iconResId = R.drawable.skeleton;
        card.desc = "Deads are not totally dead, and they strangely know how to send arrows in your face\n ● Resistant to magic: -1 received damage";
        card.fightAction = new FightAction() {
            @Override
            public void applyDamageFromOpponent(Card current, Card opponent) {
                if (opponent.useMagic) {
                    current.defense -= opponent.attack >= 1 ? opponent.attack - 1 : opponent.attack;
                } else {
                    current.defense -= opponent.attack;
                }
            }
        };
        checkCreatureCard(card);

        card = generateDefaultCreatureCard(CREATURE_SYLPH_2, 3, obtainedList, inDeckList);
        card.name = "Charming Sylph";
        card.attack = 6;
        card.defense = 3;
        card.useMagic = true;
        card.iconResId = R.drawable.sylph_2;
        card.desc = "Will you dare hit a beautiful lady?\n ● Weak against weapons: +1 received damage";
        card.fightAction = new FightAction() {
            @Override
            public void applyDamageFromOpponent(Card current, Card opponent) {
                if (opponent.useWeapon) {
                    current.defense -= opponent.attack + 1;
                } else {
                    current.defense -= opponent.attack;
                }
            }
        };
        checkCreatureCard(card);

        card = generateDefaultCreatureCard(CREATURE_GRUNT, 3, obtainedList, inDeckList);
        card.name = "Grunt";
        card.attack = 4;
        card.defense = 4;
        card.iconResId = R.drawable.grunt;
        card.desc = "Half human, half beast. Killing someone is a natural law for them and they don't perceive that as a problem.";
        checkCreatureCard(card);

        card = generateDefaultCreatureCard(CREATURE_ENCHANTED_TREE, 3, obtainedList, inDeckList);
        card.name = "Enchanted Tree";
        card.attack = 3;
        card.defense = 6;
        card.iconResId = R.drawable.enchanted_tree;
        card.desc = "Nature is beautiful, except maybe when it tries to kill you\n ● Weak against weapons: +2 received damage";
        card.fightAction = new FightAction() {
            @Override
            public void applyDamageFromOpponent(Card current, Card opponent) {
                if (opponent.useWeapon) {
                    current.defense -= opponent.attack + 2;
                } else {
                    current.defense -= opponent.attack;
                }
            }
        };
        checkCreatureCard(card);

        card = generateDefaultCreatureCard(CREATURE_LICH, 4, obtainedList, inDeckList);
        card.name = "Lich";
        card.attack = 7;
        card.defense = 5;
        card.useMagic = true;
        card.iconResId = R.drawable.lich;
        card.desc = "Ancient mage who found a way to not be affected by the time anymore";
        checkCreatureCard(card);

        card = generateDefaultCreatureCard(CREATURE_EMPTY_ARMOR, 4, obtainedList, inDeckList);
        card.name = "Empty armor";
        card.attack = 5;
        card.defense = 7;
        card.useWeapon = true;
        card.iconResId = R.drawable.empty_armor;
        card.desc = "Looks empty and harmless, but don't turn your back on it or you may regret it";
        checkCreatureCard(card);

        card = generateDefaultCreatureCard(CREATURE_SPECTRE, 5, obtainedList, inDeckList);
        card.name = "Spectre";
        card.attack = 7;
        card.defense = 9;
        card.useMagic = true;
        card.iconResId = R.drawable.spectre;
        card.desc = "It's never good when nightmare creatures are becoming reality and attack you";
        checkCreatureCard(card);

        card = generateDefaultCreatureCard(CREATURE_ZOMBIE, 6, obtainedList, inDeckList);
        card.name = "Zombie";
        card.attack = 6;
        card.defense = 10;
        card.iconResId = R.drawable.zombie;
        card.desc = "Why dead people cannot live like everyone else?";
        checkCreatureCard(card);

        card = generateDefaultCreatureCard(CREATURE_SNAKE, 6, obtainedList, inDeckList);
        card.name = "M. Python";
        card.attack = 8;
        card.defense = 10;
        card.useWeapon = true;
        card.iconResId = R.drawable.snake;
        card.desc = "They are fast and, like Brian, always look at the bright side of life\n ● Weak against magic: +3 received damage";
        card.fightAction = new FightAction() {
            @Override
            public void applyDamageFromOpponent(Card current, Card opponent) {
                if (opponent.useMagic) {
                    current.defense -= opponent.attack + 3;
                } else {
                    current.defense -= opponent.attack;
                }
            }
        };
        checkCreatureCard(card);

        card = generateDefaultCreatureCard(CREATURE_TROLL_2, 7, obtainedList, inDeckList);
        card.name = "Slinger Troll";
        card.attack = 5;
        card.defense = 15;
        card.useWeapon = true;
        card.iconResId = R.drawable.troll_2;
        card.desc = "Always play 'Rock' in rock-paper-scissor game\n ● Resistant to weapon: -2 received damage\n ● Weak against magic: +3 received damage";
        card.fightAction = new FightAction() {
            @Override
            public void applyDamageFromOpponent(Card current, Card opponent) {
                if (opponent.useMagic) {
                    current.defense -= opponent.attack + 3;
                } else if (opponent.useWeapon) {
                    current.defense -= opponent.attack - 2 > 0 ? opponent.attack - 2 : 0;
                } else {
                    current.defense -= opponent.attack;
                }
            }
        };
        checkCreatureCard(card);

        card = generateDefaultCreatureCard(CREATURE_GRUNT_2, 7, obtainedList, inDeckList);
        card.name = "Crossbowman Grunt";
        card.attack = 11;
        card.defense = 10;
        card.useWeapon = true;
        card.iconResId = R.drawable.grunt_2;
        card.desc = "Born with less muscles than others, he compensates with a good manipulation of a crossbow";
        checkCreatureCard(card);


        /****************************/
        /****** SUPPORT CARDS *******/
        /****************************/

        card = generateDefaultSupportCard(SUPPORT_MEDICAL_ATTENTION, 2, obtainedList, inDeckList);
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

        card = generateDefaultSupportCard(SUPPORT_ADD_WEAPON, 3, obtainedList, inDeckList);
        card.name = "Battle axe";
        card.iconResId = R.drawable.axe;
        card.desc = "The best way to gain respect from your enemy is by putting an axe in his face\n ● Increase attack by 6 if the creature doesn't already use any weapon or magic";
        card.supportAction = new SupportAction() {
            @Override
            public void executeSupportAction(BattleManager manager, boolean fromEnemyPointOfView) {
                Card player = manager.getLastUsedPlayerCreatureCard(fromEnemyPointOfView);
                if (!player.useWeapon && !player.useMagic) {
                    player.attack += 6;
                }
            }
        };

        card = generateDefaultSupportCard(SUPPORT_WEAPON_EROSION, 3, obtainedList, inDeckList);
        card.name = "Weapon erosion";
        card.iconResId = R.drawable.erode_weapon;
        card.desc = "Your enemy weapon starts to run into pieces. Serves him damned right!\n ● Lower enemy attack by 5 if he uses a weapon";
        card.supportAction = new SupportAction() {
            @Override
            public void executeSupportAction(BattleManager manager, boolean fromEnemyPointOfView) {
                Card enemy = manager.getLastUsedEnemyCreatureCard(fromEnemyPointOfView);
                if (enemy.useWeapon) {
                    enemy.attack -= 5;
                    if (enemy.attack < 0) {
                        enemy.attack = 0;
                    }
                }
            }
        };

        card = generateDefaultSupportCard(SUPPORT_POWER_POTION, 4, obtainedList, inDeckList);
        card.name = "(Fake) Potion of invincibility";
        card.iconResId = R.drawable.red_potion;
        card.desc = "It's only syrup, but placebo effect makes your creature feel invincible\n ● Multiply attack by 2\n ● Divide defense by 1.3";
        card.supportAction = new SupportAction() {
            @Override
            public void executeSupportAction(BattleManager manager, boolean fromEnemyPointOfView) {
                Card player = manager.getLastUsedPlayerCreatureCard(fromEnemyPointOfView);
                player.attack *= 2;
                player.defense /= 1.3;
                if (player.defense <= 0) {
                    player.defense = 1;
                }
            }
        };

        card = generateDefaultSupportCard(SUPPORT_SURPRISE, 4, obtainedList, inDeckList);
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

        card = generateDefaultSupportCard(SUPPORT_CONFUSION, 5, obtainedList, inDeckList);
        card.name = "Confusion";
        card.iconResId = R.drawable.confusion;
        card.desc = "We say that the pen is mightier than the sword, but there is something even more efficient: a sneaky confusing lie\n ● Enemy is confused and skip this turn";
        card.supportAction = new SupportAction() {
            @Override
            public void executeSupportAction(BattleManager manager, boolean fromEnemyPointOfView) {
                manager.stunEnemy(fromEnemyPointOfView);
            }
        };

        card = generateDefaultSupportCard(SUPPORT_FREEDOM, 6, obtainedList, inDeckList);
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

        card = generateDefaultSupportCard(SUPPORT_SWITCH_POTION, 7, obtainedList, inDeckList);
        card.name = "Switch potion";
        card.iconResId = R.drawable.purple_potion;
        card.desc = "Your creature switch it's attack/defense level with his opponent's ones. How does the potion work? Well, it's secret.";
        card.supportAction = new SupportAction() {
            @Override
            public void executeSupportAction(BattleManager manager, boolean fromEnemyPointOfView) {
                Card player = manager.getLastUsedPlayerCreatureCard(fromEnemyPointOfView);
                Card enemy = manager.getLastUsedEnemyCreatureCard(fromEnemyPointOfView);

                int playerAttack = player.attack;
                int playerDefense = player.defense;
                player.attack = enemy.attack;
                player.defense = enemy.defense;
                enemy.attack = playerAttack;
                enemy.defense = playerDefense;
            }
        };
    }

    private static Card generateDefaultCreatureCard(int id, int neededSlots, SparseBooleanArray obtainedList, SparseBooleanArray inDeckList) {
        Card card = new Card();
        card.id = id;
        card.isObtained = obtainedList.get(card.id);
        card.isInDeck = inDeckList.get(card.id);
        card.neededSlots = neededSlots;
        card.price = card.neededSlots * 50;

        ALL_CARDS_MAP.put(card.id, card);

        return card;
    }

    private static Card generateDefaultSupportCard(int id, int neededSlots, SparseBooleanArray obtainedList, SparseBooleanArray inDeckList) {
        Card card = new Card();
        card.id = id;
        card.type = Type.SUPPORT;
        card.isObtained = obtainedList.get(card.id);
        card.isInDeck = inDeckList.get(card.id);
        card.neededSlots = neededSlots;
        card.price = card.neededSlots * 50;

        ALL_CARDS_MAP.put(card.id, card);

        return card;
    }

    private static void checkCreatureCard(Card card) {
        int acceptableSum = card.neededSlots * 3;
        float margin = acceptableSum / 100f * 25f;
        if (card.attack + card.defense > acceptableSum + margin || card.attack + card.defense < acceptableSum - margin) {
            throw new IllegalStateException();
        }
    }
}
