;( ; deze als haakjes niet kloppen.
(ns scheidsclj.core
  (:gen-class)
  (:use scheidsclj.db)
  (:use scheidsclj.geneticlib)
  (:use scheidsclj.lib)
  (:use scheidsclj.util)
  (:use scheidsclj.print))

; global vars, but only set/read-in once.
(declare *lst-inp-games* *lst-inp-persons* *ar-inp-games*) 
  
(defn select-referee [game referee]
  (merge (select-keys game [:game-id :game-name :date])
         (select-keys referee [:referee-id :referee-name :whinefactor :value :same-day])))
  
; @result game-hashmap, als element in vec-sol-referee
(defn choose-random-referee [game-id]
  "@result game-hashmap, as element in vec-sol-referee"
  (let [game (*ar-inp-games* game-id)
        referee (rand-nth (:lst-can-referee game))]
    (select-referee game referee))) 

; @note hogere fitness is beter.
; prod_games_person_dag: 1..veel minder is beter, alles meer dan 1 is gewoon fout.
; wel bepalen hoe fout het is, zodat het beter kan worden, als bv 2 persons elk 2x op een dag moeten fluiten.
; max_referee: 1..10 minder is beter
; n_versch_referee: 1..20 meer is beter
; som_whinefactoren: 0..10000 minder is beter
; door 1-maxwedstrperdag wordt dit deel 0 als het gewoon goed is, en negatief bij fouten.
; 19-9-2010 NdV max_referee toch niet zo belangrijk, zelfdedag telt minder dan andere dag, en in zf al rekening mee gehouden.
; 19-9-2010 NdV zelfde geldt eigenlijk ook voor aantal verschillende refereeen.
; 19-9-2010 NdV maar wel de lasten goed verdelen, dus max_whinefactoren wel belangrijk.
; expr (1-$prod_games_person_dag) * 100000 + (10-$max_referee)*100 + $n_versch_referee - (0.0001 * $som_whinefactoren)
(defn calc-fitness [prod-games-person-dag max-referee n-versch-referee som-whinefactoren max-whinefactoren]
  (- (* (- 1 prod-games-person-dag) 100000)
     max-whinefactoren
     (* 0.0001 som-whinefactoren)))

; bepaal per person welke games deze fluit in de gemaakte oplossing
; input lijst van persons (hashmap)
; result lijst van persons (hashmap) aangevuld met lijst van games per person
(defn det-person-games [lst-inp-persons vec-sol-referee]
  (map #(assoc %1 :lst-games (for [sol vec-sol-referee :when (= (:referee-id %1) (:referee-id sol))]
    sol)) lst-inp-persons))

