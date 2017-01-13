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

package net.fred.taskgame.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.AdapterView
import android.widget.Spinner

/**
 * A Spinner will normally call it's OnItemSelectedListener
 * when you use setSelection(...) in your initialization code.
 * This is usually unwanted behavior, and a common work-around
 * is to use spinner.post(...) with a Runnable to assign the
 * OnItemSelectedListener after layout.
 *
 * If you do not call setSelection(...) manually, the callback
 * may be called with the first item in the adapter you have
 * set. The common work-around for that is to count callbacks.
 *
 * While these workarounds usually *seem* to work, the callback
 * may still be called repeatedly for other reasons while the
 * selection hasn't actually changed. This will happen for
 * example, if the user has accessibility options enabled -
 * which is more common than you might think as several apps
 * use this for different purposes, like detecting which
 * notifications are active.
 *
 * Ideally, your OnItemSelectedListener callback should be
 * coded defensively so that no problem would occur even
 * if the callback was called repeatedly with the same values
 * without any user interaction, so no workarounds are needed.
 *
 * This class does that for you. It keeps track of the values
 * you have set with the setSelection(...) methods, and
 * proxies the OnItemSelectedListener callback so your callback
 * only gets called if the selected item's position differs
 * from the one you have set by code, or the first item if you
 * did not set it.
 *
 * This also means that if the user actually clicks the item
 * that was previously selected by code (or the first item
 * if you didn't set a selection by code), the callback will
 * not fire.
 */
class BugFixedSpinner : Spinner {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private var lastPosition = -1
    private var firstTrigger = true

    override fun setOnItemSelectedListener(listener: OnItemSelectedListener?) {
        if (listener == null) {
            super.setOnItemSelectedListener(null)
        } else {
            firstTrigger = true

            super.setOnItemSelectedListener(object : OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    if (firstTrigger) {
                        firstTrigger = false
                        lastPosition = position
                    } else {
                        if (position != lastPosition) {
                            lastPosition = position
                            listener.onItemSelected(parent, view, position, id)
                        }
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    if (firstTrigger) {
                        firstTrigger = false
                    } else {
                        if (-1 != lastPosition) {
                            lastPosition = -1
                            listener.onNothingSelected(parent)
                        }
                    }
                }
            })
        }
    }
}