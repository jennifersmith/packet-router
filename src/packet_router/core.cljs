(ns packet-router.core)

(enable-console-print!)


(defn start-game []
  (.init js/Crafty 480 320)
  (.background js/Crafty "rgb(87, 109, 20)")
  (println "Wooooop"))

(.addEventListener js/window "load" start-game)
