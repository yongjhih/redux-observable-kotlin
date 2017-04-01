package redux.observable

import com.jakewharton.rxrelay2.BehaviorRelay
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.schedulers.Schedulers
import redux.api.Dispatcher
import redux.api.Store
import redux.api.enhancer.Middleware
import java.util.concurrent.atomic.AtomicBoolean

/*
 * Copyright (C) 2016 Michael Pardo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

interface EpicMiddleware<S : Any> : Middleware<S> {
    fun replaceEpic(epic: Epic<S>)
}

fun <S : Any> createEpicMiddleware(epic: Epic<S>): EpicMiddleware<S> {
    return object : EpicMiddleware<S> {
        private val actions = PublishRelay.create<Any>()
        private val epics = BehaviorRelay.createDefault(epic)
        private val subscribed = AtomicBoolean(false)

        override fun dispatch(store: Store<S>, next: Dispatcher, action: Any): Any {
            if (subscribed.compareAndSet(false, true)) {
                epics.switchMap { it.map(actions.subscribeOn(Schedulers.trampoline()), store) }
                    .subscribe { store.dispatch(it) }
            }
            val result = next.dispatch(action)
            actions.accept(action)
            return result
        }

        override fun replaceEpic(epic: Epic<S>) {
            epics.accept(epic)
        }
    }
}
