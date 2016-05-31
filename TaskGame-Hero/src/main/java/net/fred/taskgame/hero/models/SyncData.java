package net.fred.taskgame.hero.models;

import com.google.gson.annotations.Expose;
import com.raizlabs.android.dbflow.sql.language.Select;

import net.fred.taskgame.hero.utils.PrefUtils;

public class SyncData {
    @Expose
    public long lastSyncDate;
    @Expose
    public boolean isFemaleHero;
    @Expose
    public Card[] cards;

    public static SyncData getLastData() {
        SyncData syncData = new SyncData();
        syncData.lastSyncDate = System.currentTimeMillis();
        syncData.isFemaleHero = PrefUtils.getBoolean(PrefUtils.PREF_IS_FEMALE_HERO, false);
        syncData.cards = new Select().from(Card.class).queryList().toArray(new Card[]{});
        return syncData;
    }
}