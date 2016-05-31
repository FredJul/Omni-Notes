package net.fred.taskgame.hero.models;

import com.google.gson.annotations.Expose;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.structure.BaseModel;

import org.parceler.Parcel;

import java.util.ArrayList;
import java.util.List;

@Parcel
@Table(database = AppDatabase.class)
public class Level extends BaseModel {

    private transient static List<Level> sAllLevelsList = new ArrayList<>();

    @PrimaryKey
    @Expose
    public int levelNumber;

    public transient List<Card> enemyCards = new ArrayList<>();

    public static List<Level> getAllLevelsList() {
        return sAllLevelsList;
    }

    public static void populate() {
        Level level = new Level();
        level.levelNumber = 1;
        level.enemyCards.add(Card.getAllCardsMap().get(Card.CREATURE_SKELETON_ARCHER));
        sAllLevelsList.add(level);

        level = new Level();
        level.levelNumber = 2;
        level.enemyCards.add(Card.getAllCardsMap().get(Card.CREATURE_ORC_ARCHER));
        level.enemyCards.add(Card.getAllCardsMap().get(Card.CREATURE_SKELETON_ARCHER));
        sAllLevelsList.add(level);

        level = new Level();
        level.levelNumber = 3;
        level.enemyCards.add(Card.getAllCardsMap().get(Card.CREATURE_ORC_ARCHER));
        level.enemyCards.add(Card.getAllCardsMap().get(Card.CREATURE_SKELETON_ARCHER));
        level.enemyCards.add(Card.getAllCardsMap().get(Card.CREATURE_ORC_ARCHER));
        sAllLevelsList.add(level);

        level = new Level();
        level.levelNumber = 4;
        level.enemyCards.add(Card.getAllCardsMap().get(Card.CREATURE_ORC_ARCHER));
        level.enemyCards.add(Card.getAllCardsMap().get(Card.CREATURE_SKELETON_ARCHER));
        level.enemyCards.add(Card.getAllCardsMap().get(Card.CREATURE_ORC_ARCHER));
        level.enemyCards.add(Card.getAllCardsMap().get(Card.CREATURE_SKELETON_ARCHER));
        sAllLevelsList.add(level);

        level = new Level();
        level.levelNumber = 5;
        level.enemyCards.add(Card.getAllCardsMap().get(Card.CREATURE_ORC_ARCHER));
        level.enemyCards.add(Card.getAllCardsMap().get(Card.CREATURE_SKELETON_ARCHER));
        level.enemyCards.add(Card.getAllCardsMap().get(Card.CREATURE_ORC_ARCHER));
        level.enemyCards.add(Card.getAllCardsMap().get(Card.CREATURE_SKELETON_ARCHER));
        level.enemyCards.add(Card.getAllCardsMap().get(Card.CREATURE_ORC_ARCHER));
        sAllLevelsList.add(level);
    }
}
