(ns duct.component.handler
  (:require [com.stuartsierra.component :as component]
            [compojure.core :as compojure]))

(defn- find-routes [component]
  (keep :routes (vals component)))

(defn- middleware-fn [component middleware]
  (if (vector? middleware)
    (let [[f & keys] middleware
          arguments  (map #(get component %) keys)]
      #(apply f % arguments))
    middleware))

(defn- compose-middleware [{:keys [middleware] :as component}]
  (->> (reverse middleware)
       (map #(middleware-fn component %))
       (apply comp identity)))

(defrecord Handler [defaults middleware]
  component/Lifecycle
  (start [component]
    (if-not (:handler component)
      (let [routes  (find-routes component)
            wrap-mw (compose-middleware component)
            handler (wrap-mw (apply compojure/routes routes))]
        (assoc component :handler handler))
      component))
  (stop [component]
    (dissoc component :handler)))

(defn handler-component [options]
  (map->Handler options))
