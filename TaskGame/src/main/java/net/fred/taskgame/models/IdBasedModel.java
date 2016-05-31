package net.fred.taskgame.models;

import android.database.Cursor;

import com.google.gson.annotations.Expose;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.sql.language.Method;
import com.raizlabs.android.dbflow.sql.language.Select;
import com.raizlabs.android.dbflow.sql.language.property.LongProperty;
import com.raizlabs.android.dbflow.structure.BaseModel;

public class IdBasedModel extends BaseModel {

    public final static long INVALID_ID = 0;

    @PrimaryKey
    @Expose
    public long id = INVALID_ID;

    @Override
    public void save() {
        if (id <= 0) {
            Cursor maxIdCursor = new Select(Method.max(new LongProperty(this.getClass(), "id"))).from(this.getClass()).query();
            if (maxIdCursor != null) {
                if (maxIdCursor.moveToFirst()) {
                    id = maxIdCursor.getLong(0) + 1;
                }
                maxIdCursor.close();
            }
        }

        super.save();
    }
}
