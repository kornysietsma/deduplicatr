(ns deduplicatr.throttler)

(defn now [] (java.lang.System/currentTimeMillis))

(defn new-agent "make an agent for intermittent running"
  [interval]
  (agent {:next-time (- (now) 1)
          :interval interval}))

(defn- update-agent-and-maybe-run
  [run-fn {:keys [next-time interval] :as orig-state}]
  (let [the-time (now)]
    (if (> the-time next-time)
      (do
        (run-fn)
        {:next-time (+ the-time interval) :interval interval})
      orig-state)))

(defn run
  "runs the specified function if enough time has passed"
  [the-agent run-fn]
  (send the-agent (partial update-agent-and-maybe-run run-fn)))
