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

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.support.annotation.RawRes
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_buy_cards.*
import net.fred.taskgame.hero.R
import net.fred.taskgame.hero.activities.MainActivity
import net.fred.taskgame.hero.adapters.BuyCardsAdapter
import net.fred.taskgame.hero.adapters.RecyclerViewItemListener
import net.fred.taskgame.hero.models.Card
import net.fred.taskgame.hero.utils.TaskGameUtils
import net.frju.androidquery.gen.CARD
import org.jetbrains.anko.onClick


class BuyCardsFragment : BaseFragment() {

    private var nonObtainedCardList: MutableList<Card>? = null
    private var adapter: BuyCardsAdapter? = null

    override val mainMusicResId: Int
        @RawRes
        get() = R.raw.main_theme

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_buy_cards, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        recycler_view.layoutManager = layoutManager
        recycler_view.setEmptyView(empty_view)

        nonObtainedCardList = Card.nonObtainedCardList
        nonObtainedCardList?.let {
            adapter = BuyCardsAdapter(it, object : RecyclerViewItemListener {
                override fun onItemClicked(position: Int) {
                    val card = it[position]
                    if (card.price == 0) {
                        buyCard(position, card)
                    } else {
                        try {
                            startActivityForResult(TaskGameUtils.getRequestPointsActivityIntent(context, card.price.toLong()), card.id)
                        } catch (e: ActivityNotFoundException) {
                            // TaskGame application is not installed
                        }

                    }
                }
            })
        }
        recycler_view.adapter = adapter

        back.onClick { fragmentManager.popBackStack() }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            // the user accepted, let's refresh the DB and UI

            nonObtainedCardList?.let {
                for (i in it.indices) {
                    val card = it[i]
                    if (card.id == requestCode) { // requestCode is card id
                        buyCard(i, card)
                        break
                    }
                }
            }
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun buyCard(position: Int, card: Card) {
        mainActivity?.playSound(MainActivity.SOUND_NEW_CARD)
        card.isObtained = true
        CARD.save(card).query()
        nonObtainedCardList?.removeAt(position)
        adapter?.notifyItemRemoved(position)
    }
}
