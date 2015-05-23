package net.fred.taskgame.model;

import android.database.Cursor;

import com.google.gson.annotations.Expose;
import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.sql.language.Select;
import com.raizlabs.android.dbflow.structure.BaseModel;

public abstract class AutoIncrementModel extends BaseModel {

    @Column
    @PrimaryKey
    @Expose
    public long id;

    @Override
    public void save() {
        if (id <= 0) {
            Cursor maxIdCursor = new Select().method("MAX", "id").from(this.getClass()).query();
            if (maxIdCursor != null) {
                if (maxIdCursor.moveToFirst()) {
                    id = maxIdCursor.getLong(0) + 1;
                    super.save();
                }
                maxIdCursor.close();
            }
        } else {
            super.save();
        }
    }
}
