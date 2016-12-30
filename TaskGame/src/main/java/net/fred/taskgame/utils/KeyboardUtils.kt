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

import android.content.Context
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager

import net.fred.taskgame.activities.MainActivity

object KeyboardUtils {

    fun showKeyboard(view: View?) {
        if (view == null) {
            return
        }

        view.requestFocus()

        val inputManager = view.context.getSystemService(
                Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)

        (view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(view, 0)

        if (!isKeyboardShowed(view)) {
            inputManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
        }
    }

    fun isKeyboardShowed(view: View?): Boolean {
        if (view == null) {
            return false
        }
        val inputManager = view.context.getSystemService(
                Context.INPUT_METHOD_SERVICE) as InputMethodManager
        return inputManager.isActive(view)
    }

    fun hideKeyboard(view: View?) {
        if (view == null) {
            return
        }
        val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        if (!imm.isActive) {
            return
        }
        imm.hideSoftInputFromWindow(view.windowToken, 0)

        //		if (!isKeyboardShowed(view)) {
        //			imm.toggleSoftInput(InputMethodManager.HIDE_NOT_ALWAYS, InputMethodManager.RESULT_HIDDEN);
        //		}

    }

    fun hideKeyboard(mActivity: MainActivity) {
        mActivity.window.setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
    }
}
