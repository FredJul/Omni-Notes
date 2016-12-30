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

package net.fred.taskgame.hero.logic

import net.fred.taskgame.hero.models.Card
import net.fred.taskgame.hero.utils.Dog
import org.parceler.Parcel
import java.util.*

@Parcel(Parcel.Serialization.BEAN)
class BattleManager {

    enum class BattleStep {
        APPLY_PLAYER_SUPPORT, SELECT_STRATEGY, APPLY_ENEMY_SUPPORT, FIGHT, APPLY_DAMAGES, PLAYER_DEATH, ENEMY_DEATH, END_TURN, PLAYER_WON, ENEMY_WON, DRAW
    }

    enum class BattleStrategy {
        ATTACK, DEFENSE, ALEATORY
    }

    enum class AleatoryAffectedField {
        ATTACK, DEFENSE
    }

    inner class AleatoryResult {
        var affectedField: AleatoryAffectedField? = null
        var bonusOrPenalty: Int = 0
    }

    internal var remainingEnemyCards: MutableList<Card> = ArrayList()
    internal var usedEnemyCards: MutableList<Card> = ArrayList()
    internal var remainingPlayerCards: MutableList<Card> = ArrayList()
    internal var usedPlayerCards: MutableList<Card> = ArrayList()
    var currentStep = BattleStep.APPLY_PLAYER_SUPPORT
        internal set
    internal var nextSteps: MutableList<BattleStep> = ArrayList()

    internal var currentStrategy = BattleStrategy.ATTACK

    internal var stunPlayer: Boolean = false
    internal var stunEnemy: Boolean = false

    val remainingPlayerCharacterCards: List<Card>
        get() {
            return remainingPlayerCards.filterTo(ArrayList<Card>()) { it.type == Card.Type.CREATURE }
        }

    val remainingPlayerSupportCards: List<Card>
        get() {
            return remainingPlayerCards.filterTo(ArrayList<Card>()) { it.type == Card.Type.SUPPORT }
        }

    fun addEnemyCards(cards: List<Card>) {
        for (card in cards) {
            remainingEnemyCards.add(card.clone()) // We need to clone them to not modify the original data
        }
    }

    fun addPlayerCards(cards: List<Card>) {
        for (card in cards) {
            remainingPlayerCards.add(card.clone()) // We need to clone them to not modify the original data
        }
    }

    val nextPlayerCreatureCard: Card?
        get() {
            return remainingPlayerCards.firstOrNull { it.type == Card.Type.CREATURE }
        }

    val nextEnemyCreatureCard: Card?
        get() {
            return remainingEnemyCards.firstOrNull { it.type == Card.Type.CREATURE }
        }

    val lastUsedEnemySupportCard: Card?
        get() {
            return usedEnemyCards.indices.reversed()
                    .map { usedEnemyCards[it] }
                    .firstOrNull { it.type == Card.Type.SUPPORT }
        }

    val lastUsedEnemyCreatureCard: Card?
        get() {
            return usedEnemyCards.indices.reversed()
                    .map { usedEnemyCards[it] }
                    .firstOrNull { it.type == Card.Type.CREATURE }
        }

    val lastUsedPlayerCreatureCard: Card?
        get() {
            return usedPlayerCards.indices.reversed()
                    .map { usedPlayerCards[it] }
                    .firstOrNull { it.type == Card.Type.CREATURE }
        }

    val currentOrNextAliveEnemyCreatureCard: Card?
        get() {
            if (isEnemyCreatureStillAlive) {
                return lastUsedEnemyCreatureCard
            } else {
                return nextEnemyCreatureCard
            }
        }

    val isPlayerCreatureStillAlive: Boolean
        get() {
            val creatureCard = lastUsedPlayerCreatureCard
            return creatureCard != null && creatureCard.defense > 0
        }

    val isEnemyCreatureStillAlive: Boolean
        get() {
            val creatureCard = lastUsedEnemyCreatureCard
            return creatureCard != null && creatureCard.defense > 0
        }

    fun getLastUsedEnemyCreatureCard(fromEnemyPointOfView: Boolean): Card? {
        if (fromEnemyPointOfView) {
            return lastUsedPlayerCreatureCard
        } else {
            return lastUsedEnemyCreatureCard
        }
    }

    fun getLastUsedPlayerCreatureCard(fromEnemyPointOfView: Boolean): Card? {
        if (fromEnemyPointOfView) {
            return lastUsedEnemyCreatureCard
        } else {
            return lastUsedPlayerCreatureCard
        }
    }

    fun play() {
        nextSteps.add(BattleStep.SELECT_STRATEGY)
        if (!isEnemyCreatureStillAlive) {
            val enemy = nextEnemyCreatureCard
            usedEnemyCards.add(enemy!!)
            remainingEnemyCards.remove(enemy)
        } else {
            if (remainingEnemyCards.size > 0 && remainingEnemyCards[0].type == Card.Type.SUPPORT) {
                usedEnemyCards.add(remainingEnemyCards[0])
                remainingEnemyCards.removeAt(0)
                nextSteps.add(BattleStep.APPLY_ENEMY_SUPPORT)
            }
        }

        nextSteps.add(BattleStep.FIGHT)
        nextSteps.add(BattleStep.APPLY_DAMAGES)
    }

    fun play(card: Card) {
        remainingPlayerCards.remove(card)
        usedPlayerCards.add(card)

        if (card.type == Card.Type.SUPPORT) {
            nextSteps.add(BattleStep.APPLY_PLAYER_SUPPORT)
        }

        play()
    }

