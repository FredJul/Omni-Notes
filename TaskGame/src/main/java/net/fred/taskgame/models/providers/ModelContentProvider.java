package net.fred.taskgame.models.providers;

import net.fred.taskgame.models.Task;
import net.frju.androidquery.database.BaseContentProvider;
import net.frju.androidquery.database.BaseLocalDatabaseProvider;
import net.frju.androidquery.gen.Q;

public class ModelContentProvider extends BaseContentProvider {

    @Override
    protected BaseLocalDatabaseProvider getLocalSQLProvider() {
        Q.init(getContext());
        return Q.getResolver().getLocalDatabaseProviderForModel(Task.class);
    }
}
