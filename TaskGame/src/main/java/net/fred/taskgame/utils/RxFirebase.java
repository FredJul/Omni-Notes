package net.fred.taskgame.utils;

import android.support.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.functions.Cancellable;
import io.reactivex.functions.Predicate;


public class RxFirebase {

    public enum EventType {
        CHILD_ADDED, CHILD_CHANGED, CHILD_REMOVED, CHILD_MOVED
    }

    /**
     * Essentially a struct so that we can pass all children events through as a single object.
     */
    public static class FirebaseChildEvent {
        public DataSnapshot snapshot;
        public EventType eventType;
        public String prevName;

        FirebaseChildEvent(DataSnapshot snapshot, EventType eventType, String prevName) {
            this.snapshot = snapshot;
            this.eventType = eventType;
            this.prevName = prevName;
        }
    }

    public static Observable<FirebaseChildEvent> observeChildren(final Query ref) {
        return Observable.create(new ObservableOnSubscribe<FirebaseChildEvent>() {

            @Override
            public void subscribe(final ObservableEmitter<FirebaseChildEvent> emitter) throws Exception {
                final ChildEventListener listener = ref.addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(DataSnapshot dataSnapshot, String prevName) {
                        emitter.onNext(new FirebaseChildEvent(dataSnapshot, EventType.CHILD_ADDED, prevName));
                    }

                    @Override
                    public void onChildChanged(DataSnapshot dataSnapshot, String prevName) {
                        emitter.onNext(new FirebaseChildEvent(dataSnapshot, EventType.CHILD_CHANGED, prevName));
                    }

                    @Override
                    public void onChildRemoved(DataSnapshot dataSnapshot) {
                        emitter.onNext(new FirebaseChildEvent(dataSnapshot, EventType.CHILD_REMOVED, null));
                    }

                    @Override
                    public void onChildMoved(DataSnapshot dataSnapshot, String prevName) {
                        emitter.onNext(new FirebaseChildEvent(dataSnapshot, EventType.CHILD_MOVED, prevName));
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        // Notify Subscriber
                        emitter.onError(error.toException());
                    }
                });

                // When the subscription is cancelled, remove the listener
                emitter.setCancellable(new Cancellable() {
                    @Override
                    public void cancel() throws Exception {
                        ref.removeEventListener(listener);
                    }
                });
            }
        });
    }

    private static Predicate<FirebaseChildEvent> makeEventFilter(final EventType eventType) {
        return new Predicate<FirebaseChildEvent>() {
            @Override
            public boolean test(FirebaseChildEvent firebaseChildEvent) throws Exception {
                return firebaseChildEvent.eventType == eventType;
            }
        };
    }

    public static Observable<FirebaseChildEvent> observeChildAdded(Query ref) {
        return observeChildren(ref).filter(makeEventFilter(EventType.CHILD_ADDED));
    }

    public static Observable<FirebaseChildEvent> observeChildChanged(Query ref) {
        return observeChildren(ref).filter(makeEventFilter(EventType.CHILD_CHANGED));
    }

    public static Observable<FirebaseChildEvent> observeChildMoved(Query ref) {
        return observeChildren(ref).filter(makeEventFilter(EventType.CHILD_MOVED));
    }

    public static Observable<FirebaseChildEvent> observeChildRemoved(Query ref) {
        return observeChildren(ref).filter(makeEventFilter(EventType.CHILD_REMOVED));
    }

    public static Observable<DataSnapshot> observe(final Query ref) {

        return Observable.create(new ObservableOnSubscribe<DataSnapshot>() {

            @Override
            public void subscribe(final ObservableEmitter<DataSnapshot> emitter) throws Exception {
                final ValueEventListener listener = ref.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        emitter.onNext(dataSnapshot);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        // Notify Subscriber
                        emitter.onError(error.toException());
                    }
                });

                // When the subscription is cancelled, remove the listener
                emitter.setCancellable(new Cancellable() {
                    @Override
                    public void cancel() throws Exception {
                        ref.removeEventListener(listener);
                    }
                });
            }
        });
    }

    /**
     * @param ref
     * @return
     */
    public static Observable<DataSnapshot> observeSingle(final Query ref) {

        return Observable.create(new ObservableOnSubscribe<DataSnapshot>() {

            @Override
            public void subscribe(final ObservableEmitter<DataSnapshot> emitter) throws Exception {
                ref.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        emitter.onNext(dataSnapshot);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        // Notify Subscriber
                        emitter.onError(error.toException());
                    }
                });
            }
        });
    }


    /**
     * @param dbRef
     * @param object
     * @return
     */
    public static Observable<Task<Void>> observePush(final DatabaseReference dbRef, final Object object) {
        return Observable.create(new ObservableOnSubscribe<Task<Void>>() {
            @Override
            public void subscribe(final ObservableEmitter<Task<Void>> emitter) throws Exception {
                dbRef.push().setValue(object)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                emitter.onNext(task);
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                emitter.onError(e);
                            }
                        });
            }
        });
    }

    /**
     * @param dbRef
     * @param object
     * @return
     */
    public static Observable<Task<Void>> observeUpdate(final DatabaseReference dbRef, final Object object) {
        return Observable.create(new ObservableOnSubscribe<Task<Void>>() {
            @Override
            public void subscribe(final ObservableEmitter<Task<Void>> emitter) throws Exception {
                dbRef.setValue(object)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                emitter.onNext(task);
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                emitter.onError(e);
                            }
                        });
            }
        });
    }
}