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

(defn make-entity [name]
  (.e js/Crafty name))
;; So I am REALLY not using clojure correctly. I don't care. I can see a way and it still
;; feels easier to me to be in fam language. Justifications :)
(defn position-port [e loc]
  (.setPortLoc e loc) e)

(defn entity-count [name]
  (.-length (js/Crafty name)))

(defn packet-created [game-scene]
  (when (> (entity-count "Packet") 15)
    (println "TOO MANY PACKETS!")
    (.scene js/Crafty "Finish")))


(defn game-scene []
  (this-as me
           (.e js/Crafty "Router")
           (let [ports [(position-port (.e js/Crafty "Port") :north)
                        (position-port (.e js/Crafty "Port") :east)
                        (position-port (.e js/Crafty "Port") :west)
                        (position-port (.e js/Crafty "Port") :south)
                        unbind (.bind js/Crafty "PacketCreated" #(packet-created me))]]
             (set! (.-unbind-my-events me) unbind)
             (.activatePort (first ports)))))

(defn game-scene-uninit []
  (this-as me
           (if-let [unbind (.-unbind-my-events me)]
             (unbind))))
 
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
           (.requires me "Delay, 2D, Canvas, Color, Polygon")
           (.color me "rgb(50, 0, 50)")))

(def loc->position
  {:west
   {"x" 30 
    "y" 120
    "r" 0}
   :east
   {"x" (- width 20 router-padding) 
    "y" 120
    "r" 0}
   :south
   {"x" (+ (- (/ width 2)  80 ) router-padding) 
    "y" (- height (- router-padding 20))
    "r" -90}
   :north
   {"x" (+ (- (/ width 2)  80 ) router-padding) 
    "y" (+ router-padding 20)
    "r" -90}})

(defn set-port-loc [loc]
  (let [
        position (loc->position loc)
        coords (merge {:w 40 :h 80} (select-keys position ["x" "y"]) )]
    (println "Setting port location" :loc loc :coords coords)
    (this-as me
             (.attr me (clj->js coords))
             (set! (.-loc me) loc)
             (set!
              (.-rotation me) (position "r")))))

(defn update-position-of-moving-component [entity]
  (let [[x-vel y-vel] (.-_velocity entity)
        time (.-_time entity)]
    (set! (.-x entity) (+ (.-x entity) (* time x-vel)))
    (set! (.-y entity) (+ (.-y entity) (* time y-vel))))
          entity)

(defn init-moving []
  (this-as me
           (.requires me "2D")
           (dump-statebag "I am a moving thing" :time (.-_time me) :vel (.-_velocity me))
           
           (.bind me "EnterFrame" #(update-position-of-moving-component me))
           )
  )

(make-component "Mover" 
                (clj->js {
                          :init init-moving
                          :_time 0.1
                          :_velocity [10 10]
                                   }) )

;; private member access oyeah

(defn init-random-mover []
  (this-as me
           (let [vel [ (- 40 (rand-int 80)) (- 40 (rand-int 80))]]
             (println "HERE" vel)
             (.requires me "Mover")
             (set!
              (.-_velocity me) vel))))

(make-component "RandomMover"
                (clj->js {
                          :init init-random-mover
                          }))

(defn emit-packet []
  ;; eek
  (this-as me
           (dump-statebag "Emitting packet from port " :loc (.-loc me))
           (make-entity "Packet"))
  )

(defn activate-port []
  (this-as me
           (dump-statebag "I am a port that is being activated" :loc (.-loc me))
           (.delay me emit-packet 1000 1000)))


(defn init-router-component [] 
  (this-as me
           (.requires me "2D, Canvas, Color, Polygon")
           (.color me "rgb(20, 125, 40)")
           (.attr me (clj->js {"x" router-padding 
                               "y" router-padding 
                               "w" router-width
                               "h" router-height}))))

(defn init-packet []
  (this-as me
           (.requires me "2D, Canvas, Color, Polygon, RandomMover")
           (.color me "rgb(100,0,0)")
           (.attr me (clj->js {:w 10 :h 10 :x 150 :y 150}))
;;           (.velocity me 1 1 0)
           (.trigger js/Crafty "PacketCreated")))

(make-component "Router" (clj->js {:init init-router-component
                                   }) )

(make-component "Port" (clj->js {:init init-port
                                 :setPortLoc set-port-loc
                                 :activatePort activate-port}))

(make-component "Packet" (clj->js {:init init-packet}))

(make-scene-with-transition "Intro" loading-scene "Game")
(make-scene "Game" game-scene game-scene-uninit)
(make-scene-with-transition "Finish" finish-scene "Intro")

(defn start-game []
  (.init js/Crafty 480 320)
  (.background js/Crafty "rgb(87, 109, 20)")
  (switch-to-scene "Intro"))

(.addEventListener js/window "load" start-game)
