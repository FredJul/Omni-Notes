package net.fred.taskgame.hero.models;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import net.frju.androidquery.database.BaseLocalDatabaseProvider;
import net.frju.androidquery.database.Resolver;
import net.frju.androidquery.gen.Q;

public class LocalDatabaseProvider extends BaseLocalDatabaseProvider {

    public LocalDatabaseProvider(Context context) {
        super(context);
    }

    @Override
    protected String getDbName() {
        return "game_data";
    }

    @Override
    protected int getDbVersion() {
        return 1;
    }

    @Override
    protected Resolver getResolver() {
        return Q.getResolver();
    }

    @Override
    protected void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        super.onUpgrade(db, oldVersion, newVersion);

        // Put here your migration code
    }
}