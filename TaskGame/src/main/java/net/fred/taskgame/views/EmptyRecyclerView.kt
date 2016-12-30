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
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.View

import net.fred.taskgame.utils.UiUtils

class EmptyRecyclerView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) : RecyclerView(context, attrs, defStyle) {

    private val observer = object : RecyclerView.AdapterDataObserver() {
        override fun onChanged() {
            super.onChanged()
            checkIfEmpty()
        }

        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            super.onItemRangeInserted(positionStart, itemCount)
            checkIfEmpty()
        }

        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
            super.onItemRangeRemoved(positionStart, itemCount)
            checkIfEmpty()
        }
    }

    private var emptyView: View? = null

    override fun setAdapter(adapter: RecyclerView.Adapter<*>?) {
        val oldAdapter = getAdapter()
        oldAdapter?.unregisterAdapterDataObserver(observer)

        adapter?.registerAdapterDataObserver(observer)
        super.setAdapter(adapter)
        checkIfEmpty()
    }

    override fun swapAdapter(adapter: RecyclerView.Adapter<*>?, removeAndRecycleExistingViews: Boolean) {
        val oldAdapter = getAdapter()
        oldAdapter?.unregisterAdapterDataObserver(observer)

        adapter?.registerAdapterDataObserver(observer)
        super.swapAdapter(adapter, removeAndRecycleExistingViews)
        checkIfEmpty()
    }

    fun setEmptyView(mEmptyView: View?) {
        this.emptyView = mEmptyView
        checkIfEmpty()
    }

    override fun getMinFlingVelocity(): Int {
        return UiUtils.dpToPixel(500) // Nothing related to empty view, but the default velocity is clearly TOO sensible!
    }

    private fun checkIfEmpty() {
        if (emptyView != null && adapter != null) {
            emptyView!!.visibility = if (adapter.itemCount > 0) View.GONE else View.VISIBLE
        }
    }
}
