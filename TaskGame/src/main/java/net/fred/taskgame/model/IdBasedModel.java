package net.fred.taskgame.model;

import com.google.gson.annotations.Expose;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.structure.BaseModel;

public class IdBasedModel extends BaseModel {

    public final static long INVALID_ID = 0;

    @PrimaryKey(autoincrement = true, quickCheckAutoIncrement = true)
    @Expose
    public long id = INVALID_ID;
}
