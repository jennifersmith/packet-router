(ns packet-router.core)

(enable-console-print!)

;;===== These are all moving out interop calls into own functions : not long term thing ... presume could get a bit more fancy ====

(defn set-color
([entity color alpha]
                   (.color entity color alpha)) 
([entity color]
                   (.color entity color)))

(defn set-attr [entity attributes]
  (.attr entity (clj->js attributes)))

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
  (let [title-text (.e js/Crafty "TitleText") ]
    (.text
     title-text
     "Welcome to Packet Router.... <br/>a game created for Connected Worlds theme Ludum Dare 30")
    (set-attr title-text  {:y 100})) )

(defn make-entity [name]
  (.e js/Crafty name))
;; So I am REALLY not using clojure correctly. I don't care. I can see a way and it still
;; feels easier to me to be in fam language. Justifications :)
(defn position-port [e loc]
  (.setPortLoc e loc) e)

(defn entity-count [name]
  (.-length (js/Crafty name)))



(defn game-scene []
  (this-as me
           (.e js/Crafty "Router")
           (let [ports [(position-port (.e js/Crafty "Port") :north)
                        (position-port (.e js/Crafty "Port") :east)
                        (position-port (.e js/Crafty "Port") :west)
                        (position-port (.e js/Crafty "Port") :south)]]
             (set! (.-packetQueue me) (atom #queue []))
             (doseq [port ports]

               (.activatePort port)))))

(defn game-scene-uninit []
  (this-as me
           (if-let [unbind (.-unbind-my-events me)]
             (unbind))))
 
(defn finish-scene []
  (let [title-text (.e js/Crafty "TitleText") ]
    (.text
     title-text
     "YOU DIED! <br/>(But you got a good score, honest)")
    (set-attr title-text  {:y 120}))
)


(def width 480)
(def height 320)
(def router-padding 50)

(def router-width (- width (* 2 router-padding)))
(def router-height (- height (* 2 router-padding)))

(def port-width 40)

(defn process-port-key [port]
  (when (.isDown port (.-shortcut-key port))
    (.trigger js/Crafty "PortOpened" {:port port})))

(defn init-port []
  (this-as me
           (.requires me "Delay, 2D, Canvas, Color, Polygon, Keyboard")
           (set-color me "rgb(50, 0, 50)")
           (set! (.-z me) 2)
           (set-attr me {:x 0 :y 0 :w 40 :h 80})
           (.origin me "center")
           (.bind me "KeyDown" #(process-port-key me))
           (let [entrance (make-entity "Entrance")]
             (.attach me entrance)
             (set! (.-entrance me) entrance)
             (set-attr entrance {:x 35 :y 37 })
             )
           (let [shortcut (make-entity "Color, 2D, DOM, Text")]
             (set-attr shortcut {
                                 :x -50
                                 :y -1})
             (.attach me shortcut)
             (.text shortcut "\u2190")
             (.textFont shortcut (clj->js {:weight "bold" :size "70px"}))
             (set-color shortcut "#000000", 1.0)
             (set! (.-shortcut me) shortcut))))
(def colors #{
              "rgb(0,0,255)"
              "rgb(0,255,0)"
              "rgb(255,0,0)"
              "rgb(255,0,255)"})

(def loc->position
  {:west
   {"x" router-padding
    "y" 120
    "r" 0
    :entrance [(+ router-padding 40) (+ 120 40)]
    :heading 90
    :color "rgb(0,0,255)"
    :shortcut (.-LEFT_ARROW (.-keys js/Crafty))}
   :east
   {"x" (- width router-padding 40) 
    "y" 120
    "r" 180
    :entrance [150 150]
    :heading -90
    :color "rgb(0,255,0)"
    :shortcut-icon "\u2190"
    :shortcut (.-RIGHT_ARROW (.-keys js/Crafty))}
   :south
   {"x" (+ (- (/ width 2)  80 ) router-padding) 
    "y" (- height router-padding 60)
    "r" -89.999999
    :entrance [150 150]
    :heading 180
    :color "rgb(255,0,0)"
    :shortcut-icon "\u2190"
    :shortcut (.-DOWN_ARROW (.-keys js/Crafty))}
   :north
   {"x" (+ (- (/ width 2)  80 ) router-padding) 
    "y" 30
    "r" 89.999999
    :entrance [150 150]
    :heading 0
    :color "rgb(255,0,255)"
    :shortcut-icon "\u2190"
    :shortcut (.-UP_ARROW (.-keys js/Crafty))}})

(defn set-port-loc [loc]
  (let [
        position (loc->position loc)
        heading (:heading position)
        coords (select-keys position ["x" "y"])]
    (println "Setting port location" :loc loc :coords coords :position position)
    (this-as me
             (let [shortcut (.-shortcut me)
                   shortcut-key (:shortcut position)]
               (set! (.-x me) (position "x"))
               (set! (.-shortcut-key me) (position :shortcut))
               (set! (.-y me) (position "y"))
               (println (.-w me))
               (set! (.-loc me) loc)
               (set-color me (:color position))
               (set! (.-heading me) heading)
               (set! (.-rotation me) (position "r"))
               )
)))

(defn update-position-of-moving-component [entity]
  (let [[x-vel y-vel] (.-vectorvelocity entity)
        time (.-_time entity)]
    (set! (.-x entity) (+ (.-x entity) (* time x-vel)))
    (set! (.-y entity) (+ (.-y entity) (* time y-vel))))
          entity)

(defn move-mover [heading velocity] 
  (this-as me
           (set! (.-heading me) heading) 
           (set! (.-velocity me) velocity) 
           (set! (.-vectorvelocity me) (map #(* % velocity) (angle-to-unit-vectors (deg->rad heading))))))

(defn reflect-mover [normal] 
  (this-as me
           (.move me (- (* 2 normal) (.-heading me)) (.-velocity me))))

(defn init-moving []
  (this-as me
           (.requires me "2D")
           (set! (.-move me) move-mover) ;; mmmmmm
           (set! (.-reflect me) reflect-mover)
           (.bind me "EnterFrame" #(update-position-of-moving-component me))))

(make-component "Mover" 
                (clj->js {
                          :init init-moving
                          :_time 0.1
                          :velocity 0
                          :vectorvelocity [0 0]
                          :heading 0
                                   }) )

;; the programmer can only work in degrees today
(defn deg->rad [angle]
  (* angle (/ (.-PI js/Math) 180)))

(defn sin [angle]
  (.sin js/Math  angle))

(defn cos [angle]
  (.cos js/Math angle))

(def angle-to-unit-vectors (juxt sin cos))
;; private member access oyeah

(defn init-random-mover []
  (this-as me
           (.requires me "Mover")))

(defn move-randomly [min-heading max-heading]
  (this-as me
           (let [vel (+ 10 (rand-int 40))
                 heading  (+ min-heading (rand-int (- max-heading min-heading)))]
             (.move me heading vel))))


(make-component "RandomMover"
                (clj->js {
                          :init init-random-mover
                          :moveRandomly move-randomly
                          }))

(defn fetch-global-router-evil []
  (js/Crafty (aget (js/Crafty "Router") 0)))

(defn emit-packet [port]
  ;; eek
  (let [packet (make-entity "Packet")
        heading (.-heading port)
        entrance (.-entrance port)]
    (set! (.-portColor packet) (.-_color port))
    (change-state packet :emitted)
    (set-attr packet {:x (.-x entrance) :y (.-y entrance)})
    (.moveRandomly packet (- heading 60) (+ heading 60))))

(defn activate-port []
  (this-as me
           (dump-statebag "I am a port that is being activated" :loc (.-loc me))
           (.delay me #(emit-packet me) 1000 1000)))

;; normal - the angle of 'normal' for bounce
(defn make-router-boundary [router x y w h normal]
  (let [router-boundary (make-entity "RouterBoundary")]
    (.attach router router-boundary)
    (set! (.-normal router-boundary) normal)
    (set-attr router-boundary {:w w :x x :h h :y y})))

(defn current-packet-changed [packet-queue]
  (when-let [next (peek packet-queue)]
    (change-state next :next)))

(defn send-packet-out [router args]
  (let [
        port (:port args)
        packet-queue (.-packetQueue router)]
    (when-let [current-packet (peek @packet-queue)]
      (prn "SENDING OUT PACKET... now find correct queue")
      (swap! packet-queue pop)
      (current-packet-changed @packet-queue)
      (set! (.-destination current-packet) port)
      (change-state current-packet :sent))))

(defn new-packet-arrived [router args]
  (let [
        packet-queue (.-packetQueue router)
        new-packet (args :new-packet)]
    (swap! packet-queue conj (args :new-packet))
    (if (= 1 (count @packet-queue))
      (current-packet-changed @packet-queue))
    (prn "NOW THERE ARE " (count @packet-queue) " PACKETS IN THE QUEUE")
    (when (> (count @packet-queue) 40)
      (println "TOO MANY PACKETS!")
      (.scene js/Crafty "Finish"))))

(defn init-router-component [] 
  (this-as me
           (.requires me "2D, Canvas, Color, Polygon")
           (set-color me "rgb(20, 125, 40)")
           (set-attr me {"x" router-padding 
                      "y" router-padding 
                      "w" router-width
                      "h" router-height})
           (make-router-boundary me (+ 5 router-padding)   router-padding  5 router-height 180)
           (make-router-boundary me (+ router-width router-padding -10)  router-padding  5 router-height 180)
           (make-router-boundary me router-padding  router-padding  router-width 5, 90)
           (make-router-boundary me router-padding  (+ router-padding router-height -5) router-width 5, 90)
           (prn "INIT ROUTER" (count (.-packetQueue me)))
           (set! (.-packetQueue me) (atom #queue []))
           (.bind me "PacketCreated" #(new-packet-arrived me %))
           (.bind me "PortOpened" #(send-packet-out me %))))


(defn bounce [packet [args]]
  (let [wall (.-obj args)
        normal (.-normal wall)]
    (.reflect packet normal)))

(defn make-tick [packet]
  (let [tick (make-entity "Dom, 2D, Color, Canvas, Text")]
    (.attach packet tick)
    (set-color tick (.-_color (.-destination packet)))
    (.text tick "X")
    (.textFont tick (clj->js {"size" "20px"}))
    (set-attr tick {:x (+ 5 (.-_x packet)) :y (+ 5 (.-_y packet))})
    ))


(def state->paint
  {:new
   (fn [packet]
     (set-color packet "rgb(0,0,0)")
     (set! (.-w packet) 10)
     (set! (.-h packet) 10)
     (set! (.-alpha packet) 1.0))
   :queued 
   (fn [packet]
     (set-color packet "rgb(100,0,0)")
     (set! (.-w packet) 10)
     (set! (.-h packet) 10)
     (set! (.-alpha packet) 0.7))
   :emitted
   (fn [packet]
     (set-color packet (first (shuffle (disj colors (.-portColor packet)))))
     )
   :sent
   (fn [packet]
     (set-color packet "rgb(100,100,100)")
     (make-tick packet)
     (.delay packet #(.destroy packet) 1000 0 )
     )
   :next
   (fn [packet]
     (set! (.-w packet) 30)
     (set! (.-h packet) 30)
     (set! (.-alpha packet) 1.0))}) 

(defn change-state [packet new-state]
  (set! (.-state packet) new-state)
  ((state->paint new-state) packet))


(defn init-packet []
  (this-as me
           (.requires me "2D, Canvas, Color, Polygon, RandomMover, Collision,Delay")
           (change-state me :new)
;;           (.velocity me 1 1 0)
           (.trigger js/Crafty "PacketCreated" {:new-packet me})
           (.onHit me "RouterBoundary" #(bounce me %))
           ))

(make-component "Router" (clj->js {:init init-router-component
                                   }) )

(defn init-entrance []
  (this-as me
           (.requires me "2D, Canvas, Color, Polygon")
           (set-color me "rgb(100,100,100)")
           (set! (.-z me) 2)
           (set-attr me {:w 5 :h 6})))

(defn init-router-boundary []
  (this-as me
           (.requires me "2D, Polygon, Canvas, Collision, Color")
           ))

(defn init-title-text []
  (this-as me
           (.requires me "Color,2D, DOM, Text")
           (.textFont me (clj->js {:size "24px"}))
           (.css me (clj->js {:text-align "center"}))
           (.textColor me "rgb(100,100,100)")
           (set-attr me {:x router-padding :w router-width})
           ))
(make-component "Entrance" (clj->js {:init init-entrance}))

(make-component "Port" (clj->js {:init init-port
                                 :setPortLoc set-port-loc
                                 :activatePort activate-port}))

(make-component "Packet" (clj->js {:init init-packet}))

(make-component "RouterBoundary" (clj->js {:init init-router-boundary}))

(make-component "TitleText" (clj->js {:init init-title-text}))

(make-scene-with-transition "Intro" loading-scene "Game")
(make-scene "Game" game-scene game-scene-uninit)
(make-scene-with-transition "Finish" finish-scene "Intro")


(defn start-game []
  (.init js/Crafty 480 320)
  (.background js/Crafty "rgb(100, 100, 100)")
  (switch-to-scene "Intro"))

(.addEventListener js/window "load" start-game)
