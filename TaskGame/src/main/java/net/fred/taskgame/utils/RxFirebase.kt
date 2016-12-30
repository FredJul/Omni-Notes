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

package net.fred.taskgame.utils

import com.google.android.gms.tasks.Task
import com.google.firebase.database.*
import io.reactivex.Observable
import io.reactivex.functions.Predicate


object RxFirebase {

    enum class EventType {
        CHILD_ADDED, CHILD_CHANGED, CHILD_REMOVED, CHILD_MOVED
    }

    /**
     * Essentially a struct so that we can pass all children events through as a single object.
     */
    class FirebaseChildEvent internal constructor(var snapshot: DataSnapshot, var eventType: EventType, var prevName: String?)

    fun observeChildren(ref: Query): Observable<FirebaseChildEvent> {
        return Observable.create { emitter ->
            val listener = ref.addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(dataSnapshot: DataSnapshot, prevName: String?) {
                    emitter.onNext(FirebaseChildEvent(dataSnapshot, EventType.CHILD_ADDED, prevName))
                }

                override fun onChildChanged(dataSnapshot: DataSnapshot, prevName: String?) {
                    emitter.onNext(FirebaseChildEvent(dataSnapshot, EventType.CHILD_CHANGED, prevName))
                }

                override fun onChildRemoved(dataSnapshot: DataSnapshot) {
                    emitter.onNext(FirebaseChildEvent(dataSnapshot, EventType.CHILD_REMOVED, null))
                }

                override fun onChildMoved(dataSnapshot: DataSnapshot, prevName: String?) {
                    emitter.onNext(FirebaseChildEvent(dataSnapshot, EventType.CHILD_MOVED, prevName))
                }

                override fun onCancelled(error: DatabaseError) {
                    // Notify Subscriber
                    emitter.onError(error.toException())
                }
            })

            // When the subscription is cancelled, remove the listener
            emitter.setCancellable { ref.removeEventListener(listener) }
        }
    }

    private fun makeEventFilter(eventType: EventType): Predicate<FirebaseChildEvent> {
        return Predicate { firebaseChildEvent -> firebaseChildEvent.eventType == eventType }
    }

    fun observeChildAdded(ref: Query): Observable<FirebaseChildEvent> {
        return observeChildren(ref).filter(makeEventFilter(EventType.CHILD_ADDED))
    }

    fun observeChildChanged(ref: Query): Observable<FirebaseChildEvent> {
        return observeChildren(ref).filter(makeEventFilter(EventType.CHILD_CHANGED))
    }

    fun observeChildMoved(ref: Query): Observable<FirebaseChildEvent> {
        return observeChildren(ref).filter(makeEventFilter(EventType.CHILD_MOVED))
    }

    fun observeChildRemoved(ref: Query): Observable<FirebaseChildEvent> {
        return observeChildren(ref).filter(makeEventFilter(EventType.CHILD_REMOVED))
    }

    fun observe(ref: Query): Observable<DataSnapshot> {

        return Observable.create { emitter ->
            val listener = ref.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    emitter.onNext(dataSnapshot)
                }

                override fun onCancelled(error: DatabaseError) {
                    // Notify Subscriber
                    emitter.onError(error.toException())
                }
            })

            // When the subscription is cancelled, remove the listener
            emitter.setCancellable { ref.removeEventListener(listener) }
        }
    }

    /**
     * @param ref
     * *
     * @return
     */
    fun observeSingle(ref: Query): Observable<DataSnapshot> {

        return Observable.create { emitter ->
            ref.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    emitter.onNext(dataSnapshot)
                }

                override fun onCancelled(error: DatabaseError) {
                    // Notify Subscriber
                    emitter.onError(error.toException())
                }
            })
        }
    }


    /**
     * @param dbRef
     * *
     * @param object
     * *
     * @return
     */
    fun observePush(dbRef: DatabaseReference, `object`: Any): Observable<Task<Void>> {
        return Observable.create { emitter ->
            dbRef.push().setValue(`object`)
                    .addOnCompleteListener { task -> emitter.onNext(task) }
                    .addOnFailureListener { e -> emitter.onError(e) }
        }
    }

    /**
     * @param dbRef
     * *
     * @param object
     * *
     * @return
     */
    fun observeUpdate(dbRef: DatabaseReference, `object`: Any): Observable<Task<Void>> {
        return Observable.create { emitter ->
            dbRef.setValue(`object`)
                    .addOnCompleteListener { task -> emitter.onNext(task) }
                    .addOnFailureListener { e -> emitter.onError(e) }
        }
    }
}