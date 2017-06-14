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
import android.os.Parcelable
import android.support.v4.app.DialogFragment
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.dialog_end_battle.*
import net.fred.taskgame.hero.R
import net.fred.taskgame.hero.models.Card
import net.fred.taskgame.hero.models.Level
import net.fred.taskgame.hero.utils.UiUtils
import org.jetbrains.anko.sdk21.coroutines.onClick
import org.parceler.Parcels

class EndBattleDialogFragment : ImmersiveDialogFragment() {

    enum class EndType {
        PLAYER_WON, ENEMY_WON, DRAW
    }

    private var level: Level? = null
    private var wasAlreadyCompletedOnce: Boolean = false
    private var endType: EndType? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        level = Parcels.unwrap<Level>(arguments.getParcelable<Parcelable>(ARG_LEVEL))
        wasAlreadyCompletedOnce = arguments.getBoolean(ARG_WAS_ALREADY_COMPLETED_ONCE)
        endType = arguments.getSerializable(ARG_END_TYPE) as EndType

        setStyle(DialogFragment.STYLE_NO_FRAME, R.style.AppTheme_Dialog)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_end_battle, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        when (endType) {
            EndBattleDialogFragment.EndType.PLAYER_WON -> {
                title.text = "VICTORY"

                var contentText = "Who is the boss now?\n\n"
                if (!wasAlreadyCompletedOnce) {
                    level?.let { lvl ->
                        val previousSlots = Level.getCorrespondingDeckSlots(lvl.levelNumber - 1)
                        val newSlots = Level.getCorrespondingDeckSlots(lvl.levelNumber)

                        val previousAvailableCreatures = Card.getNonObtainedCardList(previousSlots).size
                        val newAvailableCreatures = Card.getNonObtainedCardList(newSlots).size

                        if (newAvailableCreatures > previousAvailableCreatures) {
                            contentText += " ● You can now summon more creatures!\n"
                        }

                        if (newSlots > previousSlots) {
                            contentText += " ● You have earned new deck slots!\n"
                        }
                    }
                }

                content.text = contentText
            }
            EndBattleDialogFragment.EndType.ENEMY_WON -> {
                title.text = "DEFEAT"
                content.text = "Well, that was close, right?"
                crown.visibility = View.GONE
            }
            EndBattleDialogFragment.EndType.DRAW -> {
                title.text = "DRAW"
                content.text = "Well, that was close, right?"
                crown.visibility = View.GONE
            }
        }

        ok.onClick { dialog.cancel() }
    }

    override fun onDetach() {
        // We are removing this fragment, let's also remove the battle one which were used as background
        fragmentManager.popBackStack()

        level?.let { lvl ->
            if (endType == EndType.PLAYER_WON && !TextUtils.isEmpty(lvl.getEndStory(context))) {
                val transaction = fragmentManager.beginTransaction()
                UiUtils.animateTransition(transaction, UiUtils.TransitionType.TRANSITION_FADE_IN)
                transaction.replace(R.id.fragment_container, StoryFragment.newInstance(lvl, true), StoryFragment::class.java.name).addToBackStack(null).commitAllowingStateLoss()
            }
        }

        super.onDetach()
    }

    companion object {

        val ARG_LEVEL = "ARG_LEVEL"
        private val ARG_WAS_ALREADY_COMPLETED_ONCE = "ARG_WAS_ALREADY_COMPLETED_ONCE"
        private val ARG_END_TYPE = "ARG_END_TYPE"

        internal fun newInstance(level: Level, wasAlreadyCompletedOnce: Boolean, endType: EndType): EndBattleDialogFragment {
            val f = EndBattleDialogFragment()

            val args = Bundle()
            args.putParcelable(ARG_LEVEL, Parcels.wrap(level))
            args.putBoolean(ARG_WAS_ALREADY_COMPLETED_ONCE, wasAlreadyCompletedOnce)
            args.putSerializable(ARG_END_TYPE, endType)
            f.arguments = args

            return f
        }
    }
}