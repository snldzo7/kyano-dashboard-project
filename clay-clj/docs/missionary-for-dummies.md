# Missionary for dummies

# What is Missionary and what is this document

[Missionary](https://github.com/leonoel/missionary) is "a functional effect and streaming system for Clojure/Script". In other words, it's a Clojure library that allows you to express asynchronous/parallel processes in your programs. In many ways you would find it similar in function and features to another Clojure library called [Manifold](https://github.com/clj-commons/manifold), and to built-in [core.async](https://github.com/clojure/core.async).

Library's author, [Léo NOEL](https://github.com/leonoel), also mentions that it's similar to things from other programming languages, like ReactiveX (RxJava, Rx.NET et al), also like Scala's libraries Cats Effects, ZIO and Monix, and also like Haskell's IO monads. If you know either of those things well, you will likely understand all the Missionary's concepts rather quickly.

However, if you're a dummy like I am, don't have hardcore FP experience, and you really want to start using Missionary in your project, you might need some help! [Missionary's slack channel](https://app.slack.com/client/T03RZGPFR/CL85MBPEF) has lots of tidbits of valuable information, but I haven't really found where I could read everything in one go, from simple to complex, drip-feeding me computational wisdom at my own cognitive rate.

I am [Timur LATYPOFF](https://latypoff.com), and I'd like to make this document a complete guide for beginners (who already know Clojure though — I assume you kinda know what `clojure.core/atom` is and how `clojure.core.async/go` works), explaining all the library functions, and forming good basic understanding of what's happening, and how things work. Please suggest improvements!

This guide is meant to be used in the following way: firstly, best to read it from top to bottom, since there are many references to things defined or explained before. It is a good idea to copy-and-paste code examples to your REPL to try different variations until you understand each example. After you've read the guide completely, start coding, and feel free to search the page for specific function names that you need refreshed in your memory — this guide includes the entire Missionary's API. Also, please read other tutorials and guides on Missionary — they are not many, so it would not take a lot of time, but would let you think about the same things from different perspectives.

# Why even start with Missionary

I know why I personally want to use Missionary: I want to build apps with [Electric Clojure](https://github.com/hyperfiddle/electric)! Oh man, it's a magic technology for real-time interactive Clojure web-applications (maybe even more in the future). It's being built on top of Missionary, and understanding it — is the key to Electric Clojure.

I was so excited about Electric (former Photon), I'd started watching videos from one of its authors, [Dustin GETZ](https://github.com/dustingetz), and... I couldn't understand a thing! Why we call an `e/defn` with a dot? Why we catch a Pending exception to highlight an element while it's loading? How to add this magic to my existing project? How to make a really simple HelloWorld page with the bare minimum?

Dustin, a brilliant guy, trying to hide his smile from the pleasure of us, viewers, understanding the profound simplicity of UIs being just DAGs (directed acyclic graphs). Actually, not even DAGs, since Electric is so powerful, it gracefully resolves even cyclic dependencies, we don't even need to think about it! My eyes watering while I nod (I don't really remember much about DAGs from my university course. I just want to touch that magic).

![dags-yda-like-dags-mickey-oniel.gif][nextjournal#file#21da09e2-8dd6-4adf-8dd6-f6eae86bcd5e]

Digging deeper, I've found that the best way for me to start was using the [Biff Electric](https://biffweb.com/p/how-to-use-electric/) guide (since I already use Biff for my projects), and to grow things from there. To grow things, one needs to put actual data into the app, and Electric uses Missionary for data flows.

# What's in this guide

If you're here, you've likely already seen the [official API reference](https://cljdoc.org/d/missionary/missionary/b.31/api/missionary.core), the [official wiki](https://github.com/leonoel/missionary/wiki) with its [cheatsheet](https://github.com/leonoel/missionary/wiki/Cheatsheet). It's a good idea to also see the [explanation video](https://www.youtube.com/watch?v=tV-DoiGdUIo) from the library's author. In my opinion, Léo is very succinct and always technically correct, but that makes a very hard material for my mental teeth to chew, so I understand if you've also seen all those materials, and need additional guidance.

Feel free to use Cmd+F (or Ctrl+F if you swing that way) to search this document, and use Table of Content panel on the side to jump around. I want to cover all the current library functions and macros in this guide:

> `!` `?` `?<` `?>` `absolve` `amb` `amb=` `ap` `attempt` `blk` `buffer` `compel` `cp` `cpu` `dfv` `eduction` `group-by` `holding` `join` `latest` `mbx` `memo` `never` `none` `observe` `publisher` `race` `rdv` `reduce` `reductions` `relieve` `sample` `seed` `sem` `signal` `sleep` `sp` `stream` `subscribe` `timeout` `via` `via-call` `watch` `zip`

Further I am grouping them semantically.

Also, I will be using NextJournal to show code, so here's some initial setup for us.

```edn no-exec id=ffcf0396-b3f9-40e6-a0c2-654401879781
{:deps {org.clojure/clojure {:mvn/version "1.10.3"} ;; Clojure itself
        compliment/compliment {:mvn/version "0.3.9"} ;; Used by NextJournal, as I understand
        missionary/missionary {:mvn/version "b.31"} ;; Missionary library itself
        #_"end of dependencies"}}
```

```clojure id=2a07f446-303a-4b0a-9cf7-ff7424077bc2
(require '[missionary.core :as m])
(import 'missionary.Cancelled)
(println "Now we have everything we need")
```

# What's not in this guide

The following functions will not be covered because they have been deprecated. Just mentioning them here so that you can find them on the page.

> `?!` (alias for `?<`), `?=` (alias for `(?> ##Inf _)`), `??` (alias for `?>`) `aggregate` (alias for `reduce`), `amb>` (alias for `amb`), `enumerate` (alias for `seed`), `integrate` (alias for `reductions`), `reactor` and `reactor-call` and `signal!` (use lazy publishers `memo`, `stream`, `signal`), `transform` (alias for `eduction`)

Also, sadly, Electric Clojure is also not in this guide, because I don't understand it well so far. Actually, I don't understand Missionary well either — making this guide is my way of learning it.

# Tasks

In this section: `?` `absolve` `attempt` `blk` `cpu` `compel` `join` `memo` `never` `race` `sleep` `sp` `timeout` `via` `via-call`

**Task** ([official concept document](https://github.com/leonoel/task)) is a description of some operation that 1) **can be started**, then either finishes successfully and 2) **can return a result**, or 3) **fail with an exception** (I will be simplifying here and further — likely, the error value may be not only an exception, but any other value, however we risk getting too abstract, which is the actual problem we're trying to solve with this document), and it 4) **can be canceled**.

It's important to emphasize: a Missionary **task** is a **description** of an operation, not a running operation. So more like a "`defn` function definition that can be run later" rather than a "`future` running the function that will deliver a result later". Unlike, for example, C# [Task](https://learn.microsoft.com/en-us/dotnet/api/system.threading.tasks.task) which is a running operation that will deliver a result later.

### Running tasks

How is a task different from a plain function with no parameters `some-operation`? We can start it by calling. It may return a result, and we can use it.

```clojure id=049abd04-61b0-4509-9d4d-655668e0c5a6
(defn some-operation1 [] "RESULT")
(let [result (some-operation1)]
  (println result))
```

It may fail with an exception, we can catch it.

```clojure id=bcae4267-d0d7-48a2-97eb-f1eab871687a
(defn some-operation2 [] (throw (ex-info "FAILURE" {})))
(try (some-operation2)
  (catch Exception ex (println (ex-message ex))))
```

Can we cancel it? Well, there's Thread/interrupt, but then, firstly, we'd have to explicitly run a thread for this operation, and secondly, we might not be able to interrupt a thread if it does not call interruptible functions (see example below).

```clojure id=5c38aa89-1d21-4e20-b1a4-c7e18336bdba
;; Here's some CPU-heavy operation written in a way that (I think) prevents 
;; Clojure's compiler and JVM from optimizing it away - we _want_ it to be CPU-intensive!
(defn some-slow-operation3 [] (reduce + (range 0.0 10000000.0 0.1)))

;; We start a thread that only does CPU computations — can't .interrupt it!
(let [result (atom "Never updated result")
      thread (Thread. (fn [] (reset! result (some-slow-operation3))))]
  (.start thread)
  (Thread/sleep 50) ;; Giving it some time to start
  (.interrupt thread)
  ;; Yeah, we can .stop it, but it might leave our real-life system in a "dirty" state,
  ;; better not do it. That's why this function was deprecated
  ;; (.stop thread)
  (.join thread)
  (println @result)) ;; In the end, we print numeric result, 
                     ;; because we were not able to interrupt the thread, it finished its job.
```

So basically, we can't assume we can interrupt any function when we want it unless the function cooperates in some way (in our example, knows that it has to periodically execute interruptible functions). Also, it would be a shame to run each operation in a separate thread — it's slow and heavy on memory, and could be an overkill for some operations. Also, handling exceptions becomes more cumbersome when an operation runs in another thread.

What if we could write a function in a way that it might itself decide, whether it's worth to run it in a separate thread (or maybe a thread pool?), and how to cooperate for canceling itself? For sure the function knows better about what it does and what to do in each case.

Basically, that's what **task** is. It is an operation carefully written in a way that allows manipulations: starting it, accepting results of success or failure, and canceling — in a way that the caller doesn't have to think about it.

Technically task is a specially-crafted function. It takes two parameters: success callback and failure callback. It returns a zero-arguments function that cancels the task. 

```clojure id=d5ead80f-27df-400e-aaa4-e7c58e7b0082
(defn some-task [success failure]
  (do ;; Maybe even do stuff in another thread, if needed
    (if "Do stuff successfully?"
      (success "RESULT")
      (failure (ex-info "FAILURE" {}))))
  ;; But 
  (fn cancel-me [] "Cancel the operation if possible. The return value is to be ignored"))
```

Task also must guarantee that it is a good citizen who cooperates, therefore:

* it does not throw exceptions anywhere (because the caller expects that we are very careful, and always pass failure via failure callback, so that the caller doesn't have to think of all the possible ways things could go wrong);
* it is non-blocking (we don't slow down the caller, because they might be in a hurry — if we have something long to do, we should be a good boy/girl and do things in a separate thread);
* the cancellation functions that it returns is *idempotent* (that is, its caller may be sloppy, and call it multiple times, and that shouldn't make anything bad happen like NullPointerException's or deadlocks). Also, there's no shame if we can't actually cancel it — we just guarantee that we will do our best.

Now, it's a pleasure to use such function! If only all my colleagues wrote such functions that behave that predictably and offer so much flexibility!

```clojure id=d0320276-44c3-4b07-b49f-aa210f1d2f93
(let [cancel-task (some-task
                    (fn [result] (println "Succeeded with value" result))
                    (fn [ex] (println "Caught exception" (type ex)
                               "- feeling cute, might throw later")))]
  (Thread/sleep 50)
  ;; Hey, it's taking too long, I don't like it anymore, might as well try cancel it
  (cancel-task))
```

Ok, but it's a funny feeling to write functions this way, it's kinda hard to not forget things. And it also smells like callback hell actually! 

Here's where Missionary starts helping us.

We can use `?` to run a task as if it's a normal function if we want it.

```clojure id=f0842eb0-30df-44d2-82aa-a22e17f70bd7
(defn task-that-returns-five [success _]
  (success 5)
  #(do "Can't cancel, sorry!"))

(println "Result:" (m/? task-that-returns-five))
```

```clojure id=60e0a035-216d-46ce-a1dc-c358ca77227a
(defn task-that-always-fails [_ failure]
  (failure (ex-info "I've failed" {:who 'task-that-always-fails}))
  #())

(try
  (println "Result:" (m/? task-that-always-fails))
  (catch Exception ex
    (println "EXCEPTION:" (ex-message ex) (ex-data ex))))
```

Oh, by the way, our tasks actually have taken no arguments so far, like zero-arguments functions?! HEY! But what about multiple-arguments functions? That's what I actually use to do work?

Do not fear, just make a "task factory" function that returns a task, which you can run later as a well-behaved task.

```clojure id=245574fa-03c0-450e-8153-c1054d9ad756
(defn add-two-numbers [x y]
  (fn add-two-numbers-inner-task [success _failure]
    (success (+ x y))
    #(do "Can't cancel addition, too fast anyway :(")))

(m/? (add-two-numbers 3 7))
```

How neat is that?

In real world, you might not need a cancelable task for adding two numbers, but you might need a real cancelable task that requests data via API that runs asynchronously:

```clojure id=02d45a8f-a689-4f6e-ab7a-a3b3bcbfe4b8
(defn request-user-info [user-id]
  (fn [success failure]
    (let [fut (future
                (try
                  ;; Pretending to request data via HTTP
                  (Thread/sleep 3000)
                  ;; Passing result
                  (success {:user-id user-id
                            :user-name "Timur"})
                  (catch Throwable th
                    (failure th))))]
      ;; Returning a canceling function
      (fn request-canceler []
        (future-cancel fut)))))

(println "User 1234:" (m/? (request-user-info 1234)))
```

Let's actually go raw and try manually canceling the task three times: idempotency, remember? As a caller, we should not care.

```clojure id=84e91f7e-057d-4a31-8312-539443f1ef15
(let [task-about-to-be-run (request-user-info 4567)
      cancel-this-task (task-about-to-be-run
                         #(println "User 4567:" %)
                         #(println "Got exception:" (type %)))]
  (Thread/sleep 100) ;; Now we wait a bit
  (cancel-this-task)
  (cancel-this-task)
  (cancel-this-task))
```

It worked smoothly, which means that we have implemented the canceler correctly.

Now finally starts the fun part!

Missionary makes very easy for us to write tasks ourselves with its **magic** macro `sp` (**s**equential **p**rocess). Think of `(sp ...)` as just plain Clojure's `(do ...)`, but which, instead of returning a value of the last expression, returns *a task* you can run and get the value of the last expression. Why it's a magic macro? It's actually a built-in "compiler" that transforms the code inside to make sure it behaves like "good" task. It's magic in the same sense as `clojure.core.async/go` macro is magic — it transforms your clean code into ~~a garbled state machine mess that spews scary stack traces~~ an efficient implementation that hides the underlying complexity. But you don't have to think about — Missionary's author already did all the thinking (and did it well), just use it intuitively.

Here's a task that returns a result:

```clojure id=629c904b-b88c-4a57-827d-0ff9a29fd6ee
(def our-simple-task (m/sp
                       (let [v (+ 1 2)]
                         (do
                           "whatever")
                         (str "Our result " v))))
(println (m/? our-simple-task))
```

Below is a task that fails. You can already notice the first glimpse of the "magicness" of the `sp` macro.

```clojure id=69ca744a-9d7c-427b-a4ea-edf6fde17d92
(def our-simple-task-that-fails (m/sp
                                  (throw (ex-info "We've failed" {}))
                                  "Our result that is never reached"))

;; let's run the task it in a "raw" way to see that the exception is not actually 
;; thrown up our call stack (good tasks don't throw exceptions, remember?), 
;; but instead properly passed to the failure handler
(our-simple-task-that-fails
  (fn [result] (println "Success:" result))
  (fn [ex] (println "Here's our exception:" (ex-message ex))))
```

Yeah, but if we use `sp` and want to do some CPU-heavy or I/O-wait heavy operations, do they magically run automatically in a separate thread, so that we are a "good" task, and don't block the caller?

Not automatically, it would be too difficult for the macro-compiler to guess if spawning a new thread is justified. But you can use the `(via xxx ...)` macro, it's "`(do ...)` that returns a task like `sp`, but runs its body **in another thread**" (also, quick note that `via` is not "magical" like `sp` is, but you can use `sp` inside if you want). 

In which thread? The library pre-packages you with two thread pools (`cpu` and `blk`) which should be enough for your use in place of `xxx`. For computation-heavy workloads, use `cpu` — the maximum number of parallel threads is limited to the number of cores there are on the computer running the code, so that tasks don't compete with each other for CPU, and instead wait orderly until CPU is free to run them. For I/O-heavy workloads (reading/writing files, sending/receiving data from the internet or local network), use `blk` (**bl**oc**k**ing operations) — each task gets a separate thread (with efficient re-use, don't worry), which is not bad for performance in this case because these threads are expected to do mostly nothing, waiting for slow disk/network operations to complete (even a few milliseconds of wait time is *slow* for modern CPUs).

Cousin of `via` macro, `via-call` function, does the same thing, but instead of running given expressions in a separate thread, it runs a provided zero-arguments function in another thread. So `(via xxx ...)` is technically the same thing as `(via-call xxx (fn [] ...))`. Also note that `via` and `via-call` are not supported in ClojureScript because JavaScript does not have threads in the same sense as JVM, so it's not possible to run the CLJS code in parallel on two threads at the same time.

```clojure id=e5e53beb-53ea-47d0-baa6-bb2bc4577dfc
(defn request-from-rest-api [id]
  (m/via m/blk
    (Thread/sleep 3000) ;; we simulate some (http/get ...) that takes a long time
    {:id id
     :name "Timur"}))

(defn compute-value-on-cpu [input]
  (m/via m/cpu
    ;; we re-use our slow CPU-bound function from one of the examples above
    (let [result (some-slow-operation3)]
      (* input result))))

(def task-with-subthreads
  (m/sp
    (let [entity (m/? (request-from-rest-api 100))
          new-entity (assoc entity
                       :computed-value (m/? (compute-value-on-cpu -1)))]
      new-entity)))

(println "Result:" (m/? task-with-subthreads))
```

Second glimpse of `sp`'s magic: if we cancel the task, all internal sub-tasks are canceled automatically, as you'd actually think is logical — the "compiler" of `sp` automatically propagates the cancellation signal.

```clojure id=ba09c65c-4b44-4611-b8e7-78c942f8a111
;; Running the task in a "raw" way just to show cancellation
(let [cancel-it (task-with-subthreads
                  (bound-fn [result] (println "Result:" result))
                  (bound-fn [ex] (println "Exception:" (type ex))))]
  (Thread/sleep 100)
  (cancel-it)
  (Thread/sleep 300))
```

Some important notes here:

* We use `bound-fn` instead of `fn` for callbacks here, because the callbacks actually get called in non-main thread here (in `via`'s threadpools), and in the current version of Missionary that thread does not automatically receive Clojure's Var bindings (`*out*` in this case, so if we don't do `bound-fn`s, we won't see the `println`ed messages). The behavior might or might not change in the future, I think — it's a performance trade off which might or might not be worth it for a general-purpose library. But it's a gotcha that "steeps" the learning curve, and that's why we're here.
* I've used the second `(Thread/sleep 300)` after `(cancel-it)` — to give NextJournal a chance to collect our `println` outputs and show them here. If we don't have that delay, NextJournal thinks that execution is "done", and ignores further output from other threads. No need to do it in your own REPL, but here we serve our pedagogical purposes.
* Technical detail: cancellation of `via` blocks happens through our good old Thread/interrupt, no silver bullet here, so in our case we were able to cancel early only because Thread/sleep (our simulation for I/O operation) is interruptible. If we were to decide to cancel when our CPU-bound `compute-value-on-cpu` task runs, (cancel-it) would return quickly (cancellation function is non-blocking in good-behaving tasks), but we'd have to wait longer until our failure callback delivers its result. Actually, what the heck, let's simulate it:

```clojure id=9e51ca2b-cdae-40b4-b9e5-330df30df56c
;; Running the task in a "raw" way just to show cancellation
(let [cancel-it (task-with-subthreads
                  (bound-fn [result] (println "Result:" result))
                  (bound-fn [ex] (println "Exception:" (type ex))))]
  ;; wait enough for our I/O-bound task to complete, and CPU-bound task to start
  (Thread/sleep 3300)
  (cancel-it)
  ;; then wait enough to see the result
  (Thread/sleep 4000))
```

Not sure what NextJournal shows you as the result of execution of the previous block (we're playing with timings and race conditions here, so results may vary from time do time, depending on "cloud conditions" of the cloud where NextJournal is running), but when I'm writing this, it shows the following:

![Screenshot 2023-08-29 at 11.15.03.png][nextjournal#file#97358239-413c-42d2-8b00-7f1e54b4450e]

Although we've decided to cancel early — when we decided that, our task was running a CPU-bound task in another thread, and that task was not cooperating with JVM's thread interruption (for example, by checking for Thread/interrupted to exit early), as a result **not only** we had to wait until that `via` task finishes its code block, we even received the final result which we said we don't want anymore. Oh well, such is life. Cancellation is on best-effort basis, it's a "pretty please", not a do-or-die order. "You can lead a horse to water, but you can't make him drink".

### Running tasks in parallel

What's the point of running in another thread though, if we still wait for the result, and total execution time is the same, since only one processor core is working at all times? Fair question. What's the point of using Clojure if we don't use all CPUs to the max? I already said it's a fair question, please, no need to force it.

The power of **tasks** (that is, *functions written carefully in a way that make them more controllable* — **not** some magic classes or macros that solve all problems) allows us to manipulate them in a uniform way, we just use the instruments provided by Missionary, without caring about internal details of tasks we are manipulating.

Function `join` runs multiple tasks in parallel and returns (as a task) their result combined in the original order (not in their order of completion) with a function that you provide (basically, because in Clojure, a function can't return multiple values, so we might want to pack them into a vector for further destructuring, or maybe combine the results in some other way — Missionary gives you freedom to supply your own combining function). Naturally, if an error happens, it propagates upwards, and remaining subtasks are automatically canceled.

```clojure id=86c5324d-671c-457d-a593-528049860887
(println (m/? (m/join vector
                (m/sleep 1000 :hi)
                (m/sleep 1500 :there)
                (m/sleep 500 :fellow)
                (m/sleep 0 :curious-reader))))
(println (m/? (m/join +
                (m/sleep 1000 3)
                (m/sleep 1500 4))))
(try
  (println "Result is"
    (m/? (m/join vector
           ;; first task returns :success after 100 ms
           (m/sleep 100 :success)
           ;; second task signals faillure after 400 ms
           (m/sleep 400 (m/? (m/sp
                               (throw (ex-info "Failure in one of the branches" {}))))))))
  (catch Exception ex (println "Exception:" (ex-message ex))))
```

What's the `sleep` function mentioned above? It makes a task that "sleeps" for a given amount of milliseconds, and then returns the value you provide (or `nil`, if you don't give it the second argument). We'll use this function instead of Thread/sleep from now on, because Thread/sleep **blocks** the thread that calls it, but `sleep` **parks** instead (the same way as `clojure.core.async`'s `<!` and `>!` **park** their `go` block instead of **blocking** like `<!!` and `>!!`), so it kind of simulates the delay, but does not prevent you from running multiple `sleep`s in the same thread in parallel, as it's shown in the example above. Note that there are no new threads created, it's all single-threaded.

By the way, wanna see the third glimpse of `sp`'s magic? When used inside `sp` block, `?` also **parks** instead of blocking. When it's used outside of `sp`, it **blocks** execution thread until the task completes with either success or failure (as we've naturally assumed before). But inside — it parks, which means we can run multiple *question marks* "in parallel" on a single thread:

```clojure id=008c648e-d150-4b40-be13-42015e92642f
(m/? ;; The outer m/? is here just to force the m/sp to actually run. This one blocks.
  (m/join vector
    ;; all five question marks below run "in parallel" on a single thread, they park their
    ;; parent tasks (generated by m/sp) instead of blocking our thread from doing anything
    (m/sp (m/? (m/sleep 1000 :how)))
    (m/sp (m/? (m/sleep 1000 :do)))
    (m/sp (m/? (m/sleep 1000 :you)))
    (m/sp (m/? (m/sleep 1000 :like)))
    (m/sp (m/? (m/sleep 1000 :this?)))))
```

Quick aside: inside `sp` you can use any Clojure code, even throw and catch exceptions and it correctly interacts with parking, just like in `clojure.core.async/go`.

```clojure id=0e3cdc8b-b4ee-48ed-be73-c1ade9955f50
(m/? (m/sp
       (try
         (m/? (m/sleep 1000))
         (throw (ex-info "Thrown after parking" {}))
         (m/? (m/sleep 1000)) ;; this sleep is not reached
         (catch Exception ex
           (tap> (str "Exception caught in m/sp across parking sites: " (ex-message ex)))
           :successful-value-of-the-task))))
```

So, back to `join`. It runs subtasks in parallel and waits for all of them to provide results (so it lasts as long as its longest-lasting subtask). What if we don't want to wait for the slowest subtask? Say, we send a request to three data centers, and we use the fastest result, and ignore the rest? We use `race` for that: we supply multiple tasks, and whenever one of them returns a successful result, it will be a result of the task that our `race` returns. What if any subtasks fail? We don't care, we just care about the fastest, the fittest task that delivers us value, we don't care about the rest. In fact, we care so little that when we have one successful result already, all remaining tasks are automatically canceled from wasting our precious resources (how neat is that! The power of tasks!).

```clojure id=40b2e226-8642-4251-aa66-de3dca1ae735
(defn request-result-via-api-from-region [region]
  (m/sp ;; wrapping in m/sp so that exception is thrown in task-oriented way and when task is run,
        ;; not when the task is being created (before being run)
    (let [network-delay (case region
                          :frankfurt 30 ;; Frankfurt's data center is 30ms away
                          :sydney 800 ;; Australian one is 800 ms away
                          :boston 250 ;; Boston is 250 ms away
                          (throw (ex-info (str "No data center in " region) {:region region})))]
      (m/? (m/sleep
             network-delay
             (str "Result from " region " after " network-delay " ms delay"))))))

(println "What we have:"
  (m/?
    (m/race
      (request-result-via-api-from-region :sydney)
      (request-result-via-api-from-region :boston)
      ;; request to Dubai fails, we don't have a data center there
      (request-result-via-api-from-region :dubai))))
```

What if we want to send a request with a timeout? Let's implement a helper function for that:

```clojure id=5d31a0e2-9464-4ec0-a684-5fc9b4c0cd2e
(defn with-timeout [some-task timeout-ms timeout-value]
  (m/race
    some-task
    (m/sleep timeout-ms timeout-value)))

(let [result (m/? (with-timeout (request-result-via-api-from-region :sydney)
                   100 "Timeout limit of 100 ms has been reached"))]
  (println "Result:" result))
```

It works! **Or does it?** (It doesn't)

Let's try requesting from Jakarta:

```clojure id=40ee2698-7d25-45b3-bd1b-33d9fa87a327
(try
  (let [result (m/? (with-timeout (request-result-via-api-from-region :jakarta)
                      100 "Timeout limit of 100 ms has been reached"))]
    (println "Result:" result))
  (catch Exception ex (println "Error:" (ex-message ex))))
```

The request has failed (we don't have data centers in Jakarta), but instead we are told that the request took too long. It didn't take too long though, it failed immediately. Moreover, if we do it with a timeout limit of 30 seconds, our implementation of `with-timeout` not only tells us again that the request has took too long, it will also force us to wait 30 seconds for no good reason.

It's actually not what we want at all when we use timeouts. We want to know if our main operation actually failed. Thankfully, there's a correct version that comes bundled with Missionary: `timeout`. Let's try it in exactly the same way:

```clojure id=af2086f2-0d9e-4cfd-ac55-fc0346892c80
(try
  (let [result (m/? (m/timeout (request-result-via-api-from-region :jakarta)
                      100 "Timeout limit of 100 ms has been reached"))]
    (println "Result:" result))
  (catch Exception ex (println "Error:" (ex-message ex))))
```

Nice! But how does `timeout` work then? I am curious too, let's take a look at [its source code](https://github.com/leonoel/missionary/blob/f594fb0a7bcc2fb2ab8cf75c1d1d190c638bf9f8/src/missionary/core.cljc#L160C3-L166C20). Actually, here it is with some minor modifications for clarity:

```clojure no-exec id=acd0619c-0653-4a37-9fe0-b0e435ab4fce
(fn timeout
  ([some-task timeout-ms] (timeout some-task timeout-ms nil))
  ([some-task timeout-ms timeout-value]
   (-> some-task
     (attempt)
     (race (sleep timeout-ms #(-> timeout-value)))
     (absolve))))
```

Léo does use the `race` function (our intuition was correct), but before racing he `attempt`s our task, and afterwards he `absolve`s it! These two functions do exactly the opposite of each other. Both `attempt` and `absolve` **take a task** and **return a task**. Simply put, `attempt` takes a task that might succeed or fail, and converts it to a task that **always succeeds** with *a surprise box*, `absolve` does the opposite, takes a task that succeeds with a *surprise box*, and unpacks it and, as a result, **either succeeds** with a value that was inside, **or fails** with the exception that was inside.

This way, if our `some-task` fails, `race` does not discard it as an inferior task not worthy of attention — attempt wraps the failure in a colorful paper and pretends it a success.

![surprise.png][nextjournal#file#4c08de15-ea8f-4f8a-8c4c-51dadf6b9db4]

Our `race` passes further the fast success as a winner, and then `absolve` "unpacks" it and reveals the truth.

By the way, what's the magic "packaging" technology used for putting values and exceptions into a *surprise box*? If you have to ask, it's a zero-arity function which, when you call it, either returns that value, or throws that exception. See that part `#(-> timeout-value)`? It's where a value is wrapped in a zero-arity function that returns it, so that `absolve` could further unpack it and deliver our `timeout-value` to our caller. You'd better get used to wrapping functions with functions, we haven't yet even reached the middle of this guide!

Ok, what's `compel` for? It's for modifying a task to make it uncancelable after it has started. Usually it's pretty cool that when you cancel some task, the entire tree of unfinished subtasks is canceled, to free up resources of CPU and memory for further processing tasks. However, what if we **have to** deliver business value by registering that a user clicked a button, and we cannot miss it under any circumstances? We `compel` the valuable task to do the job.

```clojure id=d31f2704-b3f1-466a-86db-dabe3061ac33
(defn register-business-value-event [& whatever]
  (m/sp
    ;; I've recalled the way to print-debug 
    ;; in a threading-agnostic way, phew
    (tap> "Attempting to deliver value...")
    (m/? (m/sleep 2000))
    (tap> "Stakeholders have been aligned. Good job.")))

(try
  (println "Result:"
    (m/? (m/join vector
           (request-result-via-api-from-region :jakarta)
           (m/compel
             (register-business-value-event {:who? :user :did-what? :click :with-what? :button})))))
  (catch Exception ex (println "Error:" (ex-message ex))))
```

In the example above, `register-business-value-event` starts such an important task, we don't want it to be canceled if our `(request-result-via-api-from-region :jakarta)` fails. If we didn't `compel`, `join` would cancel all subtasks if one of them fails.

The last function we haven't seen is `memo`. It does to a task a similar thing that `clojure.core/memoize` does to a zero-arity function, or what `clojure.core/delay` does to some calculation. When we start a `memo`ed task, it only runs the first time, for subsequent runs the results are recalled and returned immediately. 

```clojure id=562b6433-df7a-4370-9754-6b95d85e35e9
(defn request-application-settings [central-server]
  (m/sp
    (tap> (str "Requesting settings from " central-server "..."))
    (m/? (m/sleep 1000))
    (tap> (str "Received settings from " central-server "."))
    {:should-start? true}))

(def startup-app-settings-the-wrong-way (request-application-settings "example.com"))

(println "1) Settings are:" (m/? startup-app-settings-the-wrong-way))
(println "2) Settings are:" (m/? startup-app-settings-the-wrong-way))
(println "3) Settings are:" (m/? startup-app-settings-the-wrong-way))

(def startup-app-settings (m/memo (request-application-settings "dont-ddos-me.com")))

(println "1) Memorized settings are" (m/? startup-app-settings))
(println "2) Memorized settings are" (m/? startup-app-settings))
(println "3) Memorized settings are" (m/? startup-app-settings))
```

In the example above you can see that `memo` made sure we only requested initial application settings from our server once, but when we didn't use `memo`, running a task every time started a new network request.

And `never` is a task that never succeeds (it's like an infinite `sleep`, you can only cancel it), it's like an infinite `sleep` — you can only cancel it. Please note that it's not a function that **returns** a task, it's *already* **represents** a "singleton" task that is ready to run (so be careful with how many parentheses you put around it).

```clojure id=3120d88c-0475-43c9-b550-de98541a873d
(println
  (m/?
    (m/timeout m/never ;; NOTE: not (m/never) here, m/never does not MAKE a task, it IS a task
      1000
      "We don't have time to wait forever in this guide, we have to start learning flows")))
```

# Flows

**Flow** [(official concept document](https://github.com/leonoel/flow), but in the current version of Missionary I've found things working a bit differently) is a description of some kind of sequence of **values** that go one after the other. When I get to learn about a new entity, I like to understand what it can do. Like a **task** that can do four things, a **flow** technically can do six things:

1. It can be started.
2. It can be canceled.
3. It can indicate that it has been canceled.
4. It can indicate that a value is **ready**.
5. It can indicate that no more values will be **ready**, the flow has ran out by itself.
6. It can produce a value that has been previously indicated as **ready**.

Things that you might find similar to what a flow is (but not exactly the same, of course, otherwise there would be no Missionary):

* Clojure's sequence, including a lazy sequence — it is also zero or more values that could go one after another.
* A clojure.core.async's channel — it also produces zero or more values, one at a time.
* Clojure's Atom or Ref or Agent — they all can show you different values over time, and you can subscribe to new values via `add-watch` if you want.

Any thing that changes in value over time or produces multiple results over time (or at once) can be modeled by Missionary's flow in a uniform way.

* As you press buttons on your keyboard, a **flow** of keyboard events travels through your operating system and branches into individual apps, making sure that letters retain the same order as you pressed them (except in macOS when you switch input language).
* Your mouse pointer can be thought of having a **flow** of 2D screen coordinates, showing new value every time you move the cursor.
* Your user profile on a website forms a **flow** of states, producing new updated values when you change your username.
* Your age is a **flow** of integers, producing new increased value once a year, and indicating that no more values will be produced at the moment you die.

### How flows work inside

No need to get sad though, here are some technical details on what a flow is technically. Just like a **task**, a **flow** is a function that takes two callbacks (different ones though), and returns a zero-argument function that cancels it. Also there are similar guarantees that running any functions (the flow's function itself, the canceler, the callbacks) should not block the calling thread, and that no exceptions should be thrown under normal operation with the exception (🤔) of `missionary.Cancelled` — this is an exception that could be thrown anywhere. Actually, `missionary.Cancelled` is **not** an exception, it extends `Throwable` directly, so `(catch Exception _)` will not catch it (and that is good, in most cases you don't want to catch it yourself). Now that I've confused you enough, you will remember for the rest of your life that `missionary.Cancelled` is something to be careful about.

There similarities with tasks end, let's try constructing some example flows with "raw" code, to get a better feeling. Here is a more complete description. Flow is a function that takes two zero-argument callbacks: first one notifies that a new value is **ready**, second one notifies that no more values will be ready in the future. Running the function starts the flow. The function must return an object which is **both** an **idempotent** zero-arity function that cancels the flow **and** is something a `deref` or `@` can be called upon in a non-blocking way to retrieve a **ready** value (if at the moment of a `deref` call the value is not ready, it's okay to crash the program with an exception just out of spite — but don't block!). 

Any of the functions or callbacks may throw `missionary.Cancelled` to indicate the entire thing has been canceled. Please note that it's different from indicating that no more values will be ready (running out), although the difference might be subtle. Usually, **cancellation** is either caused by some outside code, or happens due to cancellation of some other flows linked to our flow, whereas **running out** of events is due to the nature of events or changing values we're modeling: end of file, collection of elements exhausted, player logged off. Cancellation will likely cascade with further cancellations of upstream and downstream flows, on the other hand *end-of-event-stream* is a valid sign of a job well done.

By the way, a kinda cool side-effect of flow functions taking two **zero**-arity callbacks, and task functions taking two **one**-arity callbacks, you get an exception saying something like "Wrong number of args (1) passed to: ...", which means you've used a flow in a place where a task was expected. I don't want to start a debate about whether static type checks would provide superior programmer productivity in this case, here's the best counter-argument I have:

![7xnmpj.jpg][nextjournal#file#ca2203ae-5f80-4a56-854b-1da814bbeb34]

Anyway, here's a flow that produces no values and stops:

```clojure id=1974925d-c3be-42a9-94cc-ec934762f1b8
(defn flow-producing-no-values-before-running-out [_value-is-ready! no-more-values!]
  (no-more-values!)
  (reify
    clojure.lang.IFn
    ;; Cancellation function. Note that we're idempotent here
    (invoke [_this] (println "They tried to cancel me :("))
    
    clojure.lang.IDeref
    ;; Value retrieval function. In this case, the value can never be ready, so we crash
    ;; those who tried to retrieve it
    (deref [_this] (throw (ex-info "C'mon man" {})))))

(let [flow-control-panel (flow-producing-no-values-before-running-out
                           #(println "NEW VALUE READY!")
                           #(println "NO MORE NEW VALUES!"))]
  ;; getting a value from the flow:
  (try
    (println "Current value:" @flow-control-panel)
    (catch Throwable th (println "What did I do? Why throw this:" (ex-message th))))
  ;; canceling the flow:
  (flow-control-panel))
```

Here's a function that makes a flow that produces a single value, then finishes:

```clojure id=2547db1f-e39a-490f-b80c-38a7ac8745f7
(defn make-a-flow-from-a-single-value [the-single-value]
  (fn flow-producing-no-values-before-running-out [value-is-ready! no-more-values!]
    (value-is-ready!)
    (reify
      clojure.lang.IFn
      (invoke [_]
        (println "Already done by the time you attempted to cancel :)"))
      
      clojure.lang.IDeref
      ;; We already know what value we will produce, so our job here is simple
      (deref [_this]
        (no-more-values!) ;; we indicate that no more values will follow
        the-single-value))))

(let [the-flow (make-a-flow-from-a-single-value :some-valuable-thing)
      flow-control-panel (the-flow
                           #(println "NEW VALUE READY!")
                           #(println "NO MORE NEW VALUES!"))]
  ;; getting a value from the flow:
  (println "Current value:" @flow-control-panel)
  ;; canceling the flow:
  (flow-control-panel))
```

I think that enough for starters, let's get into some philosophy.

## Taxonomy of flows

Google's definitions from [Oxford Languages](https://languages.oup.com/google-dictionary-en/):

![Screenshot 2023-09-02 at 12.20.41.png][nextjournal#file#65ca7c06-3adc-4baa-a7ec-52ebde274103]

I think it is important to keep in mind, *how* flows can be classified into mutually exclusive classes in multiple ways, to understand their nature better, and how to handle them in your use case.

### Discrete / continuous

Officially, these two "classes" of flows are recognized by the library's author. It's a **very** important distinction which you should **always** keep in mind, and probably in a different, statically-typed programming language, those would be two different types, so that a compiler can ensure you don't accidentally mix them together and scratch your head. Mixed with the fact that Missonary's exceptions are rather hard to debug (error messages do not really tell you exactly where you messed up — and it's not really the library author's fault, because there's a lot of real magic going on under the hood), I want to emphasize how important is to think, which of the flows you work with are discrete, and which ones are continuous.

**Discrete flow** — is stream of events, each having its own significance. You can think of it as something similar to a `java.util.concurrent.BlockingQueue`, or a `core.async` channel, or a Clojure's lazy sequence. New values come whenever they wish at any time — a million of new values could come each second, waiting for you to retrieve and process them one after the other, or you could be waiting for the next event for 48 hours, or for an eternity. 

Some examples of discrete flows: a stream of *changes ("deltas")* in mouse coordinates, real-time debit or credit operations on your bank account, series of database transactions adding items to your online shopping cart, records of historical daily stock price changes.

**Continuous flow** — is a value that may change over time, but you only really care about the current value. An analogy is Clojure's Atom or Ref or an Agent — usually you're not interested in how many times per second the value changes (although you can track each change if you want), you're interested in the outcome of all the changes: the current value, the most up-to-date thing, and you are ready to discard any intermediary updates between your subsequent reads. Also you want to be able to know the current value at any time without waiting, it should always be **ready**.

Some examples of continuous flows: your current mouse pointer's coordinate, your bank account's balance, your shopping cart's contents, current stock prices.

I hope from the examples above you can get a feeling how discrete and continuous flows *about the same things* are related semantically. It's not something about real-world things themselves that make flows discrete and continuous in each case, it's about what you want to achieve.

### Initialized / uninitialized

In previous drafts, this dichotomy was called "headful / headless", because I didn't see anywhere in the library docs any specific mention of this property, but recently I've seen Léo using the term "initialized", so I've decided we'd better switch to the official terminology here.

A flow is **initialized** if, upon running, it **immediately** has a value **ready** (in the terminology of our manually-crafted example flows above, `value-is-ready!` callback is called *before* the flow-running function returns).

We say that a flow is **uninitialized** in the opposite case — when we can't retrieve a ready value right after the flow is run.

From the explanation of continuous flow above we understand that a **continuous** flow is usually expected to always have some retrievable value at all times — even right after they've been started, so ideally all your **continuous** flows should be **initialized**. However you might run into situations where Missionary silently allows you to accidentally construct **uninitialized** **continuous** flows, and that violates expectations of some functions working with continuous flows, and you get an exception saying something like "Undefined continuous flow", which was not clear for me as a beginner ("I clearly see that I *define* my continuous flow, what else do you want?").

So make sure you think whether your continuous flow has some initial value naturally, *or* you have to **supply the initial value** separately, to make the flow "well-behaved".

For example, you might want to have a continuous flow of "current logged-in user's details retrieved from database" — but is it a **initialized** flow? No, because before your first database query finishes, you don't have any defined value in the flow. So you have to separately supply this flow with a head value of `nil` or `:loading-in-progress` or whatever fits your use case.

For **discrete** flow, you usually don't care much about *initializedness* of them because consumers of discrete flows are expected to wait for new values — unless you plan to construct a **continuous** flow from that **discrete** one later.

### Backpressure-passing / decoupled

Let's first quickly cover what backpressure is.

*Backpressure* is resistance of a flow in the opposite direction. Values go inside the flow from one side (from an *upstream* end), and are taken from the flow from the opposite end (from a *downstream* end). Programmatically, you are free to retrieve a value from a flow at any time, so not taking a value when it's ready creates backpressure, some resistance appears in how fast the values move through the "pipes", and it might affect someone who's about to put a new value into the flow.

We will call a flow **backpressure-passing** (official term — **coupled**) if a slow downstream reader of values from a flow prevents an upstream writer of values from putting the next value of the flow — that is, the flow passes backpressure to the flow that feeds it ("freezes" it), which in turn might pass it further, etc. This way, backpressure affects the speed of the entire chain of connected backpressure-passing flows.

We will call a flow **decoupled** if it a slow reader does not affect how fast new values might arrive into the flow, and thus, doesn't slow down the upstream chain of flows. Decoupled flows have some mechanism of relieving the backpressure.

Quick note that by definition, **continuous** flows are decoupled, as when consuming from them, you are relieved from handling intermediary values that happen between your attempts of consumption.

### Finite / infinite

This distinction is a minor and "obvious" one, but it has caught me off-guard as a beginner enough times that I decided it's worth a mention.

**Finite** flows run out by themselves (technically, they at some point call the `no-more-values!` callback from our examples above). **Infinite** flows don't. Pretty simple. But the next time you try to print the collection of all elements of a flow while debugging, and your REPL hangs, you know that you forgot that the flow is in fact infinite, and you can't print *all* the values from it, but instead you should print them one by one as they come. Also it is in general useful (I think) to ask yourself questions when programming: what will happen in this case when this flow **spontaneously terminates** (official term)? under what circumstances it might run out? will it *spontaneously terminate* or will it *cancel itself* instead?

## Flow basics

In this section: `?<` `?>` `amb` `amb=` `ap` `none` `reduce` `seed` `zip`

Let's start with the simplest functions. Remember when we made our own `flow-producing-no-values-before-running-out`? Built-in Missionary's function `none` is exactly equivalent to ours, it **represents** a flow that after starting immediately indicates that it has run out. Just like `never` with tasks, `none` doesn't **make** a flow (it doesn't *return* a two-arity function that runs the flow), it **represent** a "singleton" flow (it *is* the two-arity function itself). So be careful about the number of round parentheses around it in code.

```clojure id=728b5c13-aaa5-482c-896e-0946ba366720
(def running-flow-state (m/none ;; NOTE: not (m/none) here, m/none does not MAKE a flow, it IS a flow
                          #(println "The flow said a value is ready")
                          #(println "The flow said it has run out")))
```

To which categories does the `none` flow belong? To be honest, looking at the four categories above, I think we can only say it's *finite*, all other classes don't make sense in this case because it runs out before having a chance to demonstrate any properties, so other categories don't apply. Maybe *uninitialized*?

A function `seed` makes a flow of values from a collection (including lazy sequences) that you supply. The resulting flow is *discrete*, *initialized*, *backpressure-passing* (in this case — in a way that a lazy sequence will be evaluated lazily, as new values are needed), and either *finite* or *infinite*, depending on whether you pass it a finite or infinite sequence. Please note that Clojure's lazy sequences may block (for example, ones that are being read from a file with `clojure.core/line-seq`), which means that `seed`'s value retrieval operation will block in this case, which is a no-no (you violate library's expectations — the library might have to violate your expectations in return).

We will also need this function now: `reduce` — it's a function exactly like `clojure.core/reduce` and `clojure.core.async/reduce`, but for flows instead of sequences and channels. It takes three parameters: a **reducing function**, an initial value, and a flow. If you don't supply the initial value (so call `reduce` as a two-arity function), your reducing function will be called with zero arguments first to generate the initial value, very similar to what happens in `clojure.core/reduce`. The flow that `reduce` takes as an input can be of any class (please keep in mind that if you pass it an infinite flow, your resulting task will never succeed with a final product of reduction — however you are free to do side effects in the reducing function). As its result, reduce makes a task that would succeed with the result of reduction (it can't just return the result, it *has to* return a task, because not all values of the flow might be available for reduction at the moment we run `reduce`, so it returns a task that will provide a result when it's ready). 

Let's try to reduce some things:

```clojure id=54ff7c44-fc6a-427e-a301-7faca9ee3300
(println "Reduce of `none` with conj into a list:"
   (m/? (m/reduce conj '() m/none)))
(println "Reduce of `none` with conj with no initial value:"
  (m/? (m/reduce conj m/none))) ;; conj will be called as a zero-arity function to get an initial value

(println "Reduce of flow of [1 2 3 2 1] with conj into a set:"
  (m/? (m/reduce conj #{} (m/seed [1 2 3 2 1]))))
(println "Sum of a finite flow of numbers 0 1 2 3 4 5:"
  (m/? (m/reduce + (m/seed (range 6)))))

;; Print each successive value from a flow
(m/? (m/reduce
       (fn [_ x] (prn x))
       :whatever ;; our reducing function doesn't have a zero-arity, so we have to supply an initial value
       (m/seed (take 6 (cycle (reverse (range 4)))))))
```

The `zip` function is for combining successive new values of multiple flows into a single flow. So it combines the first new values from each input flow, then the second new values from each flow, then the third ones — with a combining function you supply. Its exact equivalent for sequences is `clojure.core/map`, so `(m/zip vector flow1 flow2 flow3)` is equivalent to `(map vector seq1 seq2 seq3)`, and yes, you can `zip` a single flow, so it's just mapping a function over a flow's subsequent values. Input flows of `zip` will pass mutual backpressure upstream (not only backpressure from a downstream consumer, but also the backpressure that happens from waiting each subsequent value from all of the inputs at the same time, so one slow input flow will slow down all the rest), and usually you want to `zip` discrete flows (because you're interested in each subsequent value in order, not the "current" value — for the case when you want current values there's a similar function `latest`, we'll get to it later). If at least one of the input flows runs out (one input flow is finite), `zip`'s resulting flow also ends (result also is *finite* in this case), and all other input flows are canceled automatically.

```clojure id=4bc08949-b0a3-4bc8-a2d2-5fa851aa2e8f
(prn (m/? (m/reduce conj {} (m/zip vector
                              (m/seed [:key1 :key2 :key3])
                              (m/seed ["value1" "value2" "value3"])))))
```

Let's make a useful function for inspecting flows, we will be using it a lot further. We want it to work with all classes of flows, but we also don't want to wait infinitely if we stumble upon an infinite flow, and we don't want to print too many values if the flow produces too many values. Also I'd like to see if a flow is initialized or not, and since our flows are async, and new values might come over time, so I'd like to know how much time passed between each successive values.

It turned out a bit complicated, but it's complicated on the Clojure side, not on the Missionary side, you already know all these things:

```clojure id=4a03b99d-e254-45fc-aee7-e7beff5b0d11
;; This is a helper function that takes a flow and returns the same flow,
;; but also reports whether the flow is initialized
(defn report-whether-flow-initialized [name wrapped-flow]
  (fn [value-is-ready! no-more-values!] ;; we're raw dogging a flow like pros
    (let [called-ready-during-startup? (atom true)
          check-where-we-are-once (delay
                                    (if @called-ready-during-startup?
                                      (tap> (str "> Flow '" name "' is initialized"))
                                      (tap> (str "> Flow '" name "' is UN-initialized")))
                                    :done)
          
          _ (tap> (str "> Flow '" name "' is starting"))
          ;; we're running the flow we're wrapping
          wrapped-flow-control-panel
          #_=> (wrapped-flow
                 (fn wrapped-value-is-ready! []
                   (force check-where-we-are-once) ;; do our check before notifying downstream
                   (value-is-ready!))
                 no-more-values!)]
      (reset! called-ready-during-startup? false)
      (force check-where-we-are-once)
      ;; then we return the canceler/retriever of our original flow
      wrapped-flow-control-panel)))

;; This is also a helper function that packs each value of a flow within [index value],
;; just like you'd do (map-indexed vector some-sequence) — so that when printing each value,
;; we could also print the index of the value, which is very informative.
;; We could have just used (m/zip vector (m/seed (range)) the-flow), but m/zip currently 
;; has an issue with not working properly with some empty or infinite flows — 
;; https://github.com/leonoel/missionary/issues/74 — so we work around that for our
;; educational purposes
(defn mark-each-subsequent-value-with-an-index [wrapped-flow]
  (fn [value-is-ready! no-more-values!] ;; we're going raw again, you like to see it!
    (let [current-index (atom 0)
          wrapped-flow-control-panel (wrapped-flow value-is-ready! no-more-values!)]
      (reify
        clojure.lang.IFn
        (invoke [_]
          (wrapped-flow-control-panel))
        clojure.lang.IDeref
        (deref [_this]
          [(first (swap-vals! current-index inc))
           (deref wrapped-flow-control-panel)])))))

;; I'm using exclamation mark in the name to remind myself
;; in the future that this function blocks
(defn inspect-flow! [name flow]
  (let [MAX_NUMBER_OF_RESULTS_WE_WANT 10
        MILLISECONDS_WE_ARE_READY_TO_WAIT 5000
        result (m/? (m/timeout
                      (m/reduce
                        (fn [last-timestamp-ms [index new-value]]
                          (if (= index MAX_NUMBER_OF_RESULTS_WE_WANT)
                            ;; if we don't want any more values, stop the reduction
                            (reduced ::flow-hasn't-yet-run-out)
                            ;; otherwise report a new value
                            (let [current-timestamp-ms (System/currentTimeMillis)
                                  delay-ms (- current-timestamp-ms last-timestamp-ms)]
                              ;; we use tap> instead of printing, so that we can inspect in any thread
                              ;; in NextJournal environment
                              (tap>
                                (str "> " name "[" index "] after "
                                  delay-ms "ms: " (pr-str new-value)))
                              current-timestamp-ms)))
                        (System/currentTimeMillis) ;; start the clock
                        (->> flow
                          ;; report flow's initial properties
                          (report-whether-flow-initialized name)
                          ;; mark each new value with its index for displaying
                          (mark-each-subsequent-value-with-an-index)))
                      MILLISECONDS_WE_ARE_READY_TO_WAIT
                      ::flow-hasn't-yet-run-out))]
    (if (= ::flow-hasn't-yet-run-out result)
      (tap> (str "> Flow '" name "' continues afterwards..."))
      (tap> (str "> Flow '" name "' has ran out completely.")))))

(inspect-flow! "none" m/none) 
(inspect-flow! "seed-5-values" (m/seed (range 40 45)))
(inspect-flow! "seed-15-values" (m/seed (range 50 65)))
(inspect-flow! "seed-infinite" (m/seed (range)))
```

If you don't think this `inspect-flow!` function is neat, idk istg fr fr.

Now let's finally meet some magic again: `ap` (**a**mbiguous **p**rocess). On #missionary Slack experienced people say that it's the most powerful and complex macro that should be taught last. I respectfully disagree, I think it's the most interesting (maybe even — genius?) macro that is pretty straightforward and 'tactile' (I can feel how it works with the tips of my fingers) when you wrap your head around it.

This `ap` macro is similar to `sp` we've seen with tasks (but even more magical and more powerful), and it generates a discrete flow (that is either backpressure-passing, or decoupled, we'll see later), which could be initialized or not depending on when you start emitting values (immediately or later). It's a flow-magic version of Clojure's built-in `do`.

Let's try it!

```clojure id=70077d97-d6c2-41b9-919c-08e057c1ec64
(inspect-flow! "empty-body" (m/ap))
(inspect-flow! "body-of-just-nil" (m/ap nil))
```

As you can see, just like with Clojure's built-in `do`, empty body `(do)` is the same as `(do nil)` — it just returns a `nil` value, but here we are in flow-generating business, so we generate a flow which produces a single value — `nil`. 

You may also notice that our resulting flow is *initialized*, because we produced the value immediately. How can we produce a value not immediately? We can use our magic `?` operator, just like in `sp`, to await for some task result — `ap` possesses all the magic powers of its little cousin `sp` from the tasks family (and more!).

```clojure id=1eca05e0-e534-48a4-abae-90514f262dc5
(inspect-flow! "produce-delayed-value"
  (m/ap
    (m/? (m/sleep 100 :my-delayed-value))))
```

You may notice that now our flow is ***un****initialized* now, because we didn't produce the value immediately.

By the way, why is `ap` called **a**mbiguous **p**rocess? Because it's ambiguous like "I am feeling cute today, might produce value later". If we're not feeling cute today, we might produce no value in our flow (empty flow, exactly the same as `none`), see below:

```clojure id=246d97a7-3b24-4426-893f-7dd71a6a33fb
(inspect-flow! "empty-flow"
  (m/ap
    (m/amb)))
```

The `amb` operator used inside `ap` lets us be **amb**iguous. It allows us to return nothing — or return something if we decide so.

```clojure id=62f625f5-946b-424b-879c-f077f00aa76c
(def should-be-empty?)
(def the-ambiguous-flow
  (m/ap
    (if should-be-empty?
      (m/amb)
      (m/amb :result))))

(alter-var-root #'should-be-empty? (constantly true))
(inspect-flow! "empty-or-not" the-ambiguous-flow)
(alter-var-root #'should-be-empty? (constantly false))
(inspect-flow! "empty-or-not" the-ambiguous-flow)
```

Please also note that the following two flows are equivalent: `(m/ap :single-value)` and `(m/ap (m/amb :single-value))` — the last value in `ap` is in an implicit single-value `amb`.

Single-value `amb`? 🤔 Can there be a two-value `amb`? It can be, see for yourself:

```clojure id=4208153e-f7b6-4a07-a790-5e3dae73117b
(inspect-flow! "two-values"
  (m/ap
    (m/amb :value1 :value2)))
```

Can we `apply` an `amb` to a collection of values to return multiple values one after another? No, `amb` is a macro. But it's a magic macro, the time has come to witness it.

Let me call what's inside an `ap` block a **scenario**. I am inventing terminology as I go, to help the explanation. It's a scenario for producing the next value of a flow. I will write you now a scenario which blew my mind when I first saw it, let's be very careful here — it's the very meat of `ap`'s magic.

```clojure id=bfefecf2-90ec-45de-8096-c6bb1365b9b7
(def our-collection [:value1 :value2])

(inspect-flow! "values-from-a-collection"
  (m/ap
    (println "Let's start")
    (loop [coll (seq our-collection)]
      (if (nil? coll)
        (m/amb) ;; if we've finished with our collection, produce no more elements
        (m/amb ;; if not:
          (first coll) ;; produce the first element
          (recur (next coll))))))) ;; WTF??
```

**I lied to you,** `amb` doesn't produce values, it **forks** the current scenario into multiple scenarios running one after the other (in the similar sense how [fork()](https://en.wikipedia.org/wiki/Fork_(system_call)) splits a UNIX process into two almost exact copies), each yielding their value into `ap`'s resulting flow. Zero-argument `amb` cancels the current branch without yielding any value.

Might be a lot to take in, but it's very beautiful, let's go step by step. Here's how our initial scenario runs. First, we print "Let's start":

![Screenshot 2023-09-01 at 14.10.55.png][nextjournal#file#4303ebdd-ed16-4270-956a-95b5d20e700e]

Then, we start our loop and initialize our `coll` loop variable:

![Screenshot 2023-09-01 at 14.12.35.png][nextjournal#file#a2c7543c-212b-49cd-881f-99f42f5393d1]

Then we check if our remaining collection is empty:

![Screenshot 2023-09-01 at 14.12.13.png][nextjournal#file#64061ffb-2fd0-410a-bd4c-f23aa1e5491c]

It's not — therefore this two-parameter `amb` runs:

![Screenshot 2023-09-01 at 14.12.59.png][nextjournal#file#7f12eaa6-47d7-4617-bbd1-1a6a57e65a2b]

And now `amb` does its magic — it splits the current scenario into two, running each of the supplied code forms in each of the resulting scenarios:

![Screenshot 2023-09-01 at 14.14.35.png][nextjournal#file#bf47a555-38d8-4d84-aa5d-1835ddee36df]

Then both these scenarios run until completion, in that order, one after another. The first scenario finishes immediately, yielding the first value of our resulting flow:

![Screenshot 2023-09-01 at 14.20.02.png][nextjournal#file#65098ef4-fd76-40ff-9b9a-948885516cbb]

Then the second scenario runs, `recur`ring our `loop` with the remainder of the collection:

![Screenshot 2023-09-01 at 14.20.02.png][nextjournal#file#de28b340-b1fb-46d8-b9a1-586a430db149]

Again we check if the remainder is empty:

![Screenshot 2023-09-01 at 14.20.02.png][nextjournal#file#bdd8f00b-5002-4e5d-9871-0903009ecb71]

It's not empty. We run the else-branch's `amb` again:

![Screenshot 2023-09-01 at 14.20.02.png][nextjournal#file#7122e241-2676-4b12-8c1d-f15a3c50f536]

That `amb` again forks our scenario number two into two scenarios:

![Screenshot 2023-09-01 at 14.34.00.png][nextjournal#file#001ab6e5-c126-403a-bd45-90fe13fb9850]

Our scenario number two finishes with the next value of our collection, and scenario number three recurs with the remainder of the collection:

![Screenshot 2023-09-01 at 14.34.00.png][nextjournal#file#25556e53-4620-4e24-9a63-0a3a323a2a0b]

Which is `nil` in our case, because we've reached the end of our two-element collection:

![Screenshot 2023-09-01 at 14.34.00.png][nextjournal#file#0a3f474c-7b1d-416c-aba0-7bb15f42b24c]

Then our zero-argument `amb` is reached:

![Screenshot 2023-09-01 at 14.34.00.png][nextjournal#file#eb7d56f0-dad6-4a05-a811-85ed02505643]

Zero-argument `amb` kills our third scenario without producing any value into our flow:

![Screenshot 2023-09-01 at 14.34.00.png][nextjournal#file#dd949525-4c8d-4b03-8ae5-1ca33cea9ade]

Mind-bending? Yeah. Is it enough to generate a flow with multiple values in a versatile way? Hell yeah!

Will it also fork `try`/`catch` blocks? Magically, yes:

```clojure id=7fb77584-21c9-4d71-8795-5bff78e881eb
(inspect-flow! "forking-with-exceptions"
  (m/ap
    (try
      (m/amb
        (throw (ex-info "Exception 1" {}))
        (throw (ex-info "Exception 2" {}))
        (throw (ex-info "Exception 3" {})))
      (catch Exception ex
        ;; yield exception's message as our new value
        (ex-message ex)))))
```

Honestly, this have been the trickiest things in flows. If you understand everything so far, everything further is going to be a breeze.

More magic to come! Brother of `amb`, operator `amb=` also does a similar **forking** of scenarios, but all forked scenarios execute **in parallel** (instead of one after another in `amb`'s case), racing to produce a value in `ap`'s resulting flow. See below the comparison of these two brothers:

```clojure id=da1fa825-6049-40f4-8f63-2d5d3abd03ed
(inspect-flow! "amb"
  (m/ap
    (m/amb
      (m/? (m/sleep 400 :delay400))
      (m/? (m/sleep 300 :delay300))
      (m/? (m/sleep 200 :delay200))
      (m/? (m/sleep 100 :delay100)))))

(inspect-flow! "amb="
  (m/ap
    (m/amb=
      (m/? (m/sleep 400 :delay400))
      (m/? (m/sleep 300 :delay300))
      (m/? (m/sleep 200 :delay200))
      (m/? (m/sleep 100 :delay100)))))
```

As you can see, `amb` first completes its first scenario branch that ends with a value, then the second one, etc, until it runs out of scenarios. In contrast, `amb=` starts all the branches at the same time, and values get delivered based on first-come-first-served basis, and the last value in the flow is the slowest one to arrive.

Magic operator `?>` used inside `ap` **forks** the scenario for each new value of a flow it's used on, in the order they arrive, one after another. For example, let's make a flow of numbers that plays them with a built-in time delay between values — out of a collection.

```clojure id=7c103df6-a499-4bb6-a5c4-746d14cdfd8c
(def a-collection-of-numbers [7 6 5 4 3 2 1])
(def delayed-numbers-flow ;; we will be using it in further examples
  (m/ap
    (let [the-number (m/?> (m/seed a-collection-of-numbers))]
      ;; we play our delay first
      (m/? (m/sleep 80))
      ;; only after the delay we produce a number in this scenario
      the-number)))

(inspect-flow! "delayed-numbers" delayed-numbers-flow)
```

You could also do the same thing with `loop`/`recur`/`amb` of course, but `?>` looks "cleaner" for a case when you just want to go over a flow and do something with each value (produce zero, one or more values for each input flow's value with multi-arity `amb`). You could say that `?>` is similar to `clojure.core/for` in this case. Naturally, you could nest multiple `?>`s — just visualize that forking is happening for every new value of a flow.

```clojure id=737911e0-96fd-4a43-b7e5-03e14b96d144
(inspect-flow! "2d-delayed-numbers"
  (m/ap
    (let [y (m/?> delayed-numbers-flow)
          x (m/?> delayed-numbers-flow)]
      (if (and ;; let's only produce values that match our criteria
            (even? (+ x y))
            (not= x y))
        (m/amb [x y])
        (m/amb)))))
```

The `?>` operator also has an optional parameter for parallelism, allowing multiple forked scenarios to run in parallel — it's for the cases when you want to process all elements of an incoming flow, but you care more about splitting work among multiple processors than exact ordering of results (like in `amb=`'s case, the order in `ap`'s resulting flow will be based on which of the parallel scenarios finished first).

```clojure id=b2b33b8f-a6cb-4a91-b9ad-bafe71819b2b
(defn long-processing-task [input]
  (m/sleep
    (rand 400) ;; takes a random amount of time to complete
    (str "⭐️" input "⭐️")))

(inspect-flow! "three-cores-parallel"
  (m/ap
    (let [x (m/?> 3 (m/seed (range 20)))]
      (m/? (long-processing-task x)))))
```

As you can see in the trace above, the ordering of resulting numbers has been compromised a bit, because up to three elements are racing for completion at any moment.

And the last magic piece of `ap` is operator `?<` — just by the looks of it, you can say it's somehow similar to `?>`, you use it the same way: `(m/?< some-flow)`, and the arrow shows, which side (the sender or the receiver of a value) it prioritizes. Our previous operator `?>` prioritizes the reading side (the mount of the "greater than" operator shows the left side is higher priority), which means that we as a reader get values one by one at the speed that we choose, and if we're slow to consume, the flow we read from is waiting for us to retrieve the next value — we pass our backpressure upstream (resulting flow from `ap` is **backpressure-passing**). This new operator `?<` prioritizes the producer side: for every value we read, it forks a new scenario with a new value for us to process — however, whenever a new value is ready to be read from the flow, `?<` **cancels** the current scenario if it hasn't produced a value yet, then forks a new one immediately with the freshest value. So this operator does not "tolerate" slow iteration, it will always try to feed us the freshest value, discarding unfinished work on previous ones. You could say that `?<` operator relieves the backpressure this way (by dropping intermediary values that are too slow to process), so the resulting `ap` flow is **decoupled**.

Let's try them both to feel the difference. Here's again our source flow:

```clojure id=01bad44d-924f-40b5-89f8-572ca2e92a20
;; we have a flow with new numbers coming every 80 ms
(inspect-flow! "original" delayed-numbers-flow)
```

Here's how `?>` deals with long processing for each value:

```clojure id=03df3244-d79c-4215-9ddb-32adee8e22f3
(inspect-flow! "?>"
  (m/ap
    (let [x (m/?> delayed-numbers-flow)]
      (m/? (m/sleep 110)) ;; we do some processing before producing a value
      x)))
```

As you can see above, although original numbers were processed once every 80 ms, our "processing" task takes 110 milliseconds, so in the resulting flow all the numbers are present with 110 ms time delay between them. Source `delayed-numbers-flow` was silently waiting until we read the next number each time.

Here's how `?<` works in the same situation:

```clojure id=3ce2a889-6136-494f-a870-08d8662df24e
(try
  ;;;;;;;;;;;;
  (inspect-flow! "?<"
    (m/ap
      (let [x (m/?< delayed-numbers-flow)]
        (m/? (m/sleep 110)) ;; we do some processing before producing a value
        x)))
  ;;;;;;;;;;;;
  (catch Throwable th
    (println (type th) "-" (ex-message th))))
```

Oh wow, the thing "crashes" with `missionary.Cancelled`. Did I say that `?<` cancels the scenario? I did. Did I say that throwing `missionary.Cancelled` is one of six things a flow could do to indicate that it has been cancelled? I did, but long time ago. Ok, so Missionary gives us an opportunity to handle a situation when cancellation happens. In our case here with our example, we could say: ok, if I can't produce value before a new value arrives, I will drop my old value, and start processing the new one:

```clojure id=cb2c5cd0-d1cb-490c-b057-aee9f83b0e0b
(inspect-flow! "?<"
  (m/ap
    (let [x (m/?< delayed-numbers-flow)]
      (try
        (m/? (m/sleep 110)) ;; we do some processing before producing a value
        x ;; produce our value
        (catch Cancelled _
          ;; we produce no value here if we could finish it in time
          (m/amb))))))
```

As expected, we had time to fully process only the last element in the flow. Processing of all previous elements was interrupted before it could finish.

Just had an idea for a quick experiment. What if we don't `(? (sleep 110))`, but `(Thread/sleep 110)` like a gangsta? We know it's bad to block, but we do what we want in our life.

```clojure id=cdfaa7a4-aa59-4a3e-843a-5ec114b25dfd
(inspect-flow! "?<"
  (m/ap
    (let [x (m/?< delayed-numbers-flow)]
      (try
        (Thread/sleep 110) ;; !!! DON'T DO THIS AT HOME !!!
        x ;; produce our value
        (catch Throwable th
          (tap> (str "Caught " (type th) " with message: " (ex-message th)))
          ;; we produce no value here if we could finish it in time
          (m/amb))))))
```

Léo knew we would be cheeky, and slapped our cheeks. When they say `?<` cancels, it cancels HARD. So probably good to keep in mind that if you do some non-trivial processing after `?<` forks, it's probably a good idea to watch out for JVM thread interruptions if you're not inside a cancelable task.

Also Léo said that in the next version of Missionary behavior of `?<` might change — instead of **cancelling** an old scenario, it will be **ignored**, which would solve potential race conditions and simplify semantics.

## Continuous flows

In this section: `cp` `reductions` `relieve` `signal`

Flows we've worked so far were all discrete. However, very often your program might need continuous flows for operation — for example, Electric Clojure is built on continuous flows (its `new` operator attaches to a continuous flow you provide, all `e/defn`s and` e/fn`s are continuous flows that compose into a beautiful reactive cross-server DAG), so to use your data in Electric you have to construct a continuous flow at some point.

Here's the gist. There are **only two** ways of constructing a continuous flow in Missionary:

1. You can make a `signal` out of a **prepared** discrete flow.
2. You can derive a `cp` (**c**ontinuous **p**rocess) from another continuous or **prepared** discrete flow.

**Preparing** a discrete flow in this case means just two things:

1. Ensuring it's **initialized**  — otherwise our continuous flow would not have a value at the very beginning, but continuous flows should **always** have a value. In the intervals between new values of discrete flow, continuous flow assumes the last update to be the current value, always making it available for reading.
2. Ensuring it's **decoupled** — we don't want backpressure-passing flow as a source, because then our continuous flow would be consuming stale values in vain from its "frozen" upstream backpressure-passing flow instead of freshest current values.

Let's make a continuous flow that yields current UNIX timestamp with seconds-level resolution — an integral number that represents the number of seconds that passed since 00:00 January 1, 1970. First, we make our original discrete flow:

```clojure id=30a5053c-70ed-4c60-a5dd-c474c29fb708
;; the time logic is kinda buggy (might skip a second or emit the same second twice), 
;; but not a big deal for our example, let's not overcomplicate the code
(def discrete-flow-of-unix-timestamps
  (m/ap
    (tap> "Starting our clock...") ;; we'll need this later
    (loop []
      (let [now-ms (System/currentTimeMillis)
            now-s (quot now-ms 1000)
            next-s (inc now-s)
            ms-until-next-s (- (* next-s 1000) now-ms)]
        (m/? (m/sleep ms-until-next-s))
        (m/amb
          next-s
          (recur))))))

(inspect-flow! "discrete" discrete-flow-of-unix-timestamps)
```

Then, we make a continuous flow out of it by 1) supplying an initial value, 2) relieving backpressure on the flow, 3) making a `signal` out of it.

```clojure id=2ce243af-1314-4151-8288-9ad15d110eae
(def prepared-flow-of-unix-timestamps
  (->> discrete-flow-of-unix-timestamps
    (m/reductions {} :unknown) ;; initializing here
    (m/relieve {}))) ;; decoupling here

(def continuous-flow-of-unix-timestamps (m/signal prepared-flow-of-unix-timestamps))

(inspect-flow! "continuous" continuous-flow-of-unix-timestamps)
```

Whoa, a few things to unpack here. First, we notice that conveniently all flow-modifying functions take their input flow as a last parameter, so we can use Clojure's thread last macro `->>` to build transformations step by step, similarly to how we'd do with Clojure's sequences.

The `reductions` function seemingly supplies the initial value `:unknown` to our flow — to make it **initialized**, but why the weird name for that, and what does the empty map `{}` do? What `reductions` actually does is exactly the same what `reduce` does, and takes exactly the same parameters: a **reducing function**, an optional initial value (if you don't supply it, reducing function will be called with zero arguments to supply the value), and an input flow. But instead of returning a task yielding the result of reduction, `reductions` produces a *discrete* *initialized* *backpressure-passing* *flow* which yields each successive step of reduction. So the resulting flow will *immediately yield an initial value*, then a result of the first reduction step from initial value and the first value from the input flow, then a result of the second reduction step with the second value from the input flow, etc.

So to initialize our flow, we use that property of reductions where it will immediately yield an initial value we supply (effectively, making the resulting flow **initialized**) — in our case, a keyword `:unknown`. And we also want to keep all our other values from input stream intact, for this we use a reducing function that just always returns its second argument, like this: `(fn [_ x] x)`, but Missionary and Electric people like to use `{}` for that. If you remember, a map can be called as a function, and it can be called with two arguments: `{}` called with two arguments is equivalent to `#(get {} %1 %2)`, so it always returns the second argument — which is a "missing value" not found in the empty map. In Missionary and Electric, `{}` used this way is called a **discard** function. It's a stylistic choice, rather chic if you ask me. When I use it, I feel like when I stick out my pinky finger drinking my Starbucks venti latte, so it kinda makes my day a bit better, but also it might confuse other people looking at your code, and you might or might not want to achieve that.

Of course, you can use reductions for other element-wise flow transformations, and don't forget that you can use Clojure's `reduced` as a return from the **reducing function** to spontaneously terminate the resulting flow.

```clojure id=1480c33a-8def-464a-be64-7d65c2133dc4
;; this will yield two zeros: first is the sum's initial value generated by (+),
;; the second one is 0 + the first value of our (range) sequence, which is also 0
(inspect-flow! "rolling-sum" (m/reductions + (m/seed (range 5))))

(inspect-flow! "accumulated-maps"
  (m/reductions
    (fn [acc v]
      (let [new-acc (conj acc v)]
        (if (< (count new-acc) 3)
          new-acc
          (reduced new-acc))))
    #{}
    (m/seed [:apple :banana :pear :orange :watermelon :plum])))
```

Ok, back to continuous flows. The `relieve` function relieves the backpressure on our discrete flow by taking care of intermediary values if they come into a flow more often than retrieved. It takes a **combining function** and an input flow to relieve. Please note that **combining function** is **not** a *reducing function*. The differences are:

1. Their "type signatures" are different: a *reducing function* takes an accumulation (think for example: a collection of values) and a new value, and returns an accumulation (for example: a collection that includes the new value); a *combining function* takes an old value and a new value, and returns another value of a similar type.
2. A *reducing function* might be called with zero arguments if you don't supply an initial value, a *combining function* is not called with zero arguments because it is only used to combine two elements.
3. A *reducing function* might return `(reduced last-value)` to terminate the reduction, a *combining function* does not have this trick available.

Thinking about it now, all *combining functions* are *reducing functions*, but not vice versa. But that's not important.

What's important is that `relieve` has to somehow decide, what single value it should hold ready for retrieval when asked for it, and a new value comes into the flow, and relieve calls the **combining function** to decide, what value it should hold from now on given this old value and this new value. In our example case we use the same "venti-latte-pinky discard" `{}` function which just returns the newest value (its second argument) always, so effectively our flow becomes **decoupled** because all intermediary values are discarded, and only the freshest one remains available for retrieval.

But backpressure-relieving logic could be different. For example, we have a flow of purchases made by clients, and we're interested in `println`ing how much money we've just made, but our `println` is actually slow.

```clojure id=d56c9a9f-3674-490f-b018-4a6044373637
(def purchases (m/seed [{:what :shoes :usd 119.95}
                        {:what :shirt :usd 69.95}
                        {:what :pants :usd 99.95}
                        {:what :coat :usd 169.95}
                        {:what :gloves :usd 19.95}
                        {:what :shirt :usd 69.95}
                        {:what :coat :usd 169.95}
                        {:what :sweater :usd 89.95}]))

(def purchases-with-time-delays (m/ap
                                  (let [purchase (m/?> purchases)]
                                    (m/? (m/sleep 300))
                                    purchase)))

(def purchases-with-time-delays-and-no-backpressure
  (->> purchases-with-time-delays
    (m/relieve (fn [prev next]
                 {:what :multiple-items
                  :usd (+ (:usd prev) (:usd next))}))))

(def slow-printer (m/ap
                    (let [purchase (m/?> purchases-with-time-delays-and-no-backpressure)]
                      (tap> purchase)
                      (m/? (m/sleep 1000))))) ;; we're slower to consume than new purchases to come

(m/? (m/reduce (constantly nil) slow-printer))
```

So what does `signal` do? It does two things:

1. It makes our **prepared** input flow **continuous** — the current (last) value of the flow is always available for retrieval.
2. It makes our input flow *memoized* — so all runs of the resulting flow will share the same single running input flow, without spawning it multiple times for each run.

Let's dig in!

First, let's see what'd the difference between our prepared flow and continuous flow:

```clojure id=6d6e543b-9274-4d37-97e8-3a5c9b5db53a
;; let's run the flows in a "raw" way to see if we can retrieve values any time we want
(def running-prepared-flow (prepared-flow-of-unix-timestamps (constantly nil) (constantly nil)))
(def running-continuous-flow (continuous-flow-of-unix-timestamps (constantly nil) (constantly nil)))

(try
  (println "1. Getting a value from our continuous flow:")
  (println "Value:" @running-continuous-flow)
  (println "2. Getting a value from our continuous flow:")
  (println "Value:" @running-continuous-flow)
  (println "3. Getting a value from our continuous flow:")
  (println "Value:" @running-continuous-flow)
  (println "1. Getting a value from our prepared flow:")
  (println "Value:" @running-prepared-flow)
  (println "2. Getting a value from our prepared flow:")
  (println "Value:" @running-prepared-flow)
  (println "3. Getting a value from our prepared flow:")
  (println "Value:" @running-prepared-flow)
  (catch Throwable th (println "Exception thrown!")))
```

See? We didn't get any exceptions here, but consecutive retrievals from a *non-continuous* flow start returning some garbled mess (in the current version of Missionary, it's a sentinel value indicating that we don't have a value yet, an internal implementation detail), while our **continuous** flow behaves very well: it always has a value ready.

Memoization is a useful thing for **continuous** flows because since we are already only interested in the latest value of some measurement or state (for example, current UNIX timestamp from the examples above), it doesn't matter if successive derivation of the current value is done once, or hundred times in parallel — it's going to be the same current value, we don't care about the intermediary values regardless of when we decide to read. So `signal` runs a single instance of its input flow for all flow instances its output runs.

Below we can see the difference. First we run our "prepared" flow three times and retrieve a value from it, then we run our continuous flow and also retrieve a value from it.

```clojure id=a7de1060-b63e-4343-a062-3339e8f779f4
(tap> "Running a flow that has not been m/signal'ed")
(tap> @(prepared-flow-of-unix-timestamps #() #()))
(tap> @(prepared-flow-of-unix-timestamps #() #()))
(tap> @(prepared-flow-of-unix-timestamps #() #()))

(tap> "Running continuous flow returned by m/signal three times")
(tap> @(continuous-flow-of-unix-timestamps #() #()))
(tap> @(continuous-flow-of-unix-timestamps #() #()))
(tap> @(continuous-flow-of-unix-timestamps #() #()))
```

We can see that every time we run a flow that has not been `signal`ed, our code in `ap` block starts afresh (it prints the "Starting our clock..." message), and we only get to see our initial value that we've supplied (since the flow has just started, we haven't emitted any timestamp yet). But our flow that has been returned by `signal` does not even print anything about an `ap` block being started — in fact, it has been started long time ago in another code snippet waaay up, and all instances of subsequent runs of our continuous flow just attach to the same instance of running prepared flow.

![images.png][nextjournal#file#a5315597-df66-421f-8abf-cb44a0c0c62e]

Now, `cp` is a magic block like our good old `sp` and `ap`, but it has some restrictions that only allow it to produce continuous flows.

```clojure id=5f31b3b6-f753-4bdf-b4ca-b41f9e049933
(def ttt (m/cp (let [t (m/?< prepared-flow-of-unix-timestamps)]
                 t)))

(def rttt (ttt #() #()))

@rttt
(try @rttt (catch Exception ex (prn ex)))
```

TODO: ap eager, cp lazy, difference between ap and cp (cp always has value)

```clojure id=e6c0e691-511d-4bab-84c5-3b0f892d8771
#_(def xxx
  (m/cp
    (let [x (m/?< (->> delayed-numbers-flow
                    (m/reductions {} 0)))]
      (try
        ((m/sleep 110) )
        x
        (catch Throwable th
          (tap> (str "Exception: " (type th) " " (ex-message th))))))))

#_(def xxx (m/latest identity (m/watch (atom 5)))) 
(def aaa (atom 5)) 
(def xxx (m/watch aaa))
#_(inspect-flow! "xxx" xxx) 

(reset! aaa 6)
(reset! aaa 6)
(reset! aaa 7)

(def xm (xxx #(tap> "READY") #(tap> "DONE")))
(tap> (str "Value 0: " (deref xm)))
(m/? (m/sleep 150))
(tap> (str "Value 1: " (deref xm)))
(m/? (m/sleep 150))
(tap> (str "Value 2: " (deref xm)))
(m/? (m/sleep 150))
(tap> (str "Value 3: " (deref xm)))
```

## Flowing external data

In this section: `buffer` `eduction` `group-by` `latest` `observe` `sample` `stream` `watch`

## Java interop with Reactive Streams

In this section: `publisher` `subscribe`

# Ports and other things

In this section: `dfv` `mbx` `rdv` `holding` `sem` `!`

[nextjournal#file#21da09e2-8dd6-4adf-8dd6-f6eae86bcd5e]:
<https://nextjournal.com/data/Qmb8U6yeiWAnBnkCt4HZVwdgKAnshvpDRs5JhPpNFWiU1U?content-type=image/gif&node-id=21da09e2-8dd6-4adf-8dd6-f6eae86bcd5e&filename=dags-yda-like-dags-mickey-oniel.gif&node-kind=file> (<p>The first and the last gif in this tutorial</p>)

[nextjournal#file#97358239-413c-42d2-8b00-7f1e54b4450e]:
<https://nextjournal.com/data/QmNYiNctuf6W2PCckyFyAgKnN253KnyjBSb812aX5PMKKT?content-type=image/png&node-id=97358239-413c-42d2-8b00-7f1e54b4450e&filename=Screenshot+2023-08-29+at+11.15.03.png&node-kind=file>

[nextjournal#file#4c08de15-ea8f-4f8a-8c4c-51dadf6b9db4]:
<https://nextjournal.com/data/QmWzSKkb9rLYg79iRvYHGzFd7hHmBe5F8DShnDfjCFHfkv?content-type=image/png&node-id=4c08de15-ea8f-4f8a-8c4c-51dadf6b9db4&filename=surprise.png&node-kind=file>

[nextjournal#file#ca2203ae-5f80-4a56-854b-1da814bbeb34]:
<https://nextjournal.com/data/QmeA6NbQjrn3BFdjXsAvU7uKfoPE5iufT71naNpx4Fiyj6?content-type=image/jpeg&node-id=ca2203ae-5f80-4a56-854b-1da814bbeb34&filename=7xnmpj.jpg&node-kind=file>

[nextjournal#file#65ca7c06-3adc-4baa-a7ec-52ebde274103]:
<https://nextjournal.com/data/QmQLRVpKpYJFx7ncZx6iUxtq986rrsnfZUvTKEqgfEQNWX?content-type=image/png&node-id=65ca7c06-3adc-4baa-a7ec-52ebde274103&filename=Screenshot+2023-09-02+at+12.20.41.png&node-kind=file>

[nextjournal#file#4303ebdd-ed16-4270-956a-95b5d20e700e]:
<https://nextjournal.com/data/QmZAw2HK4VLr9Ktj8tUk4LwEri9hJ4wGdSq1hBoisALVGF?content-type=image/png&node-id=4303ebdd-ed16-4270-956a-95b5d20e700e&filename=Screenshot+2023-09-01+at+14.10.55.png&node-kind=file>

[nextjournal#file#a2c7543c-212b-49cd-881f-99f42f5393d1]:
<https://nextjournal.com/data/QmV6xGTmQTYTKvG8FNTeWo9KPzAy87iuxGQk16ntpYyXw5?content-type=image/png&node-id=a2c7543c-212b-49cd-881f-99f42f5393d1&filename=Screenshot+2023-09-01+at+14.12.35.png&node-kind=file>

[nextjournal#file#64061ffb-2fd0-410a-bd4c-f23aa1e5491c]:
<https://nextjournal.com/data/QmW5aaq52hn68Xz9gyYedAacaxw5yjjdi3UFPqmKfrtreq?content-type=image/png&node-id=64061ffb-2fd0-410a-bd4c-f23aa1e5491c&filename=Screenshot+2023-09-01+at+14.12.13.png&node-kind=file>

[nextjournal#file#7f12eaa6-47d7-4617-bbd1-1a6a57e65a2b]:
<https://nextjournal.com/data/QmSm7t6xm6EV35HHdjnd2RxjzRH5mLWtdmR8ussQ4pCoQg?content-type=image/png&node-id=7f12eaa6-47d7-4617-bbd1-1a6a57e65a2b&filename=Screenshot+2023-09-01+at+14.12.59.png&node-kind=file>

[nextjournal#file#bf47a555-38d8-4d84-aa5d-1835ddee36df]:
<https://nextjournal.com/data/QmSTndhx2Y3F31FT4bAW3nC24LLQo8ShetXwyySGURH7Z9?content-type=image/png&node-id=bf47a555-38d8-4d84-aa5d-1835ddee36df&filename=Screenshot+2023-09-01+at+14.14.35.png&node-kind=file>

[nextjournal#file#65098ef4-fd76-40ff-9b9a-948885516cbb]:
<https://nextjournal.com/data/QmVJk3XAzvzE5PiHCqTNqjuZYKyHKPgP6RNTp2tVBb62sj?content-type=image/png&node-id=65098ef4-fd76-40ff-9b9a-948885516cbb&filename=Screenshot+2023-09-01+at+14.20.02.png&node-kind=file>

[nextjournal#file#de28b340-b1fb-46d8-b9a1-586a430db149]:
<https://nextjournal.com/data/QmTRfUqaLZFHr7CZhEygKabrvRBjuXCVsJvBYZpbh1Tj7H?content-type=image/png&node-id=de28b340-b1fb-46d8-b9a1-586a430db149&filename=Screenshot+2023-09-01+at+14.20.02.png&node-kind=file>

[nextjournal#file#bdd8f00b-5002-4e5d-9871-0903009ecb71]:
<https://nextjournal.com/data/QmRWvGgmD6PSCd6WrnAie2Akp22Kqka1Lc4cfktd67Xea1?content-type=image/png&node-id=bdd8f00b-5002-4e5d-9871-0903009ecb71&filename=Screenshot+2023-09-01+at+14.20.02.png&node-kind=file>

[nextjournal#file#7122e241-2676-4b12-8c1d-f15a3c50f536]:
<https://nextjournal.com/data/QmXbVSYMXJmC7iBw84i34z6Rukxoxn52az1zqFwEuxV29z?content-type=image/png&node-id=7122e241-2676-4b12-8c1d-f15a3c50f536&filename=Screenshot+2023-09-01+at+14.20.02.png&node-kind=file>

[nextjournal#file#001ab6e5-c126-403a-bd45-90fe13fb9850]:
<https://nextjournal.com/data/QmQwNn17JCrFf4K9S41WXqFy4XoU3Tjtz7SrLumfk6Fi8b?content-type=image/png&node-id=001ab6e5-c126-403a-bd45-90fe13fb9850&filename=Screenshot+2023-09-01+at+14.34.00.png&node-kind=file>

[nextjournal#file#25556e53-4620-4e24-9a63-0a3a323a2a0b]:
<https://nextjournal.com/data/QmdSm2ZdbVQC8ZtxxNj44D4LUknT1x5Sar4VzhBaN4QKTt?content-type=image/png&node-id=25556e53-4620-4e24-9a63-0a3a323a2a0b&filename=Screenshot+2023-09-01+at+14.34.00.png&node-kind=file>

[nextjournal#file#0a3f474c-7b1d-416c-aba0-7bb15f42b24c]:
<https://nextjournal.com/data/QmWPMT6Q1HXZ9ESwvRhZuMWB55NiibJko63LnA8Y18p7uN?content-type=image/png&node-id=0a3f474c-7b1d-416c-aba0-7bb15f42b24c&filename=Screenshot+2023-09-01+at+14.34.00.png&node-kind=file>

[nextjournal#file#eb7d56f0-dad6-4a05-a811-85ed02505643]:
<https://nextjournal.com/data/QmbajDAgTyJE12LLq2DxRc3yJsLNe4YMLJDMuqfbMQ3nT3?content-type=image/png&node-id=eb7d56f0-dad6-4a05-a811-85ed02505643&filename=Screenshot+2023-09-01+at+14.34.00.png&node-kind=file>

[nextjournal#file#dd949525-4c8d-4b03-8ae5-1ca33cea9ade]:
<https://nextjournal.com/data/QmacWwusWiR3gy8ASsEard2561QjgNg1AUN5Vv322QZSDa?content-type=image/png&node-id=dd949525-4c8d-4b03-8ae5-1ca33cea9ade&filename=Screenshot+2023-09-01+at+14.34.00.png&node-kind=file>

[nextjournal#file#a5315597-df66-421f-8abf-cb44a0c0c62e]:
<https://nextjournal.com/data/Qmc3mHZn6CT4YvyaiiiZRdqRVo89tC3181MnG1F9FxVwwZ?content-type=image/png&node-id=a5315597-df66-421f-8abf-cb44a0c0c62e&filename=images.png&node-kind=file> (<p>This guide is under construction, further sections are not ready yet</p>)

<details id="com.nextjournal.article">
<summary>This notebook was exported from <a href="https://nextjournal.com/a/RxvEbqiaMAaKkb9KCaq6D?change-id=DjxP9NVo5vVtzuhi5AA29S">https://nextjournal.com/a/RxvEbqiaMAaKkb9KCaq6D?change-id=DjxP9NVo5vVtzuhi5AA29S</a></summary>

```edn nextjournal-metadata
{:article
 {:nodes
  {"001ab6e5-c126-403a-bd45-90fe13fb9850"
   {:id "001ab6e5-c126-403a-bd45-90fe13fb9850", :kind "file"},
   "008c648e-d150-4b40-be13-42015e92642f"
   {:compute-ref #uuid "9869a48b-f4e8-4eda-aca4-861185c01614",
    :exec-duration 1152,
    :id "008c648e-d150-4b40-be13-42015e92642f",
    :kind "code",
    :output-log-lines {},
    :runtime [:runtime "29552ba9-d499-4842-9448-63608ce24e24"]},
   "01bad44d-924f-40b5-89f8-572ca2e92a20"
   {:compute-ref #uuid "981fcaa7-d5c2-4bcf-9489-74c2aeba686c",
    :exec-duration 573,
    :id "01bad44d-924f-40b5-89f8-572ca2e92a20",
    :kind "code",
    :output-log-lines {},
    :runtime [:runtime "29552ba9-d499-4842-9448-63608ce24e24"]},
   "02d45a8f-a689-4f6e-ab7a-a3b3bcbfe4b8"
   {:compute-ref #uuid "4f695d3a-ece6-433e-bf3b-b774798f7242",
    :exec-duration 3438,
    :id "02d45a8f-a689-4f6e-ab7a-a3b3bcbfe4b8",
    :kind "code",
    :output-log-lines {:stdout 2},
    :runtime [:runtime "29552ba9-d499-4842-9448-63608ce24e24"],
    :stdout-collapsed? false},
   "03df3244-d79c-4215-9ddb-32adee8e22f3"
   {:compute-ref #uuid "86a0b381-1c1d-4bf5-9136-1e16fa5484c1",
    :exec-duration 916,
    :id "03df3244-d79c-4215-9ddb-32adee8e22f3",
    :kind "code",
    :output-log-lines {},
    :runtime [:runtime "29552ba9-d499-4842-9448-63608ce24e24"]},
   "049abd04-61b0-4509-9d4d-655668e0c5a6"
   {:compute-ref #uuid "c6be1f10-4d82-4944-846b-09ab7774a954",
    :exec-duration 338,
    :id "049abd04-61b0-4509-9d4d-655668e0c5a6",
    :kind "code",
    :output-log-lines {:stdout 2},
    :runtime [:runtime "29552ba9-d499-4842-9448-63608ce24e24"]},
   "0a3f474c-7b1d-416c-aba0-7bb15f42b24c"
   {:id "0a3f474c-7b1d-416c-aba0-7bb15f42b24c", :kind "file"},
   "0e3cdc8b-b4ee-48ed-be73-c1ade9955f50"
   {:compute-ref #uuid "c199bea9-c3cf-4a7b-bd3e-46537d31ad34",
    :exec-duration 1320,
    :id "0e3cdc8b-b4ee-48ed-be73-c1ade9955f50",
    :kind "code",
    :output-log-lines {},
    :runtime [:runtime "29552ba9-d499-4842-9448-63608ce24e24"]},
   "1480c33a-8def-464a-be64-7d65c2133dc4"
   {:compute-ref #uuid "7fcb1c72-b9bb-40d3-a72f-2245d7db00a2",
    :exec-duration 66,
    :id "1480c33a-8def-464a-be64-7d65c2133dc4",
    :kind "code",
    :output-log-lines {},
    :runtime [:runtime "29552ba9-d499-4842-9448-63608ce24e24"]},
   "1974925d-c3be-42a9-94cc-ec934762f1b8"
   {:compute-ref #uuid "0ac68287-6c3a-4f32-a874-21e3af8d42f6",
    :exec-duration 337,
    :id "1974925d-c3be-42a9-94cc-ec934762f1b8",
    :kind "code",
    :output-log-lines {:stdout 4},
    :runtime [:runtime "29552ba9-d499-4842-9448-63608ce24e24"]},
   "1eca05e0-e534-48a4-abae-90514f262dc5"
   {:compute-ref #uuid "e2d63006-b5e5-4ea6-8843-017959502a1d",
    :exec-duration 146,
    :id "1eca05e0-e534-48a4-abae-90514f262dc5",
    :kind "code",
    :output-log-lines {},
    :runtime [:runtime "29552ba9-d499-4842-9448-63608ce24e24"]},
   "21da09e2-8dd6-4adf-8dd6-f6eae86bcd5e"
   {:id "21da09e2-8dd6-4adf-8dd6-f6eae86bcd5e", :kind "file"},
   "245574fa-03c0-450e-8153-c1054d9ad756"
   {:compute-ref #uuid "64a2aed2-034a-4241-a1c2-73bd999b13e5",
    :exec-duration 15,
    :id "245574fa-03c0-450e-8153-c1054d9ad756",
    :kind "code",
    :output-log-lines {},
    :runtime [:runtime "29552ba9-d499-4842-9448-63608ce24e24"]},
   "246d97a7-3b24-4426-893f-7dd71a6a33fb"
   {:compute-ref #uuid "c7d984ff-d305-4984-bcf1-31a13cbb6f02",
    :exec-duration 94,
    :id "246d97a7-3b24-4426-893f-7dd71a6a33fb",
    :kind "code",
    :output-log-lines {},
    :runtime [:runtime "29552ba9-d499-4842-9448-63608ce24e24"]},
   "2547db1f-e39a-490f-b80c-38a7ac8745f7"
   {:compute-ref #uuid "ea53be2e-b4fe-43b6-93a3-e46192fbbdf9",
    :exec-duration 302,
    :id "2547db1f-e39a-490f-b80c-38a7ac8745f7",
    :kind "code",
    :output-log-lines {:stdout 5},
    :runtime [:runtime "29552ba9-d499-4842-9448-63608ce24e24"]},
   "25556e53-4620-4e24-9a63-0a3a323a2a0b"
   {:id "25556e53-4620-4e24-9a63-0a3a323a2a0b", :kind "file"},
   "29552ba9-d499-4842-9448-63608ce24e24"
   {:environment
    [:environment
     {:article/nextjournal.id
      #uuid "5b45eb52-bad4-413d-9d7f-b2b573a25322",
      :change/nextjournal.id
      #uuid "6140750b-27b0-4b4d-86f3-b07682cd65c6",
      :node/id "0ae15688-6f6a-40e2-a4fa-52d81371f733"}],
    :id "29552ba9-d499-4842-9448-63608ce24e24",
    :kind "runtime",
    :language "clojure",
    :type :prepl,
    :runtime/mounts
    [{:src [:node "ffcf0396-b3f9-40e6-a0c2-654401879781"],
      :dest "/deps.edn"}]},
   "2a07f446-303a-4b0a-9cf7-ff7424077bc2"
   {:compute-ref #uuid "343d13ee-a033-41ee-8883-e18e92adf0df",
    :exec-duration 10953,
    :id "2a07f446-303a-4b0a-9cf7-ff7424077bc2",
    :kind "code",
    :output-log-lines {:stdout 2},
    :runtime [:runtime "29552ba9-d499-4842-9448-63608ce24e24"]},
   "2ce243af-1314-4151-8288-9ad15d110eae"
   {:compute-ref #uuid "efb2100a-9865-4c69-b68e-0a8d0540df62",
    :exec-duration 5024,
    :id "2ce243af-1314-4151-8288-9ad15d110eae",
    :kind "code",
    :output-log-lines {},
    :runtime [:runtime "29552ba9-d499-4842-9448-63608ce24e24"]},
   "30a5053c-70ed-4c60-a5dd-c474c29fb708"
   {:compute-ref #uuid "b2c0de39-10cb-4484-b36d-d3ab287ae6af",
    :exec-duration 5135,
    :id "30a5053c-70ed-4c60-a5dd-c474c29fb708",
    :kind "code",
    :output-log-lines {},
    :runtime [:runtime "29552ba9-d499-4842-9448-63608ce24e24"]},
   "3120d88c-0475-43c9-b550-de98541a873d"
   {:compute-ref #uuid "2f79bb4a-65dc-40bf-b821-bee26fca83fc",
    :exec-duration 1300,
    :id "3120d88c-0475-43c9-b550-de98541a873d",
    :kind "code",
    :output-log-lines {:stdout 2},
    :runtime [:runtime "29552ba9-d499-4842-9448-63608ce24e24"]},
   "3ce2a889-6136-494f-a870-08d8662df24e"
   {:compute-ref #uuid "9e790374-fea1-4f60-a939-b972468b29c9",
    :exec-duration 514,
    :id "3ce2a889-6136-494f-a870-08d8662df24e",
    :kind "code",
    :output-log-lines {:stdout 2},
    :runtime [:runtime "29552ba9-d499-4842-9448-63608ce24e24"]},
   "40b2e226-8642-4251-aa66-de3dca1ae735"
   {:compute-ref #uuid "fd201383-a407-44b5-a3a1-bc09ccf668dd",
    :exec-duration 710,
    :id "40b2e226-8642-4251-aa66-de3dca1ae735",
    :kind "code",
    :output-log-lines {:stdout 2},
    :runtime [:runtime "29552ba9-d499-4842-9448-63608ce24e24"]},
   "40ee2698-7d25-45b3-bd1b-33d9fa87a327"
   {:compute-ref #uuid "a3844baf-f91f-4706-8695-6977621ead44",
    :exec-duration 365,
    :id "40ee2698-7d25-45b3-bd1b-33d9fa87a327",
    :kind "code",
    :output-log-lines {:stdout 2},
    :runtime [:runtime "29552ba9-d499-4842-9448-63608ce24e24"]},
   "4208153e-f7b6-4a07-a790-5e3dae73117b"
   {:compute-ref #uuid "66a4aa27-5e5f-4fb1-b064-b8f0bf5f4897",
    :exec-duration 119,
    :id "4208153e-f7b6-4a07-a790-5e3dae73117b",
    :kind "code",
    :output-log-lines {},
    :runtime [:runtime "29552ba9-d499-4842-9448-63608ce24e24"]},
   "4303ebdd-ed16-4270-956a-95b5d20e700e"
   {:id "4303ebdd-ed16-4270-956a-95b5d20e700e", :kind "file"},
   "4a03b99d-e254-45fc-aee7-e7beff5b0d11"
   {:compute-ref #uuid "f6dd0a66-ce75-4aed-a28a-954401e719f4",
    :exec-duration 129,
    :id "4a03b99d-e254-45fc-aee7-e7beff5b0d11",
    :kind "code",
    :output-log-lines {},
    :runtime [:runtime "29552ba9-d499-4842-9448-63608ce24e24"]},
   "4bc08949-b0a3-4bc8-a2d2-5fa851aa2e8f"
   {:compute-ref #uuid "13a8919a-f8a7-4c6c-85f1-84e0094b02f2",
    :exec-duration 227,
    :id "4bc08949-b0a3-4bc8-a2d2-5fa851aa2e8f",
    :kind "code",
    :output-log-lines {:stdout 2},
    :runtime [:runtime "29552ba9-d499-4842-9448-63608ce24e24"]},
   "4c08de15-ea8f-4f8a-8c4c-51dadf6b9db4"
   {:id "4c08de15-ea8f-4f8a-8c4c-51dadf6b9db4", :kind "file"},
   "54ff7c44-fc6a-427e-a301-7faca9ee3300"
   {:compute-ref #uuid "c58d329f-25ea-47f5-af0a-c2fce3164c10",
    :exec-duration 335,
    :id "54ff7c44-fc6a-427e-a301-7faca9ee3300",
    :kind "code",
    :output-log-lines {:stdout 11},
    :runtime [:runtime "29552ba9-d499-4842-9448-63608ce24e24"]},
   "562b6433-df7a-4370-9754-6b95d85e35e9"
   {:compute-ref #uuid "e736ae7d-672d-4dd5-9dc2-1b217dd9960f",
    :exec-duration 4360,
    :id "562b6433-df7a-4370-9754-6b95d85e35e9",
    :kind "code",
    :output-log-lines {:stdout 7},
    :runtime [:runtime "29552ba9-d499-4842-9448-63608ce24e24"]},
   "5c38aa89-1d21-4e20-b1a4-c7e18336bdba"
   {:compute-ref #uuid "c3a41a7a-a615-441f-a20a-501a494b5be1",
    :exec-duration 780,
    :id "5c38aa89-1d21-4e20-b1a4-c7e18336bdba",
    :kind "code",
    :output-log-lines {:stdout 2},
    :runtime [:runtime "29552ba9-d499-4842-9448-63608ce24e24"]},
   "5d31a0e2-9464-4ec0-a684-5fc9b4c0cd2e"
   {:compute-ref #uuid "b3c13471-3400-4fc6-93c4-6e4ed705122e",
    :exec-duration 579,
    :id "5d31a0e2-9464-4ec0-a684-5fc9b4c0cd2e",
    :kind "code",
    :output-log-lines {:stdout 2},
    :runtime [:runtime "29552ba9-d499-4842-9448-63608ce24e24"]},
   "5f31b3b6-f753-4bdf-b4ca-b41f9e049933"
   {:compute-ref #uuid "5deb143c-f6b2-408d-aed2-1390f97d6500",
    :exec-duration 340,
    :id "5f31b3b6-f753-4bdf-b4ca-b41f9e049933",
    :kind "code",
    :output-log-lines {:stdout 35},
    :runtime [:runtime "29552ba9-d499-4842-9448-63608ce24e24"]},
   "60e0a035-216d-46ce-a1dc-c358ca77227a"
   {:compute-ref #uuid "9594e448-446f-424b-a70d-99652a904321",
    :exec-duration 332,
    :id "60e0a035-216d-46ce-a1dc-c358ca77227a",
    :kind "code",
    :output-log-lines {:stdout 2},
    :runtime [:runtime "29552ba9-d499-4842-9448-63608ce24e24"]},
   "629c904b-b88c-4a57-827d-0ff9a29fd6ee"
   {:compute-ref #uuid "d3996a54-08b1-4069-a9de-4b1d42f9c986",
    :exec-duration 631,
    :id "629c904b-b88c-4a57-827d-0ff9a29fd6ee",
    :kind "code",
    :output-log-lines {:stdout 2},
    :runtime [:runtime "29552ba9-d499-4842-9448-63608ce24e24"]},
   "62f625f5-946b-424b-879c-f077f00aa76c"
   {:compute-ref #uuid "25693bc2-4089-43a6-8e30-11fa9f636d2a",
    :exec-duration 94,
    :id "62f625f5-946b-424b-879c-f077f00aa76c",
    :kind "code",
    :output-log-lines {},
    :runtime [:runtime "29552ba9-d499-4842-9448-63608ce24e24"]},
   "64061ffb-2fd0-410a-bd4c-f23aa1e5491c"
   {:id "64061ffb-2fd0-410a-bd4c-f23aa1e5491c", :kind "file"},
   "65098ef4-fd76-40ff-9b9a-948885516cbb"
   {:id "65098ef4-fd76-40ff-9b9a-948885516cbb", :kind "file"},
   "65ca7c06-3adc-4baa-a7ec-52ebde274103"
   {:id "65ca7c06-3adc-4baa-a7ec-52ebde274103", :kind "file"},
   "69ca744a-9d7c-427b-a4ea-edf6fde17d92"
   {:compute-ref #uuid "d84a65dc-9858-4f1c-9b83-ebd3578da220",
    :exec-duration 338,
    :id "69ca744a-9d7c-427b-a4ea-edf6fde17d92",
    :kind "code",
    :output-log-lines {:stdout 2},
    :runtime [:runtime "29552ba9-d499-4842-9448-63608ce24e24"]},
   "6d6e543b-9274-4d37-97e8-3a5c9b5db53a"
   {:compute-ref #uuid "bfe402a4-fec1-444e-b27d-88e02ab8d879",
    :exec-duration 275,
    :id "6d6e543b-9274-4d37-97e8-3a5c9b5db53a",
    :kind "code",
    :output-log-lines {:stdout 13},
    :runtime [:runtime "29552ba9-d499-4842-9448-63608ce24e24"]},
   "70077d97-d6c2-41b9-919c-08e057c1ec64"
   {:compute-ref #uuid "86b34a72-225e-4d39-b805-be04998fd664",
    :exec-duration 76,
    :id "70077d97-d6c2-41b9-919c-08e057c1ec64",
    :kind "code",
    :output-log-lines {},
    :runtime [:runtime "29552ba9-d499-4842-9448-63608ce24e24"]},
   "7122e241-2676-4b12-8c1d-f15a3c50f536"
   {:id "7122e241-2676-4b12-8c1d-f15a3c50f536", :kind "file"},
   "728b5c13-aaa5-482c-896e-0946ba366720"
   {:compute-ref #uuid "3d39f18d-b648-4b3a-9584-aba5e242e34c",
    :exec-duration 253,
    :id "728b5c13-aaa5-482c-896e-0946ba366720",
    :kind "code",
    :output-log-lines {:stdout 2},
    :runtime [:runtime "29552ba9-d499-4842-9448-63608ce24e24"]},
   "737911e0-96fd-4a43-b7e5-03e14b96d144"
   {:compute-ref #uuid "0f5ebc63-6232-4609-97af-25d2319ca5a4",
    :exec-duration 2582,
    :id "737911e0-96fd-4a43-b7e5-03e14b96d144",
    :kind "code",
    :output-log-lines {},
    :runtime [:runtime "29552ba9-d499-4842-9448-63608ce24e24"]},
   "7c103df6-a499-4bb6-a5c4-746d14cdfd8c"
   {:compute-ref #uuid "8b5e3190-3f68-43f0-b532-95160a0bf259",
    :exec-duration 623,
    :id "7c103df6-a499-4bb6-a5c4-746d14cdfd8c",
    :kind "code",
    :output-log-lines {},
    :runtime [:runtime "29552ba9-d499-4842-9448-63608ce24e24"]},
   "7f12eaa6-47d7-4617-bbd1-1a6a57e65a2b"
   {:id "7f12eaa6-47d7-4617-bbd1-1a6a57e65a2b", :kind "file"},
   "7fb77584-21c9-4d71-8795-5bff78e881eb"
   {:compute-ref #uuid "ff61828c-626f-4058-b00f-22357ed1b061",
    :exec-duration 203,
    :id "7fb77584-21c9-4d71-8795-5bff78e881eb",
    :kind "code",
    :output-log-lines {},
    :runtime [:runtime "29552ba9-d499-4842-9448-63608ce24e24"]},
   "84e91f7e-057d-4a31-8312-539443f1ef15"
   {:compute-ref #uuid "269ac47a-ded2-4346-8ba8-3775d65fb431",
    :exec-duration 476,
    :id "84e91f7e-057d-4a31-8312-539443f1ef15",
    :kind "code",
    :output-log-lines {:stdout 2},
    :runtime [:runtime "29552ba9-d499-4842-9448-63608ce24e24"]},
   "86c5324d-671c-457d-a593-528049860887"
   {:compute-ref #uuid "2e49ca03-fc6e-48fb-b5e3-d93c003e58e9",
    :exec-duration 3247,
    :id "86c5324d-671c-457d-a593-528049860887",
    :kind "code",
    :output-log-lines {:stdout 4},
    :runtime [:runtime "29552ba9-d499-4842-9448-63608ce24e24"]},
   "97358239-413c-42d2-8b00-7f1e54b4450e"
   {:id "97358239-413c-42d2-8b00-7f1e54b4450e", :kind "file"},
   "9e51ca2b-cdae-40b4-b9e5-330df30df56c"
   {:compute-ref #uuid "232a24ad-1eaf-4616-9983-827c1a38af8d",
    :exec-duration 7551,
    :id "9e51ca2b-cdae-40b4-b9e5-330df30df56c",
    :kind "code",
    :output-log-lines {:stdout 2},
    :runtime [:runtime "29552ba9-d499-4842-9448-63608ce24e24"]},
   "a2c7543c-212b-49cd-881f-99f42f5393d1"
   {:id "a2c7543c-212b-49cd-881f-99f42f5393d1", :kind "file"},
   "a5315597-df66-421f-8abf-cb44a0c0c62e"
   {:id "a5315597-df66-421f-8abf-cb44a0c0c62e", :kind "file"},
   "a7de1060-b63e-4343-a062-3339e8f779f4"
   {:compute-ref #uuid "a043893a-eb7d-4544-942d-66b6a9feec4a",
    :exec-duration 78,
    :id "a7de1060-b63e-4343-a062-3339e8f779f4",
    :kind "code",
    :output-log-lines {},
    :runtime [:runtime "29552ba9-d499-4842-9448-63608ce24e24"]},
   "acd0619c-0653-4a37-9fe0-b0e435ab4fce"
   {:id "acd0619c-0653-4a37-9fe0-b0e435ab4fce", :kind "code-listing"},
   "af2086f2-0d9e-4cfd-ac55-fc0346892c80"
   {:compute-ref #uuid "840473e5-7120-40a8-8f80-84e58178ff12",
    :exec-duration 265,
    :id "af2086f2-0d9e-4cfd-ac55-fc0346892c80",
    :kind "code",
    :output-log-lines {:stdout 2},
    :runtime [:runtime "29552ba9-d499-4842-9448-63608ce24e24"]},
   "b2b33b8f-a6cb-4a91-b9ad-bafe71819b2b"
   {:compute-ref #uuid "d7a37436-9fe7-423c-9618-4a9aa8c8e9be",
    :exec-duration 900,
    :id "b2b33b8f-a6cb-4a91-b9ad-bafe71819b2b",
    :kind "code",
    :output-log-lines {},
    :runtime [:runtime "29552ba9-d499-4842-9448-63608ce24e24"]},
   "ba09c65c-4b44-4611-b8e7-78c942f8a111"
   {:compute-ref #uuid "7b7f5a16-d5bd-4b6b-9274-91846db98a89",
    :exec-duration 571,
    :id "ba09c65c-4b44-4611-b8e7-78c942f8a111",
    :kind "code",
    :output-log-lines {:stdout 2},
    :runtime [:runtime "29552ba9-d499-4842-9448-63608ce24e24"]},
   "bcae4267-d0d7-48a2-97eb-f1eab871687a"
   {:compute-ref #uuid "ab757b75-2068-4872-8018-187c43d7538d",
    :exec-duration 336,
    :id "bcae4267-d0d7-48a2-97eb-f1eab871687a",
    :kind "code",
    :output-log-lines {:stdout 2},
    :runtime [:runtime "29552ba9-d499-4842-9448-63608ce24e24"]},
   "bdd8f00b-5002-4e5d-9871-0903009ecb71"
   {:id "bdd8f00b-5002-4e5d-9871-0903009ecb71", :kind "file"},
   "bf47a555-38d8-4d84-aa5d-1835ddee36df"
   {:id "bf47a555-38d8-4d84-aa5d-1835ddee36df", :kind "file"},
   "bfefecf2-90ec-45de-8096-c6bb1365b9b7"
   {:compute-ref #uuid "cdc6fbf5-d7f6-440b-a540-29babd2c2934",
    :exec-duration 509,
    :id "bfefecf2-90ec-45de-8096-c6bb1365b9b7",
    :kind "code",
    :output-log-lines {:stdout 2},
    :runtime [:runtime "29552ba9-d499-4842-9448-63608ce24e24"]},
   "ca2203ae-5f80-4a56-854b-1da814bbeb34"
   {:id "ca2203ae-5f80-4a56-854b-1da814bbeb34", :kind "file"},
   "cb2c5cd0-d1cb-490c-b057-aee9f83b0e0b"
   {:compute-ref #uuid "884da1eb-e443-4ace-8afe-117f520555ad",
    :exec-duration 766,
    :id "cb2c5cd0-d1cb-490c-b057-aee9f83b0e0b",
    :kind "code",
    :output-log-lines {},
    :runtime [:runtime "29552ba9-d499-4842-9448-63608ce24e24"]},
   "cdfaa7a4-aa59-4a3e-843a-5ec114b25dfd"
   {:compute-ref #uuid "d3f57cd1-f352-47b7-b6b2-b5a8e10703c2",
    :exec-duration 781,
    :id "cdfaa7a4-aa59-4a3e-843a-5ec114b25dfd",
    :kind "code",
    :output-log-lines {},
    :runtime [:runtime "29552ba9-d499-4842-9448-63608ce24e24"]},
   "d0320276-44c3-4b07-b49f-aa210f1d2f93"
   {:compute-ref #uuid "76fb5e58-6cb8-475f-b02f-1b759fa9d42d",
    :exec-duration 333,
    :id "d0320276-44c3-4b07-b49f-aa210f1d2f93",
    :kind "code",
    :output-log-lines {:stdout 2},
    :runtime [:runtime "29552ba9-d499-4842-9448-63608ce24e24"]},
   "d31f2704-b3f1-466a-86db-dabe3061ac33"
   {:compute-ref #uuid "33d669aa-85f7-4fe5-a82a-1096359c58bf",
    :exec-duration 2316,
    :id "d31f2704-b3f1-466a-86db-dabe3061ac33",
    :kind "code",
    :output-log-lines {:stdout 2},
    :runtime [:runtime "29552ba9-d499-4842-9448-63608ce24e24"]},
   "d56c9a9f-3674-490f-b018-4a6044373637"
   {:compute-ref #uuid "3981141d-874a-4b0a-a20e-dfbcc0d88566",
    :exec-duration 4454,
    :id "d56c9a9f-3674-490f-b018-4a6044373637",
    :kind "code",
    :output-log-lines {},
    :runtime [:runtime "29552ba9-d499-4842-9448-63608ce24e24"]},
   "d5ead80f-27df-400e-aaa4-e7c58e7b0082"
   {:compute-ref #uuid "29b4da1e-8391-4170-83be-a0e65f665109",
    :exec-duration 43,
    :id "d5ead80f-27df-400e-aaa4-e7c58e7b0082",
    :kind "code",
    :output-log-lines {},
    :runtime [:runtime "29552ba9-d499-4842-9448-63608ce24e24"]},
   "da1fa825-6049-40f4-8f63-2d5d3abd03ed"
   {:compute-ref #uuid "7212fc0b-fe16-43df-a001-0089326ab31b",
    :exec-duration 1661,
    :id "da1fa825-6049-40f4-8f63-2d5d3abd03ed",
    :kind "code",
    :output-log-lines {},
    :runtime [:runtime "29552ba9-d499-4842-9448-63608ce24e24"]},
   "dd949525-4c8d-4b03-8ae5-1ca33cea9ade"
   {:id "dd949525-4c8d-4b03-8ae5-1ca33cea9ade", :kind "file"},
   "de28b340-b1fb-46d8-b9a1-586a430db149"
   {:id "de28b340-b1fb-46d8-b9a1-586a430db149", :kind "file"},
   "e5e53beb-53ea-47d0-baa6-bb2bc4577dfc"
   {:compute-ref #uuid "3113ae5c-425a-4627-a391-6eb7340efc79",
    :exec-duration 6243,
    :id "e5e53beb-53ea-47d0-baa6-bb2bc4577dfc",
    :kind "code",
    :output-log-lines {:stdout 2},
    :runtime [:runtime "29552ba9-d499-4842-9448-63608ce24e24"]},
   "e6c0e691-511d-4bab-84c5-3b0f892d8771"
   {:compute-ref #uuid "0b52d2dc-52bd-458e-928e-76540e3942f2",
    :exec-duration 489,
    :id "e6c0e691-511d-4bab-84c5-3b0f892d8771",
    :kind "code",
    :output-log-lines {},
    :runtime [:runtime "29552ba9-d499-4842-9448-63608ce24e24"]},
   "eb7d56f0-dad6-4a05-a811-85ed02505643"
   {:id "eb7d56f0-dad6-4a05-a811-85ed02505643", :kind "file"},
   "f0842eb0-30df-44d2-82aa-a22e17f70bd7"
   {:compute-ref #uuid "7677eba7-e3f6-4bd5-91b4-7e1c07384a51",
    :exec-duration 266,
    :id "f0842eb0-30df-44d2-82aa-a22e17f70bd7",
    :kind "code",
    :output-log-lines {:stdout 2},
    :runtime [:runtime "29552ba9-d499-4842-9448-63608ce24e24"]},
   "ffcf0396-b3f9-40e6-a0c2-654401879781"
   {:id "ffcf0396-b3f9-40e6-a0c2-654401879781",
    :kind "code-listing",
    :name "deps.edn"}},
  :nextjournal/id #uuid "037c4fa2-57d3-46a6-b2fc-73e2c3f70e06",
  :article/change
  {:nextjournal/id #uuid "672d0852-4f45-42a4-84a7-ba0d6741c4c5"}}}

```
</details>
