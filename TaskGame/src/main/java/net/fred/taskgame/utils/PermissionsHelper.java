/*
 * Copyright (C) 2015 Federico Iosue (federico.iosue@gmail.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.fred.taskgame.utils;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.View;

import com.tbruyelle.rxpermissions.RxPermissions;

import net.fred.taskgame.R;
import net.fred.taskgame.model.listeners.OnPermissionRequestedListener;

import rx.functions.Action1;


public class PermissionsHelper {

    public static void requestPermission(final Activity activity, final String permission, final int rationaleDescription,
                                         final OnPermissionRequestedListener onPermissionRequestedListener) {

        if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                Snackbar.make(activity.getWindow().getDecorView().findViewById(android.R.id.content), rationaleDescription, Snackbar.LENGTH_INDEFINITE)
                        .setAction(R.string.ok, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                requestPermissionExecute(activity, permission, onPermissionRequestedListener);
                            }
                        })
                        .show();
            } else {
                requestPermissionExecute(activity, permission, onPermissionRequestedListener);
            }
        } else {
            onPermissionRequestedListener.onPermissionGranted();
        }
    }


    private static void requestPermissionExecute(final Activity activity, final String permission, final OnPermissionRequestedListener onPermissionRequestedListener) {

        RxPermissions.getInstance(activity)
                .request(permission)
                .subscribe(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean granted) {
                        if (granted) {
                            onPermissionRequestedListener.onPermissionGranted();
                        } else {
                            UiUtils.showWarningMessage(activity, R.string.permission_not_granted);
                        }
                    }
                });
    }
}