package net.fred.taskgame.models.providers;

import android.content.ContentResolver;

import net.frju.androidquery.database.BaseContentDatabaseProvider;
import net.frju.androidquery.database.Resolver;
import net.frju.androidquery.gen.Q;

public class ContentDatabaseProvider extends BaseContentDatabaseProvider {

    public ContentDatabaseProvider(ContentResolver contentResolver) {
        super(contentResolver);
    }

    @Override
    protected String getAuthority() {
        return "net.fred.taskgame.models.providers.ModelContentProvider";
    }

    @Override
    protected Resolver getResolver() {
        return Q.getResolver();
    }
}