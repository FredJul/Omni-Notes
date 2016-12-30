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

package net.fred.taskgame.hero.views

import android.content.Context
import android.support.design.widget.BottomSheetBehavior
import android.support.design.widget.CoordinatorLayout
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class BugfixedBottomSheetBehavior<V : View> : BottomSheetBehavior<V> {

    constructor() : super()

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    override fun onTouchEvent(parent: CoordinatorLayout?, child: V, event: MotionEvent?): Boolean {
        if (state == BottomSheetBehavior.STATE_HIDDEN || state == BottomSheetBehavior.STATE_SETTLING) {
            return false
        }
        return super.onTouchEvent(parent, child, event)
    }

    override fun onInterceptTouchEvent(parent: CoordinatorLayout?, child: V, event: MotionEvent?): Boolean {
        if (state == BottomSheetBehavior.STATE_HIDDEN || state == BottomSheetBehavior.STATE_SETTLING) {
            return false
        }
        return super.onInterceptTouchEvent(parent, child, event)
    }
}
