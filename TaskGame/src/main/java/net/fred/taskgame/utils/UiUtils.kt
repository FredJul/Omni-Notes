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

package net.fred.taskgame.utils

import android.app.Activity
import android.support.annotation.StringRes
import android.support.design.widget.Snackbar
import android.support.v4.app.FragmentTransaction
import android.support.v4.content.ContextCompat
import android.util.TypedValue
import android.widget.TextView

import net.fred.taskgame.App
import net.fred.taskgame.R

object UiUtils {

    enum class TransitionType {
        TRANSITION_FADE_IN
    }

    enum class MessageType {
        TYPE_INFO, TYPE_WARN, TYPE_ERROR
    }

    fun dpToPixel(dp: Int): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), App.context?.resources?.displayMetrics).toInt()
    }

    fun animateTransition(transaction: FragmentTransaction, transitionType: TransitionType) {
        when (transitionType) {
            UiUtils.TransitionType.TRANSITION_FADE_IN -> transaction.setCustomAnimations(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out)
        }
    }

    fun showMessage(activity: Activity, @StringRes messageId: Int) {
        showMessage(activity, activity.getString(messageId), MessageType.TYPE_INFO)
    }

    fun showMessage(activity: Activity, @StringRes messageId: Int, type: MessageType) {
        showMessage(activity, activity.getString(messageId), type)
    }

    @JvmOverloads fun showMessage(activity: Activity, message: String, type: MessageType = MessageType.TYPE_INFO) {
        val snackbar = Snackbar.make(activity.findViewById(R.id.coordinator_layout), message, Snackbar.LENGTH_SHORT)
        when (type) {
            UiUtils.MessageType.TYPE_WARN -> {
                val textView = snackbar.view.findViewById(R.id.snackbar_text) as TextView
                textView.setTextColor(ContextCompat.getColor(activity, R.color.warning))
            }
            UiUtils.MessageType.TYPE_ERROR -> {
                val textView = snackbar.view.findViewById(R.id.snackbar_text) as TextView
                textView.setTextColor(ContextCompat.getColor(activity, R.color.error))
            }
            else -> {
            }
        }
        snackbar.show()
    }

    fun showWarningMessage(activity: Activity, @StringRes messageId: Int) {
        showMessage(activity, activity.getString(messageId), MessageType.TYPE_WARN)
    }

    fun showWarningMessage(activity: Activity, message: String) {
        showMessage(activity, message, MessageType.TYPE_WARN)
    }

    fun showErrorMessage(activity: Activity, @StringRes messageId: Int) {
        showMessage(activity, activity.getString(messageId), MessageType.TYPE_ERROR)
    }

    fun showErrorMessage(activity: Activity, message: String) {
        showMessage(activity, message, MessageType.TYPE_ERROR)
    }
}
