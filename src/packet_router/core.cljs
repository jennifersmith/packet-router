(ns packet-router.core)

(enable-console-print!)

(defn make-scene [name init-fn uninit-fn]
  (.scene js/Crafty name init-fn uninit-fn))

(defn switch-to-scene [name]
  (.enterScene js/Crafty name))

(defn make-scene-with-transition [name init-fn next-scene]
  (js/makescenewithtransition name init-fn next-scene))

(defn make-scene-with-transition-rubbish [name init-fn next-scene]
  (let [arrgh (atom 0)]
    (make-scene
     name
     (fn []
       (this-as foo
                (prn foo)
        (init-fn)
        (swap! arrgh
               (fn [_] (.bind foo "KeyDown" #(switch-to-scene next-scene)))))
       (println @arrgh))
     (fn []
       (@arrgh)))))


(defn loading-scene []
  (println "loading")
  (.textFont 
   (.text
    (.e js/Crafty "2D, DOM, Text") "Welcome to Packet Router.... a game created for Connected Worlds theme Ludum Dare 36") 
   (clj->js {"size" "24px"})))

(defn game-scene []
(println "game")
  (.textFont 
   (.text
    (.e js/Crafty "2D, DOM, Text") "You are playing my totes awesome game") 
   (clj->js {"size" "24px"})))
 
(defn finish-scene []
  (println "finish")
  (.textFont 
   (.text
    (.e js/Crafty "2D, DOM, Text") "YOU DIED! But you got a good score like.") 
   (clj->js {"size" "24px"})))


(make-scene-with-transition "Intro" loading-scene "Game")
(make-scene-with-transition "Game" game-scene "Finish")
(make-scene-with-transition "Finish" finish-scene "Intro")

(defn start-game []
  (.init js/Crafty 480 320)
  (.background js/Crafty "rgb(87, 109, 20)")
  (switch-to-scene "Intro"))

(.addEventListener js/window "load" start-game)
