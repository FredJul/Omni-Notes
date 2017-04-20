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

import android.graphics.Color
import android.os.Bundle
import android.os.Parcelable
import android.support.annotation.RawRes
import android.support.v4.content.ContextCompat
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import kotlinx.android.synthetic.main.fragment_story.*
import net.fred.taskgame.hero.R
import net.fred.taskgame.hero.activities.MainActivity
import net.fred.taskgame.hero.models.Card
import net.fred.taskgame.hero.models.Level
import net.fred.taskgame.hero.utils.UiUtils
import org.jetbrains.anko.onClick
import org.parceler.Parcels
import java.util.*


class StoryFragment : BaseFragment() {

    private var level: Level? = null
    private var isInTextAnimation: Boolean = false
    private var isEndStory: Boolean = false
    private var sentences: ArrayList<String>? = null

    override val mainMusicResId: Int
        @RawRes
        get() {
            level?.let { lvl ->
                if (!isEndStory && lvl.startStoryMusicResId != Level.INVALID_ID) {
                    return lvl.startStoryMusicResId
                } else if (isEndStory && lvl.endStoryMusicResId != Level.INVALID_ID) {
                    return lvl.endStoryMusicResId
                }
            }

            return R.raw.story_normal
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val layout = inflater.inflate(R.layout.fragment_story, container, false)

        level = Parcels.unwrap<Level>(arguments.getParcelable<Parcelable>(ARG_LEVEL))
        isEndStory = arguments.getBoolean(ARG_IS_END_STORY)

        if (savedInstanceState != null) {
            sentences = savedInstanceState.getStringArrayList(STATE_SENTENCES)
        } else {
            level?.let { lvl ->
                if (!isEndStory) {
                    sentences = ArrayList(Arrays.asList(*lvl.getStartStory(context).split("\n".toRegex()).dropLastWhile(String::isEmpty).toTypedArray()))
                } else {
                    sentences = ArrayList(Arrays.asList(*lvl.getEndStory(context).split("\n".toRegex()).dropLastWhile(String::isEmpty).toTypedArray()))
                }
            }
        }

        return layout
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        updateUI()

        skip_button.onClick { endStory() }
        root_view.onClick {
            if (!isInTextAnimation) {
                if ((sentences?.size ?: 0) > 1) {
                    sentences?.removeAt(0)
                    updateUI()
                } else {
                    endStory()
                }
            }
        }
    }

    private fun updateUI() {
        sentences?.let {
            val sentence = it[0]
        val separatorIndex = sentence.indexOf(':')
        val charInfo = sentence.substring(0, separatorIndex)

        if ("story" == charInfo.trim { it <= ' ' }) {
            right_char.animate().alpha(0f)
            right_char_text.animate().alpha(0f)
            right_char_separator.animate().alpha(0f)
            left_char.animate().alpha(0f)
            left_char_text.animate().alpha(0f)

            displayTextCharPerChar(story_text, SpannableString(sentence.substring(separatorIndex + 1)), 50)
            story_text.alpha = 0f
            story_text.animate().alpha(1f)
        } else {
            story_text.animate().alpha(0f)

            val charId = charInfo.substring(0, charInfo.length - 2).trim { it <= ' ' }
            Level.STORY_CHARS_INFO_MAP[charId]?.let { char ->
                val charName = getString(char.first)
                val charResId = char.second
            val isLeft = "L" == charInfo.substring(charInfo.length - 1)

            val text = charName + ": " + sentence.substring(separatorIndex + 1)
            val spannedText = SpannableString(text)
            spannedText.setSpan(ForegroundColorSpan(ContextCompat.getColor(context, R.color.color_accent)), 0, charName.length + 1, Spanned.SPAN_INCLUSIVE_INCLUSIVE)

            if (isLeft) {
                right_char.animate().alpha(0f)
                right_char_text.animate().alpha(0f)
                right_char_separator.animate().alpha(0f)

                left_char.setImageResource(charResId)
                left_char.animate().alpha(1f)
                displayTextCharPerChar(left_char_text, spannedText, 20)
                left_char_text.alpha = 0f
                left_char_text.animate().alpha(1f)
            } else {
                left_char.animate().alpha(0f)
                left_char_text.animate().alpha(0f)

                right_char.setImageResource(charResId)
                right_char.animate().alpha(1f)
                right_char_separator.animate().alpha(1f)
                displayTextCharPerChar(right_char_text, spannedText, 20)
                right_char_text.alpha = 0f
                right_char_text.animate().alpha(1f)
            }
            }
        }
        }
    }

    private fun displayTextCharPerChar(textView: TextView, text: SpannableString, delay: Int) {
        isInTextAnimation = true

        textView.tag = 1
        val displayOneChar = object : Runnable {
            override fun run() {
                val at = textView.tag as Int
                val textViewString = SpannableString(text)
                textViewString.setSpan(ForegroundColorSpan(Color.TRANSPARENT), at, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                textView.text = textViewString

                if (at < text.length) {
                    textView.tag = at + 1
                    textView.postDelayed(this, delay.toLong())
                } else {
                    isInTextAnimation = false
                }
            }
        }
        textView.postDelayed(displayOneChar, (delay * 2).toLong())
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putStringArrayList(STATE_SENTENCES, sentences)
        super.onSaveInstanceState(outState)
    }

    private fun endStory() {
        fragmentManager.popBackStack()

        if (!isEndStory) {
            val transaction = fragmentManager.beginTransaction()
            UiUtils.animateTransition(transaction, UiUtils.TransitionType.TRANSITION_FADE_IN)
            mainActivity?.playSound(MainActivity.SOUND_ENTER_BATTLE)
            level?.let {
                transaction.replace(R.id.fragment_container, BattleFragment.newInstance(it, Card.deckCardList), BattleFragment::class.java.name).addToBackStack(null).commitAllowingStateLoss()
            }
        }
    }

    companion object {

        val ARG_LEVEL = "ARG_LEVEL"
        val ARG_IS_END_STORY = "ARG_IS_END_STORY"

        private val STATE_SENTENCES = "STATE_SENTENCES"

        fun newInstance(level: Level, isEndStory: Boolean): StoryFragment {
            val fragment = StoryFragment()
            val args = Bundle()
            args.putParcelable(ARG_LEVEL, Parcels.wrap(level))
            args.putBoolean(ARG_IS_END_STORY, isEndStory)
            fragment.arguments = args

            return fragment
        }
    }
}