; deze nog herschrijven met -> of ->>
; elke person heeft lijst van games, elke game is een hashmap. Maak per hashmap een nieuwe, zodat ze met merge-with opgeteld kunnen worden per person
(defn det-prod-games-person-dag [lst-person-games]
  (apply * 
    (map (fn [person] 
      (apply *
        (vals
          (apply merge-with + 
            (map #(hash-map (:date %) 1) (:lst-games person)))))) lst-person-games)))
  
(defn det-lst-sol-person-info [lst-person-games]
  (map #(assoc % 
              :nfluit (count (:lst-games %))
              :whinefactor (apply * (map :whinefactor (:lst-games %)))) lst-person-games))

; bepaal sleutel waarden van de oplossing
; @result hashmap
(defn det-sol-values [lst-inp-persons vec-sol-referee]
  (let [lst-person-games (det-person-games lst-inp-persons vec-sol-referee)]
    (hash-map 
      :lst-whinefactoren (map #(* (:whinefactor %1)
                          (apply * (for [sol (:lst-games %1)] 
                             (/ (:whinefactor sol) (:value sol))))) lst-person-games)
      :lst-aantallen (map #(count (:lst-games %1)) lst-person-games)
      :prod-games-person-dag (det-prod-games-person-dag lst-person-games)
      :lst-sol-person-info (det-lst-sol-person-info lst-person-games))))

(defn add-statistics [vec-sol-referee note solnr-parent]
  (let [sol-values (det-sol-values *lst-inp-persons* vec-sol-referee)
        n-versch-referee (count (for [n (:lst-aantallen sol-values) :when (> n 0)] 1))
        lst-whinefactoren (:lst-whinefactoren sol-values)] 
    (assoc sol-values
            :vec-sol-referee vec-sol-referee
            :note note
            :solnr (new-sol-nr)
            :solnr-parent solnr-parent
            :fitness (calc-fitness (:prod-games-person-dag sol-values) 
                                   (apply max (:lst-aantallen sol-values)) 
                                   n-versch-referee 
                                   (apply + lst-whinefactoren) 
                                   (apply max lst-whinefactoren))
            :max-referee (apply max (:lst-aantallen sol-values))
            :n-versch-referee n-versch-referee
            :som-whinefactoren (apply + lst-whinefactoren)
            :max-whinefactoren (max lst-whinefactoren))))

; beetje raar dat choose-random-referee met de game-id wordt aangeroepen, en niet met de game gegevens
; zelf. Zo gedaan omdat deze functie vanuit meerdere plekken wordt aangeroepen, en de gegevens niet overal bekend
; zijn. 
; @todo nog refactoren zodat het meer functioneel wordt, niet afhankelijk van global variables.
(defn make-solution [lst-input-games]
  (let [vec-sol-referee (vec (map #(choose-random-referee (:game-id %1)) lst-input-games))]
    (add-statistics vec-sol-referee "Initial solution" 0)))

(defn mutate-game [sol-referee]
  (choose-random-referee (:game-id sol-referee)))
        
; @note tail-recursive, sort-of continuation style passing?
(defn mutate-solution-rec [n vec-sol-referee solnr-parent]
  (if (zero? n) 
    (add-statistics vec-sol-referee "Mutated game(s)" solnr-parent)
    (recur (dec n) 
      (let [rnd (rand-int (count vec-sol-referee))]
        (assoc vec-sol-referee rnd (mutate-game (get vec-sol-referee rnd))))
      solnr-parent)))      
      
; @todo de rand-int 2 waarde halen uit de command-line params. Hier ook een goede lib voor?
; bepaal randomwaarde 1 of 2, en muteer oplossing zo vaak
(defn mutate-solution [sol]
  (mutate-solution-rec (inc (rand-int 2)) (:vec-sol-referee sol) (:solnr sol)))

; globals definieren met def, dan maar eenmalig een waarde toegekend (?)
; en krijgen pas een waarde bij uitvoeren, dus in andere functies niet bekend op compile time.
(defn init-globals []
  (def *lst-inp-games* (query-input-games))
  (def *lst-inp-persons* (det-lst-inp-persons)) 
  (def *ar-inp-games* 
    (zipmap (map :game-id *lst-inp-games*) *lst-inp-games*)))


; @result fitness of sol if game with index game-index is refereed by referee
(defn fitness-sol-game-change-referee [vec-sol-referee game-index referee]
      ;(let [rnd (rand-int (count vec-sol-referee))]
  (-> (assoc vec-sol-referee game-index 
         (select-referee (get vec-sol-referee game-index) referee))
      (add-statistics "" 0)
      (:fitness)))
      
; @result max fitness als bij oplossing sol de game met index game-index wordt aangepast.
; lst-kan-fluiten uit *ar-inp-games* halen, niet uit vec-sol-referee
; vraag of je deze info ook niet bij oplossing wilt zetten, is toch read-only/immutable.
(defn max-fitness-sol-game-change [vec-sol-referee game-index]
  (apply max (map #(fitness-sol-game-change-referee vec-sol-referee game-index %) 
    (:lst-can-referee (*ar-inp-games* (:game-id (get vec-sol-referee game-index))))
    )))

; @todo deze implementeren, dan wel maak-oplossing functies nodig)
; @note beetje map-reduce achtig: per game die je aanpast een andere functie, is dan parallel uit te voeren.
(defn kan-naar-betere [sol]
  (> (apply max (map #(max-fitness-sol-game-change (:vec-sol-referee sol) %)
                     (range (count (:vec-sol-referee sol)))))
    (:fitness sol)))

; @todo alleen saven bij een minimale fitness.
(defn handle-best-solution [proposition]
  (print-best-solution proposition *ar-inp-games* kan-naar-betere)
  (let [sol (first (:lst-solutions @proposition))]
    (if (> (:fitness sol) -2000)
      (save-solution sol))))

; @note evol-iteration functioneel opzetten: je krijgt iteratie en lijst oplossingen binnen, en retourneert deze ook.
; opties 1) handle-best-solution een sol meegeven. 2) de swap! in deze functie, dan handle aanroepen
; 3) check niet in deze function, maar in aanroepende make-proposition, dan hier ook de puts-dot in.
; keuze is nu optie 3
(defn evol-iteration [{:keys [lst-solutions iteration]}]
  (let [new-iteration (inc iteration)
        old-fitness (:fitness (first lst-solutions))
        new-solutions (map mutate-solution lst-solutions)
        sorted-solutions (sort-by :fitness > (concat new-solutions lst-solutions))
        best-solutions (take (count lst-solutions) sorted-solutions)
        new-fitness (:fitness (first best-solutions))]
     {:lst-solutions best-solutions 
      :iteration new-iteration}))   

(defn make-proposition [sol-args]
  (println "Make proposition" sol-args)
  (init-globals)
  (let [proposition (atom {:lst-solutions (repeatedly (:pop sol-args) #(make-solution *lst-inp-games*))
               :iteration 1})
        fitness (atom (:fitness (first (:lst-solutions @proposition))))]
    (while (< (:fitness (first (:lst-solutions @proposition))) (:fitness sol-args))
      (swap! proposition evol-iteration)
      (if (zero? (mod (:iteration @proposition) 100))
        (puts-dot))
      (when (> (:fitness (first (:lst-solutions @proposition))) @fitness)
        (reset! fitness (:fitness (first (:lst-solutions @proposition))))
        (handle-best-solution proposition)))
    (printlnf "Fitness: %f (goal: %f)" @fitness (:fitness sol-args))))

(defn -main [& args]
  (open-global-db)
  (delete-old-proposition)

  (let [sol-args {:pop 10
                  :iter 0
                  :fitness 100000
                  :nmutations 2
                  :loglevel ""
                  :print "better"}]
    (make-proposition sol-args))                  
  (close-global-db))  

;) ; deze als haakjes niet kloppen.
