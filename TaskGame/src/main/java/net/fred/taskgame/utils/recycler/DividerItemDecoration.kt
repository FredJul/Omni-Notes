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

package net.fred.taskgame.utils.recycler

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.View

class DividerItemDecoration : RecyclerView.ItemDecoration {

    private var divider: Drawable? = null
    private var showFirstDivider = false
    private var showLastDivider = false

    internal var orientation = -1

    constructor(context: Context, showFirstDivider: Boolean, showLastDivider: Boolean) : this(context, null, showFirstDivider, showLastDivider)

    @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) {
        val a = context
                .obtainStyledAttributes(attrs, intArrayOf(android.R.attr.listDivider))
        divider = a.getDrawable(0)
        a.recycle()
    }

    constructor(context: Context, attrs: AttributeSet?, showFirstDivider: Boolean, showLastDivider: Boolean) : this(context, attrs) {
        this.showFirstDivider = showFirstDivider
        this.showLastDivider = showLastDivider
    }

    constructor(context: Context, resId: Int) {
        divider = ContextCompat.getDrawable(context, resId)
    }

    constructor(context: Context, resId: Int, showFirstDivider: Boolean, showLastDivider: Boolean) : this(context, resId) {
        this.showFirstDivider = showFirstDivider
        this.showLastDivider = showLastDivider
    }

    constructor(divider: Drawable) {
        this.divider = divider
    }

    constructor(divider: Drawable, showFirstDivider: Boolean, showLastDivider: Boolean) : this(divider) {
        this.showFirstDivider = showFirstDivider
        this.showLastDivider = showLastDivider
    }

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State?) {
        super.getItemOffsets(outRect, view, parent, state)
        if (divider == null) {
            return
        }

        val position = parent.getChildAdapterPosition(view)
        if (position == RecyclerView.NO_POSITION || position == 0 && !showFirstDivider) {
            return
        }

        if (orientation == -1)
            getOrientation(parent)

        if (orientation == LinearLayoutManager.VERTICAL) {
            outRect.top = divider!!.intrinsicHeight
            if (showLastDivider && position == state!!.itemCount - 1) {
                outRect.bottom = outRect.top
            }
        } else {
            outRect.left = divider!!.intrinsicWidth
            if (showLastDivider && position == state!!.itemCount - 1) {
                outRect.right = outRect.left
            }
        }
    }

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State?) {
        if (divider == null) {
            super.onDrawOver(c, parent, state)
            return
        }

        // Initialization needed to avoid compiler warning
        var left = 0
        var right = 0
        var top = 0
        var bottom = 0
        val size: Int
        val orientation = if (orientation != -1) orientation else getOrientation(parent)
        val childCount = parent.childCount

        if (orientation == LinearLayoutManager.VERTICAL) {
            size = divider!!.intrinsicHeight
            left = parent.paddingLeft
            right = parent.width - parent.paddingRight
        } else { //horizontal
            size = divider!!.intrinsicWidth
            top = parent.paddingTop
            bottom = parent.height - parent.paddingBottom
        }

        for (i in (if (showFirstDivider) 0 else 1)..childCount - 1) {
            val child = parent.getChildAt(i)
            val params = child.layoutParams as RecyclerView.LayoutParams

            if (orientation == LinearLayoutManager.VERTICAL) {
                top = child.top - params.topMargin - size
                bottom = top + size
            } else { //horizontal
                left = child.left - params.leftMargin
                right = left + size
            }
            divider!!.setBounds(left, top, right, bottom)
            divider!!.draw(c)
        }

        // show last divider
        if (showLastDivider && childCount > 0) {
            val child = parent.getChildAt(childCount - 1)
            if (parent.getChildAdapterPosition(child) == state!!.itemCount - 1) {
                val params = child.layoutParams as RecyclerView.LayoutParams
                if (orientation == LinearLayoutManager.VERTICAL) {
                    top = child.bottom + params.bottomMargin
                    bottom = top + size
                } else { // horizontal
                    left = child.right + params.rightMargin
                    right = left + size
                }
                divider!!.setBounds(left, top, right, bottom)
                divider!!.draw(c)
            }
        }
    }

    private fun getOrientation(parent: RecyclerView): Int {
        if (orientation == -1) {
            if (parent.layoutManager is LinearLayoutManager) {
                val layoutManager = parent.layoutManager as LinearLayoutManager
                orientation = layoutManager.orientation
            } else {
                throw IllegalStateException(
                        "DividerItemDecoration can only be used with a LinearLayoutManager.")
            }
        }
        return orientation
    }
}