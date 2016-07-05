package net.fred.taskgame.hero.models;

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

    public transient int enemyIconResId = INVALID_ID;
    public transient List<Card> enemyCards = new ArrayList<>();

    public transient int battleMusicResId = INVALID_ID;

    public transient String startStory;
    public transient int startStoryMusicResId = INVALID_ID;
    public transient String endStory;
    public transient int endStoryMusicResId = INVALID_ID;

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

        Level level = new Level();
        level.levelNumber = 1; // slots=3
        level.enemyIconResId = R.drawable.invoker_male;
        level.isCompleted = completedList.get(level.levelNumber);
        level.enemyCards.add(Card.getAllCardsMap().get(Card.CREATURE_SKELETON));
        level.startStory = "school_friend@R:So, finally you'll get graduated tomorrow! Are you stressed by the final examination?\nhero@L:Well, not really. This school is not the best invoker school of the country and I don't expect the examination to be hard.";
        level.endStory = "school_friend@R:As always, my invocation is not as powerful as yours.\nhero@L:Don't worry, I didn't really expected much from you. But you still have time to learn.";
        ALL_LEVELS_LIST.add(level);

        level = new Level();
        level.levelNumber = 2; // slots=4
        level.enemyIconResId = R.drawable.invoker_male;
        level.isCompleted = completedList.get(level.levelNumber);
        level.enemyCards.add(Card.getAllCardsMap().get(Card.CREATURE_SKELETON));
        level.enemyCards.add(Card.getAllCardsMap().get(Card.CREATURE_SKELETON));
        ALL_LEVELS_LIST.add(level);

        level = new Level();
        level.levelNumber = 3; // slots=5
        level.enemyIconResId = R.drawable.invoker_male;
        level.isCompleted = completedList.get(level.levelNumber);
        level.enemyCards.add(Card.getAllCardsMap().get(Card.CREATURE_TROLL));
        level.enemyCards.add(Card.getAllCardsMap().get(Card.CREATURE_SKELETON));
        ALL_LEVELS_LIST.add(level);

        level = new Level();
        level.levelNumber = 4; // slots=6
        level.enemyIconResId = R.drawable.invoker_male;
        level.isCompleted = completedList.get(level.levelNumber);
        level.enemyCards.add(Card.getAllCardsMap().get(Card.CREATURE_TROLL));
        level.enemyCards.add(Card.getAllCardsMap().get(Card.CREATURE_SKELETON));
        level.enemyCards.add(Card.getAllCardsMap().get(Card.CREATURE_TROLL));
        ALL_LEVELS_LIST.add(level);

        level = new Level();
        level.levelNumber = 5; // slots=7
        level.enemyIconResId = R.drawable.invoker_male;
        level.battleMusicResId = R.raw.boss_theme;
        level.isCompleted = completedList.get(level.levelNumber);
        level.enemyCards.add(Card.getAllCardsMap().get(Card.CREATURE_TROLL));
        level.enemyCards.add(Card.getAllCardsMap().get(Card.CREATURE_SKELETON));
        level.enemyCards.add(Card.getAllCardsMap().get(Card.CREATURE_ENCHANTED_TREE));
        ALL_LEVELS_LIST.add(level);

        level = new Level();
        level.levelNumber = 6; // slots=8
        level.enemyIconResId = R.drawable.invoker_male;
        level.battleMusicResId = R.raw.boss_theme;
        level.isCompleted = completedList.get(level.levelNumber);
        level.enemyCards.add(Card.getAllCardsMap().get(Card.CREATURE_SKELETON));
        level.enemyCards.add(Card.getAllCardsMap().get(Card.CREATURE_ENCHANTED_TREE));
        level.enemyCards.add(Card.getAllCardsMap().get(Card.CREATURE_TROLL));
        level.enemyCards.add(Card.getAllCardsMap().get(Card.CREATURE_ENCHANTED_TREE));
        ALL_LEVELS_LIST.add(level);
    }
}
