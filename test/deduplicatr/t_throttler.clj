(ns deduplicatr.t-throttler
  (:require [deduplicatr.throttler :as throttler]
            [midje.sweet :refer :all]))

(def thing (atom :unused))

(fact "the first time you run a throttled function it runs"
      (let [a (throttler/new-agent 100)
            _ (reset! thing :unset)
            _ (throttler/run a #(reset! thing :set))
            _ (Thread/sleep 5)]
         @thing => :set))

(fact "the second time you run a throttled function it doesn't run if not enough time has passed"
      (let [a (throttler/new-agent 100)
            _ (reset! thing :unset)
            _ (throttler/run a #(reset! thing :set))
            _ (throttler/run a #(reset! thing :too-soon))
            _ (Thread/sleep 5)]
         @thing => :set))

(fact "once enough time has passed functions will run again"
      (let [a (throttler/new-agent 10)
            _ (reset! thing :unset)
            _ (throttler/run a #(reset! thing :set))
            _ (throttler/run a #(reset! thing :ignored))
            _ (Thread/sleep 15)
            _ (throttler/run a #(reset! thing :final))
            _ (Thread/sleep 5)
            ]
         @thing => :final))
