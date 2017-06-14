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
import android.support.v7.widget.GridLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_level_selection.*
import net.fred.taskgame.hero.R
import net.fred.taskgame.hero.adapters.LevelSelectionAdapter
import net.fred.taskgame.hero.adapters.RecyclerViewItemListener
import net.fred.taskgame.hero.models.Level
import net.fred.taskgame.hero.utils.UiUtils
import org.jetbrains.anko.sdk21.coroutines.onClick


class LevelSelectionFragment : BaseFragment() {

    override val mainMusicResId: Int
        @RawRes
        get() = R.raw.main_theme

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_level_selection, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val layoutManager = GridLayoutManager(context, 4)
        recycler_view.layoutManager = layoutManager


        val adapter = LevelSelectionAdapter(Level.allLevelsList, object : RecyclerViewItemListener {
            override fun onItemClicked(position: Int) {
                mainActivity?.startBattle(Level.allLevelsList[position])
            }
        })
        recycler_view.adapter = adapter

        // Scroll to the first level to do
        val firstNotCompletedLevel = Level.allLevelsList
                .takeWhile { it.isCompleted }
                .count()
        layoutManager.scrollToPositionWithOffset(firstNotCompletedLevel, UiUtils.dpToPixel(50))

        buy_cards.onClick { mainActivity?.buyCards() }
        compose_deck.onClick { mainActivity?.composeDeck() }
    }
}
