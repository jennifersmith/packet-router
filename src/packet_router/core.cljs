(ns packet-router.core)

(enable-console-print!)

(defn dump-statebag [msg & kvs]
  (println msg (apply hash-map kvs)))

(defn make-component [name obj]
  (.c js/Crafty name obj))

(defn make-scene [name init-fn uninit-fn]
  (.scene js/Crafty name init-fn uninit-fn))

(defn switch-to-scene [name]
  (.enterScene js/Crafty name))

(defn make-scene-with-transition [name init-fn next-scene]
  (js/makescenewithtransition name init-fn next-scene))

(defn loading-scene []
  (.textFont 
   (.text
    (.e js/Crafty "2D, DOM, Text") "Welcome to Packet Router.... a game created for Connected Worlds theme Ludum Dare 36") 
   (clj->js {"size" "24px"})))

;; So I am REALLY not using clojure correctly. I don't care. I can see a way and it still
;; feels easier to me to be in fam language. Justifications :)
(defn position-port [e loc]
  (.setPortLoc e loc))

(defn game-scene []
  (.e js/Crafty "Router")
;;  (position-port (.e js/Crafty "Port") :north)
;;  (position-port (.e js/Crafty "Port") :east)
  (position-port (.e js/Crafty "Port") :west)
  (position-port (.e js/Crafty "Port") :east))
 
(defn finish-scene []
  (.textFont 
   (.text
    (.e js/Crafty "2D, DOM, Text") "YOU DIED! But you got a good score like.") 
   (clj->js {"size" "24px"})))


(def width 480)
(def height 320)
(def router-padding 50)

(def router-width (- width (* 2 router-padding)))
(def router-height (- height (* 2 router-padding)))

(def port-width 40)

(defn init-port []
  (this-as me
           (.requires me "2D, Canvas, Color, Polygon")
           (.color me "rgb(50, 0, 50)")))

(def loc->position
  {:west
   {"x" 30 
    "y" 120 }
   :east
   {"x" (- width 20 router-padding) 
    "y" 120}})

(defn set-port-loc [loc]
  (let [
        coords (merge {:w 40 :h 80} (loc->position loc))]
    (println "Setting port location" :loc loc :coords coords)
    (this-as me
             (.attr me (clj->js coords)))))

(defn init-router-component [] 
  (this-as me
           (.requires me "2D, Canvas, Color, Polygon")
           (.color me "rgb(20, 125, 40)")
           (.attr me (clj->js {"x" router-padding 
                               "y" router-padding 
                               "w" router-width
                               "h" router-height}))))



(make-component "Router" (clj->js {:init init-router-component
                                   }) )
(make-component "Port" (clj->js {:init init-port
                                 :setPortLoc set-port-loc
                                   }) )

(make-scene-with-transition "Intro" loading-scene "Game")
(make-scene-with-transition "Game" game-scene "Finish")
(make-scene-with-transition "Finish" finish-scene "Intro")

(defn start-game []
  (.init js/Crafty 480 320)
  (.background js/Crafty "rgb(87, 109, 20)")
  (switch-to-scene "Intro"))

(.addEventListener js/window "load" start-game)
