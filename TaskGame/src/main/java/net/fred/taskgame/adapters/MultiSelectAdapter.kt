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

package net.fred.taskgame.adapters

import android.support.v7.widget.RecyclerView
import android.util.SparseBooleanArray
import net.fred.taskgame.utils.recycler.ItemActionAdapter
import net.fred.taskgame.utils.recycler.ItemActionListener
import net.fred.taskgame.utils.recycler.ItemActionViewHolder

abstract class MultiSelectAdapter<VH>(private val mListener: ItemActionListener, private val mRecyclerView: RecyclerView) : RecyclerView.Adapter<VH>(), ItemActionAdapter where VH : RecyclerView.ViewHolder, VH : ItemActionViewHolder {

    private val mSelectedItems = SparseBooleanArray()

    fun isItemSelected(position: Int): Boolean {
        return mSelectedItems.get(position)
    }

    fun selectAll() {
        for (i in 0..itemCount - 1) {
            if (!isItemSelected(i)) {
                updateView(mRecyclerView, i, false)
                mSelectedItems.put(i, true)
            }
        }
    }

    fun clearSelections() {
        for (i in 0..itemCount - 1) {
            updateView(mRecyclerView, i, true)
        }
        mSelectedItems.clear()
    }

    val selectedItemCount: Int
        get() = mSelectedItems.size()

    val selectedItems: IntArray
        get() {
            val itemsPos = IntArray(mSelectedItems.size())
            for (i in 0..mSelectedItems.size() - 1) {
                itemsPos[i] = mSelectedItems.keyAt(i)
            }
            return itemsPos
        }

    override fun onBindViewHolder(holder: VH, position: Int) {
        if (isItemSelected(position)) {
            holder.onItemSelected()
        } else {
            holder.onItemClear()
        }
    }

    protected fun toggleSelection(select: Boolean, position: Int) {
        updateView(mRecyclerView, position, !select)

        if (!select) {
            mSelectedItems.delete(position)
        } else {
            mSelectedItems.put(position, true)
        }
    }

    private fun updateView(recyclerView: RecyclerView, position: Int, isCurrentlySelected: Boolean) {
        val child = mRecyclerView.layoutManager.findViewByPosition(position)
        if (child != null) {
            val viewHolder = recyclerView.getChildViewHolder(child) as ItemActionViewHolder
            // Let the view holder know that this item is being moved or dragged
            if (isCurrentlySelected) {
                viewHolder.onItemClear()
            } else {
                viewHolder.onItemSelected()
            }
        }
    }

    override fun onItemMove(fromPosition: Int, toPosition: Int): Boolean {
        return mListener.onItemMove(fromPosition, toPosition)
    }

    override fun onItemMoveFinished() {
        mListener.onItemMoveFinished()
    }

    override fun onItemSwiped(position: Int) {
        mListener.onItemSwiped(position)
    }

    override fun onItemSelected(position: Int) {
        toggleSelection(true, position)
        mListener.onItemSelected(position)
    }
}
