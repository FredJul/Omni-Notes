package net.fred.taskgame.hero.models;

import android.content.Context;
import android.support.annotation.DrawableRes;
import android.support.annotation.RawRes;
import android.util.SparseBooleanArray;

import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.sql.language.Select;
import com.raizlabs.android.dbflow.structure.BaseModel;

import net.fred.taskgame.hero.R;

import org.parceler.Parcel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Parcel
@Table(database = AppDatabase.class)
public class Level extends BaseModel {

    public final static int INVALID_ID = 0;
    public final static Map<String, Integer> STORY_CHARS_DRAWABLE_MAP = new HashMap<String, Integer>() {
        {
            put("hero", R.drawable.invoker_female);
            put("school_friend", R.drawable.invoker_male);
        }
    };
    private final static List<Level> ALL_LEVELS_LIST = new ArrayList<>();

    @PrimaryKey
    public int levelNumber;

    @Column
    public boolean isCompleted;

    public transient List<Card> enemyCards = new ArrayList<>();

    @RawRes
    public transient int battleMusicResId = INVALID_ID;

    @RawRes
    public transient int startStoryMusicResId = INVALID_ID;
    @RawRes
    public transient int endStoryMusicResId = INVALID_ID;

    public
    @DrawableRes
    int getEnemyIcon(Context context) {
        return STORY_CHARS_DRAWABLE_MAP.get(context.getResources().getStringArray(R.array.level_stories)[levelNumber * 3 - 3]);
    }

    public String getStartStory(Context context) {
        return context.getResources().getStringArray(R.array.level_stories)[levelNumber * 3 - 2];
    }

    public String getEndStory(Context context) {
        return context.getResources().getStringArray(R.array.level_stories)[levelNumber * 3 - 1];
    }

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

        return getCorrespondingDeckSlots(lastCompletedLevel);
    }


    public static int getCorrespondingDeckSlots(int lastCompletedLevelNumber) {
        // Increase quickly, but then slow down
        // See graph on: http://fooplot.com/plot/lpum8k6yac
        return (int) Math.pow(Math.log10(Math.pow(lastCompletedLevelNumber + 3, 3)), 2);
    }

    public static void populate() {
        ALL_LEVELS_LIST.clear();

        SparseBooleanArray completedList = new SparseBooleanArray();
        for (Level level : new Select().from(Level.class).queryList()) {
            completedList.append(level.levelNumber, level.isCompleted);
        }

        int levelNumber = 1;

        /**************** Level 1 to 10 *****************
         Available slots == levelNumber + 1
         ************************************************/
        Level level = generateLevel(levelNumber++, completedList);
        level.addEnemyCard(Card.CREATURE_SYLPH);

        level = generateLevel(levelNumber++, completedList);
        level.addEnemyCard(Card.CREATURE_SKELETON);

        level = generateLevel(levelNumber++, completedList);
        level.addEnemyCard(Card.CREATURE_SYLPH).addEnemyCard(Card.CREATURE_SKELETON);

        level = generateLevel(levelNumber++, completedList);
        level.addEnemyCard(Card.CREATURE_SKELETON).addEnemyCard(Card.CREATURE_MERMAN).addEnemyCard(Card.CREATURE_SYLPH);

        level = generateLevel(levelNumber++, completedList);
        level.addEnemyCard(Card.CREATURE_SKELETON).addEnemyCard(Card.CREATURE_TROLL).addEnemyCard(Card.CREATURE_SYLPH);

        level = generateLevel(levelNumber++, completedList);
        level.battleMusicResId = R.raw.boss_theme;
        level.addEnemyCard(Card.CREATURE_ENCHANTED_TREE).addEnemyCard(Card.CREATURE_LICH);
    }

    private static Level generateLevel(int levelNumber, SparseBooleanArray completedList) {
        Level level = new Level();
        level.levelNumber = levelNumber;
        level.isCompleted = completedList.get(level.levelNumber);
        ALL_LEVELS_LIST.add(level);
        return level;
    }

    private Level addEnemyCard(int cardId) {
        enemyCards.add(Card.getAllCardsMap().get(cardId));
        return this;
    }

}
