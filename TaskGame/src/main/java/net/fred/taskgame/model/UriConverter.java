package net.fred.taskgame.model;

import android.net.Uri;

import com.raizlabs.android.dbflow.converter.TypeConverter;

@com.raizlabs.android.dbflow.annotation.TypeConverter
public class UriConverter extends TypeConverter<String, Uri> {

    @Override
    public String getDBValue(Uri model) {
        return model.toString();
    }

    @Override
    public Uri getModelValue(String data) {
        return Uri.parse(data);
    }
}