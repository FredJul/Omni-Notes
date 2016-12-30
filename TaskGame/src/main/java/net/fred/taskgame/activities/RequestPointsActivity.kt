/*
 * Copyright (c) 2012-2017 Frederic Julian
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
 * along with this program.  If not, see <http:></http:>//www.gnu.org/licenses/>.
 */

package net.fred.taskgame.activities

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import net.fred.taskgame.R
import net.fred.taskgame.utils.DbUtils
import net.fred.taskgame.utils.PrefUtils

class RequestPointsActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Retrieving intent
        val pointsNeeded = intent.getLongExtra(INTENT_EXTRA_POINT_AMOUNT_NEEDED, 0)
        val currentPoints = PrefUtils.getLong(PrefUtils.PREF_CURRENT_POINTS, 0)

        if (currentPoints < pointsNeeded) {
            AlertDialog.Builder(this)
                    .setTitle(R.string.app_name)
                    .setIcon(R.mipmap.ic_launcher)
                    .setMessage(if (currentPoints <= 0) getString(R.string.no_points) else getString(R.string.not_enough_points, currentPoints))
                    .setNegativeButton(android.R.string.no) { dialog, id -> dialog.cancel() }
                    .setPositiveButton(android.R.string.yes) { dialog, id ->
                        startActivity(Intent(this@RequestPointsActivity, MainActivity::class.java))
                        dialog.cancel()
                    }
                    .setOnCancelListener {
                        setResult(Activity.RESULT_CANCELED)
                        finish()
                    }.show()
        } else {
            AlertDialog.Builder(this)
                    .setTitle(R.string.app_name)
                    .setIcon(R.mipmap.ic_launcher)
                    .setMessage(getString(R.string.points_needed, pointsNeeded))
                    .setPositiveButton(android.R.string.ok) { dialog, id ->
                        DbUtils.updateCurrentPoints(currentPoints - pointsNeeded)
                        setResult(Activity.RESULT_OK)
                        finish()
                    }
                    .setNegativeButton(android.R.string.cancel) { dialog, id -> dialog.cancel() }
                    .setOnCancelListener {
                        setResult(Activity.RESULT_CANCELED)
                        finish()
                    }.show()
        }
    }

    companion object {
        val INTENT_EXTRA_POINT_AMOUNT_NEEDED = "taskgame.intent.extra.POINT_AMOUNT_NEEDED"
    }
}
