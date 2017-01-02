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

import android.graphics.Color
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.item_task.view.*
import net.fred.taskgame.R
import net.fred.taskgame.models.Task
import net.fred.taskgame.utils.recycler.ItemActionListener
import net.fred.taskgame.utils.recycler.ItemActionViewHolder
import org.jetbrains.anko.onClick


class TaskAdapter(private val itemActionListener: ItemActionListener, recyclerView: RecyclerView, private var _tasks: MutableList<Task>) : MultiSelectAdapter<TaskAdapter.TaskViewHolder>(itemActionListener, recyclerView) {

    class TaskViewHolder(v: View) : RecyclerView.ViewHolder(v), ItemActionViewHolder {

        companion object {
            val SELECTED_COLOR = Color.parseColor("#e6e6e6")
        }

        override fun onItemSelected() {
            // Highlighted if is part of multi selection of tasks. Remember to search for child with card ui
            itemView.card.setCardBackgroundColor(SELECTED_COLOR)
        }

        override fun onItemClear() {
            itemView.card.setCardBackgroundColor(Color.WHITE)
        }
    }

    var tasks: MutableList<Task>
        get() = _tasks
        set(tasks) {
            _tasks = tasks
            notifyDataSetChanged()
        }

    init {
        setHasStableIds(true)
        recyclerView.adapter = this
    }

    override fun getItemId(position: Int): Long {
        return tasks[position].id!!.hashCode().toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return 0
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val v = inflater.inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(v)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)

        val task = tasks[position]

        // Init texts
        val titleAndContent = task.computeListItemTitleAndContent()
        holder.itemView.task_title.text = titleAndContent[0]
        if (titleAndContent[1].isNotEmpty()) {
            holder.itemView.task_content.text = titleAndContent[1]
            holder.itemView.task_content.visibility = View.VISIBLE
        } else {
            holder.itemView.task_content.visibility = View.GONE
        }
        holder.itemView.reward_points.text = task.pointReward.toString()

        // If category is set the color will be applied on the appropriate target
        if (task.category != null) {
            holder.itemView.category_marker.setBackgroundColor(task.category!!.color)
        } else {
            holder.itemView.category_marker.setBackgroundColor(Color.TRANSPARENT)
        }

        // set listeners
        holder.itemView.onClick {
            if (holder.adapterPosition != -1) { // seems -1 is sometimes possible...
                if (selectedItemCount > 0) {
                    if (isItemSelected(holder.adapterPosition)) {
                        toggleSelection(false, holder.adapterPosition)
                        itemActionListener.onItemUnselected(holder.adapterPosition)
                    } else {
                        toggleSelection(true, holder.adapterPosition)
                        itemActionListener.onItemSelected(holder.adapterPosition)
                    }
                } else {
                    itemActionListener.onItemClicked(holder.adapterPosition)
                }
            }
        }

        holder.itemView.checkbox.isChecked = task.finished
        holder.itemView.checkbox.onClick { itemActionListener.onItemSwiped(holder.adapterPosition) }

        // This is a small hack to avoid clicking on the item just after a D&D long press...
        holder.itemView.setOnLongClickListener { true }
    }

    override fun getItemCount(): Int {
        return tasks.size
    }
}
