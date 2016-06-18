package net.fred.taskgame.hero.models;

import android.util.SparseBooleanArray;

import com.google.gson.annotations.Expose;
import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.sql.language.Select;
import com.raizlabs.android.dbflow.structure.BaseModel;

import net.fred.taskgame.hero.R;

import org.parceler.Parcel;

import java.util.ArrayList;
import java.util.List;

@Parcel
@Table(database = AppDatabase.class)
public class Level extends BaseModel {

    public final static int INVALID_ID = 0;

    private final static List<Level> ALL_LEVELS_LIST = new ArrayList<>();

    @PrimaryKey
    @Expose
    public int levelNumber;

    @Column
    @Expose
    public boolean isCompleted;

    public transient int enemyIconResId = INVALID_ID;

    public transient int specialMusicResId = INVALID_ID;

    public transient List<Card> enemyCards = new ArrayList<>();

    public static List<Level> getAllLevelsList() {
        return ALL_LEVELS_LIST;
    }

    public static int getCorrespondingDeckSlots() {
        int lastCompletedLevel = 0;
        for (Level level : ALL_LEVELS_LIST) {
            if (!level.isCompleted) {
                break;
            }
            lastCompletedLevel++;
        }

        if (lastCompletedLevel < 1) {
            return 2;
        } else if (lastCompletedLevel < 2) {
            return 3;
        } else if (lastCompletedLevel < 4) {
            return 6;
        } else {
            return 8;
        }
    }

    public static void populate() {
        ALL_LEVELS_LIST.clear();

        SparseBooleanArray completedList = new SparseBooleanArray();
        for (Level level : new Select().from(Level.class).queryList()) {
            completedList.append(level.levelNumber, level.isCompleted);
        }

        Level level = new Level();
        level.levelNumber = 1;
        level.enemyIconResId = R.drawable.hero_female;
        level.isCompleted = completedList.get(level.levelNumber);
        level.enemyCards.add(Card.getAllCardsMap().get(Card.CREATURE_SKELETON_ARCHER));
        ALL_LEVELS_LIST.add(level);

        level = new Level();
        level.levelNumber = 2;
        level.enemyIconResId = R.drawable.hero_female;
        level.isCompleted = completedList.get(level.levelNumber);
        level.enemyCards.add(Card.getAllCardsMap().get(Card.CREATURE_SKELETON_ARCHER));
        level.enemyCards.add(Card.getAllCardsMap().get(Card.CREATURE_SKELETON_ARCHER));
        ALL_LEVELS_LIST.add(level);

        level = new Level();
        level.levelNumber = 3;
        level.enemyIconResId = R.drawable.hero_female;
        level.isCompleted = completedList.get(level.levelNumber);
        level.enemyCards.add(Card.getAllCardsMap().get(Card.CREATURE_TROLL));
        level.enemyCards.add(Card.getAllCardsMap().get(Card.CREATURE_SKELETON_ARCHER));
        ALL_LEVELS_LIST.add(level);

        level = new Level();
        level.levelNumber = 4;
        level.enemyIconResId = R.drawable.hero_female;
        level.isCompleted = completedList.get(level.levelNumber);
        level.enemyCards.add(Card.getAllCardsMap().get(Card.CREATURE_TROLL));
        level.enemyCards.add(Card.getAllCardsMap().get(Card.CREATURE_SKELETON_ARCHER));
        level.enemyCards.add(Card.getAllCardsMap().get(Card.CREATURE_TROLL));
        ALL_LEVELS_LIST.add(level);

        level = new Level();
        level.levelNumber = 5;
        level.enemyIconResId = R.drawable.hero_female;
        level.specialMusicResId = R.raw.boss_theme;
        level.isCompleted = completedList.get(level.levelNumber);
        level.enemyCards.add(Card.getAllCardsMap().get(Card.CREATURE_TROLL));
        level.enemyCards.add(Card.getAllCardsMap().get(Card.CREATURE_SKELETON_ARCHER));
        level.enemyCards.add(Card.getAllCardsMap().get(Card.CREATURE_TROLL));
        level.enemyCards.add(Card.getAllCardsMap().get(Card.CREATURE_SKELETON_ARCHER));
        ALL_LEVELS_LIST.add(level);
    }
}
