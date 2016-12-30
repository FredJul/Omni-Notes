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

package net.fred.taskgame.hero.utils

import android.support.v4.app.FragmentTransaction
import android.util.TypedValue

import net.fred.taskgame.hero.App
import net.fred.taskgame.hero.R

object UiUtils {

    enum class TransitionType {
        TRANSITION_FADE_IN
    }

    fun dpToPixel(dp: Int): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), App.context?.resources?.displayMetrics).toInt()
    }

    fun animateTransition(transaction: FragmentTransaction, transitionType: TransitionType) {
        when (transitionType) {
            UiUtils.TransitionType.TRANSITION_FADE_IN -> transaction.setCustomAnimations(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out)
        }
    }
}