    fun applyNewStrategy(currentStrategy: BattleStrategy): AleatoryResult? {
        this.currentStrategy = currentStrategy
        if (currentStrategy == BattleStrategy.ALEATORY) {
            val result = AleatoryResult()

            // 1 chance on top of 2
            result.affectedField = if (Math.random() > 0.555) AleatoryAffectedField.ATTACK else AleatoryAffectedField.DEFENSE

            val player = lastUsedPlayerCreatureCard
            if (result.affectedField == AleatoryAffectedField.ATTACK) {
                result.bonusOrPenalty = getRandomIntBetween(1, Math.round(`@+id/player_image`!!.attack / 2.0f)) // up to 50% bonus or penalty
                if (Math.random() > 0.555) { // 1 chance on 2 to get a negative value
                    result.bonusOrPenalty *= -1
                }
                `@+id/player_image`.attack += result.bonusOrPenalty
                if (`@+id/player_image`.attack <= 0) {
                    `@+id/player_image`.attack = 1
                }
            } else {
                result.bonusOrPenalty = getRandomIntBetween(1, Math.round(`@+id/player_image`!!.defense / 2.0f)) // up to 50% bonus or penalty
                if (Math.random() > 0.555) { // 1 chance on 2 to get a negative value
                    result.bonusOrPenalty *= -1
                }
                `@+id/player_image`.defense += result.bonusOrPenalty
                if (`@+id/player_image`.defense <= 0) {
                    `@+id/player_image`.defense = 1
                }
            }

            return result
        }

        return null
    }

    fun executeNextStep(): BattleStep {
        currentStep = nextSteps.removeAt(0)
        when (currentStep) {
            BattleManager.BattleStep.APPLY_PLAYER_SUPPORT -> {
                if (!stunPlayer) {
                    Card.allCardsMap[usedPlayerCards[usedPlayerCards.size - 1].id]?.supportAction?.executeSupportAction(this, false)
                }
            }
            BattleManager.BattleStep.APPLY_ENEMY_SUPPORT -> {
                if (!stunEnemy) {
                    Card.allCardsMap[usedEnemyCards[usedEnemyCards.size - 1].id]?.supportAction?.executeSupportAction(this, true)
                }
            }
            BattleManager.BattleStep.APPLY_DAMAGES -> {
                val enemy = lastUsedEnemyCreatureCard
                val player = lastUsedPlayerCreatureCard

                // Change the defense points
                if (!stunPlayer && currentStrategy != BattleStrategy.DEFENSE) {
                    Card.allCardsMap[enemy!!.id]?.fightAction?.applyDamageFromOpponent(enemy, `@+id/player_image`!!)
                    enemy.defense = if (enemy.defense < 0) 0 else enemy.defense
                }
                if (!stunEnemy) {
                    if (currentStrategy == BattleStrategy.DEFENSE) {
                        // We reduce damage from 0 to 100% depending of luck
                        val previousDefense = `@+id/player_image`!!.defense
                        Card.allCardsMap[`@+id/player_image`.id]?.fightAction?.applyDamageFromOpponent(`@+id/player_image`, enemy!!)
                        val realDamage = Math.round((previousDefense - `@+id/player_image`.defense) * Math.random()).toInt()
                        `@+id/player_image`.defense = previousDefense - realDamage
                    } else {
                        Card.allCardsMap[`@+id/player_image`!!.id]?.fightAction?.applyDamageFromOpponent(`@+id/player_image`, enemy!!)
                    }

                    `@+id/player_image`.defense = if (`@+id/player_image`.defense < 0) 0 else `@+id/player_image`.defense
                }
                stunPlayer = false
                stunEnemy = false

                // Death computation
                if (enemy!!.defense <= 0) {
                    nextSteps.add(BattleStep.ENEMY_DEATH)
                }
                if (`@+id/player_image`!!.defense <= 0) {
                    nextSteps.add(BattleStep.PLAYER_DEATH)
                }

                // End of battle
                val enemyRemainingLife = usedEnemyCards
                        .filter { it.type == Card.Type.CREATURE }
                        .sumBy(Card::defense) +
                        remainingEnemyCards
                                .filter { it.type == Card.Type.CREATURE }
                                .sumBy(Card::defense)

                val playerRemainingLife = usedPlayerCards
                        .filter { it.type == Card.Type.CREATURE }
                        .sumBy(Card::defense) +
                        remainingPlayerCards
                                .filter { it.type == Card.Type.CREATURE }
                                .sumBy(Card::defense)

                if (enemyRemainingLife <= 0 && playerRemainingLife <= 0) {
                    nextSteps.add(BattleStep.DRAW)
                } else if (enemyRemainingLife <= 0 && playerRemainingLife > 0) {
                    nextSteps.add(BattleStep.PLAYER_WON)
                } else if (enemyRemainingLife > 0 && playerRemainingLife <= 0) {
                    nextSteps.add(BattleStep.ENEMY_WON)
                } else {
                    nextSteps.add(BattleStep.END_TURN)
                }
            }
            else -> {
            }
        }

        Dog.i(currentStep.name)
        return currentStep
    }

    fun stunEnemy(fromEnemyPointOfView: Boolean) {
        if (fromEnemyPointOfView) {
            stunPlayer = true
        } else {
            stunEnemy = true
        }
    }

    private fun getRandomIntBetween(lower: Int, higher: Int): Int {
        if (higher <= lower) {
            return lower
        }
        return (Math.random() * (higher + 1 - lower)).toInt() + lower
    }
}
