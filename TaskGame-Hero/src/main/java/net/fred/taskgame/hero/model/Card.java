package net.fred.taskgame.hero.model;

import android.databinding.BindingAdapter;
import android.util.SparseArray;
import android.widget.ImageView;

import com.google.gson.annotations.Expose;
import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.structure.BaseModel;

import net.fred.taskgame.hero.R;

import org.parceler.Parcel;

@Parcel
@Table(database = AppDatabase.class)
public class Card extends BaseModel implements Cloneable {

    private transient static SparseArray<Card> sAllCardsMap = new SparseArray<>();

    public enum Type {CREATURE, SUPPORT}

    public enum SupportAction {PLAYER_ATTACK_MULT, PLAYER_DEFENSE_MULT, ENEMY_ATTACK_DIV}

    public final static int CREATURE_SKELETON_ARCHER = 0;
    public final static int CREATURE_ORC_ARCHER = 1;

    public final static int SUPPORT_POWER_POTION = 1000;
    public final static int SUPPORT_WEAPON_EROSION = 1001;

    public final static int INVALID_ID = 0;

    @PrimaryKey
    @Expose
    public int id = INVALID_ID;

    @Column
    @Expose
    public Type type = Type.CREATURE;

    @Column
    @Expose
    public String name = "";

    @Column
    @Expose
    public String desc = "";

    @Column
    @Expose
    public int level;

    @Column
    @Expose
    public int attack;

    @Column
    @Expose
    public int defense;

    @Column
    @Expose
    public int iconResId = INVALID_ID;

    @Column
    @Expose
    public int price;

    @Column
    @Expose
    public boolean obtained;

    @Column
    @Expose
    public SupportAction supportAction = SupportAction.PLAYER_ATTACK_MULT;

    public Card() {
    }

    @Override
    public Card clone() {
        Card card = new Card();
        card.id = id;
        card.type = type;
        card.name = name;
        card.desc = desc;
        card.level = level;
        card.attack = attack;
        card.defense = defense;
        card.iconResId = iconResId;
        card.price = price;
        card.obtained = obtained;
        card.supportAction = supportAction;
        return card;
    }

    @BindingAdapter("android:src")
    public static void setSrc(ImageView view, int res) {
        view.setImageResource(res);
    }

    public static SparseArray<Card> getAllCardsMap() {
        return sAllCardsMap;
    }

    public static void populate() {
        Card cardToAdd = new Card();
        cardToAdd.id = CREATURE_SKELETON_ARCHER;
        cardToAdd.name = "Skeleton Archer";
        cardToAdd.level = 2;
        cardToAdd.attack = 2;
        cardToAdd.defense = 5;
        cardToAdd.desc = "Deads are not deads, and strangely know how to send arrows";
        cardToAdd.iconResId = R.drawable.skeleton_archer;
        sAllCardsMap.append(cardToAdd.id, cardToAdd);

        cardToAdd = new Card();
        cardToAdd.id = CREATURE_ORC_ARCHER;
        cardToAdd.name = "Orc Archer";
        cardToAdd.level = 3;
        cardToAdd.attack = 2;
        cardToAdd.defense = 6;
        cardToAdd.desc = "You already dead!";
        cardToAdd.iconResId = R.drawable.orc_archer;
        sAllCardsMap.append(cardToAdd.id, cardToAdd);

        cardToAdd = new Card();
        cardToAdd.id = SUPPORT_POWER_POTION;
        cardToAdd.type = Card.Type.SUPPORT;
        cardToAdd.supportAction = Card.SupportAction.PLAYER_ATTACK_MULT;
        cardToAdd.name = "Fake potion of invincibility";
        cardToAdd.level = 3;
        cardToAdd.desc = "Your creature feel invincible and run into the target with a loud war cry.\nAttack multiplied by 2\nDefense divided by 1.5";
        cardToAdd.iconResId = R.drawable.red_potion;
        sAllCardsMap.append(cardToAdd.id, cardToAdd);

        cardToAdd = new Card();
        cardToAdd.id = SUPPORT_WEAPON_EROSION;
        cardToAdd.type = Card.Type.SUPPORT;
        cardToAdd.supportAction = Card.SupportAction.ENEMY_ATTACK_DIV;
        cardToAdd.name = "Weapon erosion";
        cardToAdd.level = 4;
        cardToAdd.desc = "Strangely your enemy weapon starts to run into pieces.\nEnemy attack divided by 2";
        cardToAdd.iconResId = R.drawable.erode_weapon;
        sAllCardsMap.append(cardToAdd.id, cardToAdd);
    }
}
