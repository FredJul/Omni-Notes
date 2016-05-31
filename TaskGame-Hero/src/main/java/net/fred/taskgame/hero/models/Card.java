package net.fred.taskgame.hero.models;

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
        Card card = new Card();
        card.id = CREATURE_SKELETON_ARCHER;
        card.name = "Skeleton Archer";
        card.level = 2;
        card.attack = 2;
        card.defense = 5;
        card.desc = "Deads are not deads, and strangely know how to send arrows";
        card.iconResId = R.drawable.skeleton_archer;
        sAllCardsMap.append(card.id, card);

        card = new Card();
        card.id = CREATURE_ORC_ARCHER;
        card.name = "Orc Archer";
        card.level = 3;
        card.attack = 2;
        card.defense = 6;
        card.desc = "You already dead!";
        card.iconResId = R.drawable.orc_archer;
        sAllCardsMap.append(card.id, card);

        card = new Card();
        card.id = SUPPORT_POWER_POTION;
        card.type = Card.Type.SUPPORT;
        card.supportAction = Card.SupportAction.PLAYER_ATTACK_MULT;
        card.name = "Fake potion of invincibility";
        card.level = 3;
        card.desc = "Your creature feel invincible and run into the target with a loud war cry.\nAttack multiplied by 2\nDefense divided by 1.5";
        card.iconResId = R.drawable.red_potion;
        sAllCardsMap.append(card.id, card);

        card = new Card();
        card.id = SUPPORT_WEAPON_EROSION;
        card.type = Card.Type.SUPPORT;
        card.supportAction = Card.SupportAction.ENEMY_ATTACK_DIV;
        card.name = "Weapon erosion";
        card.level = 4;
        card.desc = "Strangely your enemy weapon starts to run into pieces.\nEnemy attack divided by 2";
        card.iconResId = R.drawable.erode_weapon;
        sAllCardsMap.append(card.id, card);
    }
}
