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
import kotlinx.android.synthetic.main.item_buy_card.view.*
import net.fred.taskgame.hero.R
import net.fred.taskgame.hero.models.Card
import org.jetbrains.anko.sdk21.coroutines.onClick

class BuyCardsAdapter(val cards: List<Card>, private val mItemListener: RecyclerViewItemListener) : RecyclerView.Adapter<BuyCardsAdapter.CardViewHolder>() {

    class CardViewHolder(v: View) : RecyclerView.ViewHolder(v)

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        return cards[position].id.toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return 0
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val v = inflater.inflate(R.layout.item_buy_card, parent, false)
        return CardViewHolder(v)
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        val card = cards[position]

        holder.itemView.card.card = card
        holder.itemView.price.text = if (card.price == 0) "Free" else card.price.toString()

        holder.itemView.buy.onClick { mItemListener.onItemClicked(holder.adapterPosition) }
    }

    override fun getItemCount(): Int {
        return cards.size
    }
}
