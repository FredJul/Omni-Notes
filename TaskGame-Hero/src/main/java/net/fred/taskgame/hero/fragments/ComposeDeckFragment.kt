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

package net.fred.taskgame.hero.fragments

import android.os.Bundle
import android.support.annotation.RawRes
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_compose_deck.*
import net.fred.taskgame.hero.R
import net.fred.taskgame.hero.activities.MainActivity
import net.fred.taskgame.hero.adapters.ComposeDeckAdapter
import net.fred.taskgame.hero.adapters.RecyclerViewItemListener
import net.fred.taskgame.hero.models.Card
import net.fred.taskgame.hero.models.Level
import net.frju.androidquery.gen.Q
import org.jetbrains.anko.onClick


class ComposeDeckFragment : BaseFragment() {

    private var obtainedCardList: List<Card>? = null
    private var adapter: ComposeDeckAdapter? = null

    private val usedSlots: Int
        get() {
            return obtainedCardList!!
                    .filter(Card::isInDeck)
                    .sumBy(Card::neededSlots)
        }

    override val mainMusicResId: Int
        @RawRes
        get() = R.raw.main_theme

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_compose_deck, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        recycler_view.layoutManager = layoutManager

        obtainedCardList = Card.obtainedCardList
        adapter = ComposeDeckAdapter(obtainedCardList!!, object : RecyclerViewItemListener {
            override fun onItemClicked(position: Int) {
                val card = obtainedCardList!![position]
                if (card.isInDeck || !card.isInDeck && usedSlots + card.neededSlots <= Level.correspondingDeckSlots) {
                    mainActivity?.playSound(MainActivity.SOUND_CHANGE_CARD)
                    card.isInDeck = !card.isInDeck
                    Q.Card.save(card).query()
                    adapter!!.notifyItemChanged(position)

                    updateUI()
                } else {
                    mainActivity?.playSound(MainActivity.SOUND_IMPOSSIBLE_ACTION)
                }
            }
        })
        recycler_view.adapter = adapter

        updateUI()

        back.onClick { fragmentManager.popBackStack() }
    }

    private fun updateUI() {
        val freeSlots = Level.correspondingDeckSlots
        val usedSlots = usedSlots
        deck_slots.text = "Free slots: " + (freeSlots - usedSlots) + "/" + freeSlots
    }

}
