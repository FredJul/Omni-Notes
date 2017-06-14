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

package net.fred.taskgame.hero.adapters

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.item_level.view.*
import net.fred.taskgame.hero.App
import net.fred.taskgame.hero.R
import net.fred.taskgame.hero.models.Level
import org.jetbrains.anko.sdk21.coroutines.onClick

class LevelSelectionAdapter(val levels: List<Level>, private val mItemListener: RecyclerViewItemListener) : RecyclerView.Adapter<LevelSelectionAdapter.LevelViewHolder>() {

    class LevelViewHolder(v: View) : RecyclerView.ViewHolder(v)

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        return levels[position].levelNumber.toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return 0
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LevelViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val v = inflater.inflate(R.layout.item_level, parent, false)
        return LevelViewHolder(v)
    }

    override fun onBindViewHolder(holder: LevelViewHolder, position: Int) {
        val level = levels[position]

        holder.itemView.lock_icon.visibility = View.GONE
        if (level.isBossLevel) {
            holder.itemView.level_number.text = ""
            holder.itemView.boss_icon.visibility = View.VISIBLE
            holder.itemView.boss_icon.setImageResource(level.getEnemyIcon(App.context))
        } else {
            holder.itemView.boss_icon.visibility = View.GONE
            holder.itemView.level_number.text = level.levelNumber.toString()
        }
        holder.itemView.level_number.onClick { mItemListener.onItemClicked(holder.adapterPosition) }

        if (level.isCompleted) {
            holder.itemView.level_number.isSelected = false
            holder.itemView.level_number.isEnabled = true
        } else if (position == 0 || levels[position - 1].isCompleted) {
            holder.itemView.level_number.isSelected = true
            holder.itemView.level_number.isEnabled = true
        } else {
            holder.itemView.level_number.isSelected = true
            holder.itemView.level_number.isEnabled = false
            holder.itemView.level_number.text = ""
            if (!level.isBossLevel) {
                holder.itemView.lock_icon.visibility = View.VISIBLE
            } else {
                holder.itemView.lock_icon.visibility = View.GONE
            }
        }
    }

    override fun getItemCount(): Int {
        return levels.size
    }
}
