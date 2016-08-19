package net.fred.taskgame.models;

import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.structure.BaseModel;

import java.util.UUID;

public class IdBasedModel extends BaseModel {

    @PrimaryKey
    public String id;

    @Override
    public void save() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }

        super.save();
    }
}
