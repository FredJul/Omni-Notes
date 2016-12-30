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

package net.fred.taskgame.hero.models

import android.content.Context
import android.support.annotation.DrawableRes
import android.support.annotation.RawRes
import android.util.Pair
import android.util.SparseBooleanArray
import net.fred.taskgame.hero.R
import net.frju.androidquery.annotation.DbField
import net.frju.androidquery.annotation.DbModel
import net.frju.androidquery.gen.Q
import org.parceler.Parcel
import java.util.*

@Parcel(Parcel.Serialization.BEAN)
@DbModel(databaseProvider = LocalDatabaseProvider::class)
class Level {

    @DbField(primaryKey = true)
    var levelNumber: Int = 0

    @DbField
    var isCompleted: Boolean = false

    var isBossLevel: Boolean = false

    var enemyCards: MutableList<Card> = ArrayList()

    @RawRes
    var battleMusicResId = INVALID_ID

    @RawRes
    var startStoryMusicResId = INVALID_ID
    @RawRes
    var endStoryMusicResId = INVALID_ID

    @DrawableRes
    fun getEnemyIcon(context: Context): Int {
        return STORY_CHARS_INFO_MAP[context.resources.getStringArray(R.array.level_stories)[levelNumber * 3 - 3]]!!.second
    }

    fun getStartStory(context: Context): String {
        return context.resources.getStringArray(R.array.level_stories)[levelNumber * 3 - 2]
    }

    fun getEndStory(context: Context): String {
        return context.resources.getStringArray(R.array.level_stories)[levelNumber * 3 - 1]
    }

    private fun addEnemyCard(cardId: Int): Level {
        enemyCards.add(Card.allCardsMap[cardId]!!)
        return this
    }

    companion object {

        val INVALID_ID = 0
        val STORY_CHARS_INFO_MAP: Map<String, Pair<Int, Int>> = object : HashMap<String, Pair<Int, Int>>() {
            init {
                put("hero", Pair(R.string.hero_name, R.drawable.invoker_female))
                put("school_friend", Pair(R.string.school_friend_name, R.drawable.invoker_male))
                put("school_master", Pair(R.string.school_master_name, R.drawable.high_invoker_male))
                put("officer", Pair(R.string.officer_name, R.drawable.officer))
                put("counselor", Pair(R.string.counselor_name, R.drawable.counselor))
                put("king", Pair(R.string.king_name, R.drawable.king))
                put("priest", Pair(R.string.priest_name, R.drawable.priest))
            }
        }
        private val ALL_LEVELS_LIST = ArrayList<Level>()

        val allLevelsList: List<Level>
            get() = ALL_LEVELS_LIST

        val correspondingDeckSlots: Int
            get() {
                val lastCompletedLevel = ALL_LEVELS_LIST
                        .takeWhile { it.isCompleted }
                        .count()

                return getCorrespondingDeckSlots(lastCompletedLevel)
            }


        fun getCorrespondingDeckSlots(lastCompletedLevelNumber: Int): Int {
            // Increase quickly, but then slow down
            // See graph on: http://fooplot.com/plot/lpum8k6yac
            return Math.pow(Math.log10(Math.pow((lastCompletedLevelNumber + 3).toDouble(), 3.0)), 2.0).toInt()
        }

        fun populate() {
            ALL_LEVELS_LIST.clear()

            val completedList = SparseBooleanArray()
            for (level in Q.Level.select().query().toArray()) {
                completedList.append(level.levelNumber, level.isCompleted)
            }

            var levelNumber = 1

            /**************** Level 1 to 10 *****************
             * Available slots == levelNumber + 1
             */
            var level = generateLevel(levelNumber++, completedList)
            level.addEnemyCard(Card.CREATURE_SYLPH)

            level = generateLevel(levelNumber++, completedList)
            level.addEnemyCard(Card.CREATURE_SKELETON).addEnemyCard(Card.CREATURE_SYLPH)

            level = generateLevel(levelNumber++, completedList)
            level.addEnemyCard(Card.CREATURE_SYLPH).addEnemyCard(Card.CREATURE_MERMAN).addEnemyCard(Card.CREATURE_SKELETON)

            level = generateLevel(levelNumber++, completedList)
            level.isBossLevel = true
            level.battleMusicResId = R.raw.boss_theme
            level.addEnemyCard(Card.CREATURE_SKELETON).addEnemyCard(Card.SUPPORT_POWER_POTION).addEnemyCard(Card.SUPPORT_MEDICAL_ATTENTION)

            level = generateLevel(levelNumber++, completedList)
            level.addEnemyCard(Card.CREATURE_SKELETON).addEnemyCard(Card.CREATURE_TROLL).addEnemyCard(Card.CREATURE_SYLPH_2)

            level = generateLevel(levelNumber++, completedList)
            level.addEnemyCard(Card.CREATURE_SKELETON).addEnemyCard(Card.CREATURE_LICH)

            level = generateLevel(levelNumber++, completedList)
            level.addEnemyCard(Card.CREATURE_EMPTY_ARMOR).addEnemyCard(Card.SUPPORT_MEDICAL_ATTENTION).addEnemyCard(Card.SUPPORT_MEDICAL_ATTENTION)

            level = generateLevel(levelNumber++, completedList)
            level.isBossLevel = true
            level.battleMusicResId = R.raw.boss_theme
            level.startStoryMusicResId = R.raw.story_suspens
            level.endStoryMusicResId = R.raw.story_suspens
            level.addEnemyCard(Card.CREATURE_EMPTY_ARMOR).addEnemyCard(Card.CREATURE_GRUNT)

            level = generateLevel(levelNumber++, completedList)
            level.startStoryMusicResId = R.raw.story_suspens
            level.endStoryMusicResId = R.raw.story_suspens
            level.addEnemyCard(Card.CREATURE_EMPTY_ARMOR).addEnemyCard(Card.CREATURE_EMPTY_ARMOR)
        }

        private fun generateLevel(levelNumber: Int, completedList: SparseBooleanArray): Level {
            val level = Level()
            level.levelNumber = levelNumber
            level.isCompleted = completedList.get(level.levelNumber)
            ALL_LEVELS_LIST.add(level)
            return level
        }
    }

}
