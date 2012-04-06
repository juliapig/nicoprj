;(
(ns scrabble.core
  (:gen-class)
  (:use [clojure.java.io :only (reader writer)]
        [clojure.contrib.generic.functor :only (fmap)])
        ;[clojure.contrib.io :only (write-lines)])
  (:require [clojure.zip :as zip]))

;(use '[clojure.java.io :only (reader)])
;(use '[clojure.contrib.generic.functor :only (fmap)])
;(require '[clojure.zip :as zip])

(defn get-lines [filename]
  (with-open [rdr (reader filename)]
    (doall (line-seq rdr))))

(defn get-text [filename]
  (with-open [rdr (reader filename)]
    (slurp rdr)))

; this one also in clojure.contrib.io, but doesn't work, something with *append* being non-dynamic.
(defn write-lines
  "Writes lines (a seq) to f, separated by newlines.  f is opened with
  writer, and automatically closed at the end of the sequence."
  [f lines]
  (with-open [writer (writer f)]
    (loop [lines lines]
      (when-let [line (first lines)]
        (.write writer (str line))
        (.newLine writer)
        (recur (rest lines))))))


(defn find-re [re l]
  (filter (partial re-seq re) l))

(defn have-letters? 
  "Find out if there are enough letters to make word. A space in letters denotes a blank"
  [letters word]
  (->> letters                                ; start with available letters
       frequencies                            ; map met frequentie van elke letter in letters
       (fmap -)                               ; maak frequenties negatief, fmap is een functor, zie haskell.
       (merge-with + (frequencies word))      ; voeg deze samen met de (positieve) frequencies van het gezochte woord
       vals                                   ; lijst met alle frequentie-verschillen, een positieve betekent dat een letter in word niet in letters zit.
       (filter pos?)                          ; keep positive values
       (apply +)                              ; and sum these
       (>= (count (re-seq #" " letters)))))   ; and check if we have enough blanks for the not found letters 

; @todo letters in pattern tussen [haken] moet je niet tellen als letters die er al liggen.
(defn find-words 
  "Find words in word list (wl) that match pattern and only use letters in let
   pat is a regex like ...T.{1,3}, but given as a string, not as a #\"regexp\""
  [pat lt wl]
  (->> wl
       (find-re (re-pattern pat))
       (filter (partial have-letters? (apply str lt (re-seq #"[A-Z]" pat))))))

(defn find-words2 
  "Find words in word list (wl) that match pattern and only use letters in lt
   pat is a regex like ...T.{1,3}, but given as a string, not as a #\"regexp\""
  [pat lt wl]
  (->> wl
       (find-re (re-pattern pat))
       (filter (partial have-letters? lt))))

;(def sp (get-lines "/media/nas/media/Talen/Dictionaries/sowpods.txt"))
;(def nl (get-lines "/media/nas/media/Talen/Dictionaries/wf-nl.txt"))
;(def nl2 (get-lines "/media/nas/media/Talen/Dictionaries/perletter/alles-sorted.txt"))
; deze daggad heeft woorden van max 8 letters, is 350.000 regels.
;(def dnl (make-dawg-z (get-lines "/media/nas/media/Talen/Dictionaries/nl-daggad-sorted-8.txt")))
; kan kijken hoe ver je dit kunt rekken, maar principe voor andere functies hiermee prima te testen.

; helper
(defn empty-to-nil [val]
  "set result to nil if param is empty: nil, (), []"
  (if (empty? val) nil val))

; functions to create daggad (gaddag)
; NdV: I just know the principle, have not looked at implementations in clojure or other language yet.
; (take 0 word) returns and empty list (), which doesn't work well with if-let. Therefore, make it nil with empty-to-nil
(defn split-rev-1 [word pos]
  "Return word split at position as a string, < at the split-point"
  (if (= pos  0)
    word
    (apply str (concat (drop pos word) "<" (reverse (take pos word))))))

(defn split-rev-1-old [word pos]
  [(drop pos word) (empty-to-nil (reverse (take pos word)))])

(defn split-rev [word]
  (map (partial split-rev-1 word) (range (count word))))

(defn add-daggad-1 [daggad [forw backw]]
  (if-let [[first-letter & more] forw]
    ; still have letters to move forward
    (let [d2 (get (:next daggad) first-letter)]
      (assoc daggad :next (assoc (:next daggad) first-letter (add-daggad-1 d2 [more backw]))))
    ; else: maybe have backward letters
    (if-let [[first-letter & more] backw]
      (let [d2 (get (:prev daggad) first-letter)]
        ;(println daggad d2 first-letter more)
        (assoc daggad :prev (assoc (:prev daggad) first-letter (add-daggad-1 d2 [forw more]))))
      ; else: no more letters, mark as final
      (assoc daggad :final true))))
  
  
; add word to daggad, return new daggad
(defn add-daggad [daggad word]
  (reduce add-daggad-1 daggad (split-rev word))) 

(defn make-daggad [word-list]
  (reduce add-daggad nil word-list))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; functions above work functionally, but use too much memory (and CPU?) to read the complete english or dutch word list
;;;
;;; try if using a zipper helps
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

; a zipper holds the current node and a path.
; a path contains steps as a vector
; a step contains of of key and a node, as [key node]
(defn map-zip 
  "Create specific zipper for nested maps. Based on zipper code, but need downto function"
  [m]
  ; ((vector (vector) m)))
  [m (list)])

; create a new key if it doesn't exist
(defn downto
  "Move down to a key in a zipper of nested maps"
  [[node path] k]
  (if-let [node2 (get node k)]
    [node2 (conj path [k node])]
    [{} (conj path [k node])]))


; dan met edits
; en zonder check of path leeg is, vraag of dit nodig is, heb ook top-functie nu.
; evt checken of het kan, of pnode wel een map is. Anders vanzelf een foutmelding, maar mogelijk wat cryptisch.
(defn up
  "Move up one level in a zipper of nested maps"
  [[node [[key pnode] & rest]]]
  [(assoc pnode key node) rest])

(defn up-n
  "Move up n levels in a zipper of nested maps"
  [n z]
  (if (= n 0) z (recur (- n 1) (up z))))
  
; replace om : final true te kunnen doen.
; replace bestaat al wel in clojure.core
; :final true not used now for brevity. 
(defn zip-replace
  "Replace the current node in a zipper of nested maps"
  [[node path] val]
  [val path])

(defn top
  "Move up to the top in a zipper of nested maps"
  [[node path :as mz]]
  (if (empty? path) mz (recur (up mz))))

  ;(println "================\ntop: ")
  ;(println node)
  ;(println path)
  ;(println mz)

(defn root
  "Return root-node of a zipper of nested maps"
  [mz]
  (first (top mz)))

; end of 'generic' zipper, now daggad code
  
(defn add-daggad-z-1-old 
  "Add one letter combination to a daggad zipper. Recursive function, zipper moves to top when ready."
  [dz [forw backw]]
  (if-let [[first-letter & more] forw]
    (recur (-> dz (downto :next) (downto first-letter)) [more backw])
    ; else: maybe have backward letters
    (if-let [[first-letter & more] backw]
      (recur (-> dz (downto :prev) (downto first-letter)) [forw more])
      ; else: no more letters, mark as final
      (-> dz (downto :final) (zip-replace true) top))))      

(defn add-daggad-z-1
  "Add one letter combination to a daggad zipper. Recursive function, zipper moves to top when ready."
  [dz [forw backw]]
  (if-let [[first-letter & more] forw]
    (recur (-> dz (downto :n) (downto first-letter)) [more backw])
    ; else: maybe have backward letters
    (if-let [[first-letter & more] backw]
      (recur (-> dz (downto :p) (downto first-letter)) [forw more])
      ; else: no more letters, mark as final
      (-> dz (downto :f) top))))      


(defn add-daggad-z
  "Add one word to a daggad zipper. Zipper will be redirected to the top before adding"
  [dz word]
  (reduce add-daggad-z-1 (top dz) (split-rev word)))

; in add-daggad-1 ben je al aan het zoeken met hashmap in de daggad om te kijken waar je wat moet toevoegen.

(defn make-daggad-z
  "Make daggad using a zipper"
  ([word-list] 
    ; (make-daggad-z word-list (map-zip {:next {} :prev {} :final false})))
    (make-daggad-z word-list (map-zip {})))
  ([word-list dz]
    (if-let [[first-word & more-words] word-list]
      (recur more-words (add-daggad-z dz first-word))
      (root dz))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; also DAWG to check speed and memory usage.
(defn add-dawg-z-1
  "Add one word/letter combination to a daggad zipper. Recursive function, zipper moves to top when ready."
  [dz word]
  (if-let [[first-letter & more] (word)]
    (recur (-> dz (downto first-letter)) more)
    (-> dz (downto :f) top)))      

(defn make-dawg-z-old
  "Make dawg using a zipper"
  ([word-list] 
    ; (make-daggad-z word-list (map-zip {:next {} :prev {} :final false})))
    (make-dawg-z-old word-list (map-zip {})))
  ([word-list dz]
    (if-let [[first-word & more-words] word-list]
      (recur more-words (add-dawg-z-1 dz first-word))
      (root dz))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; read a daggad/dawg alphabetically, keep zipper at the inserted pos and move as little as possible.

(defn overlap
  "Return #letters that are the same (from the beginning) in both words"
  ([w1 w2]
    (overlap w1 w2 0))
  ([w1 w2 acc]
    (if-let [[l1 & more1] w1]
      (if-let [[l2 & more2] w2]
        (if (= l1 l2)
          (recur more1 more2 (inc acc))
          acc)
        acc)
      acc)))

; use idiomatic (?) seq instead of empty-to-nil
; http://clojuredocs.org/clojure_core/1.2.0/clojure.core/if-let
(defn add-dawg-z 
  "Add one word/letter combination to a daggad zipper. Recursive function, zipper does not move to top when ready."
  [dz word last-word]
  (loop [ov (overlap word last-word)
         dz2 (up-n (- (count last-word) ov) dz)
         word2 (seq (drop ov word))]
    (if-let [[first-letter & more] word2]
      (recur ov (downto dz2 first-letter) more)
      (-> dz2 (downto :f) up))))      

;(println ov)
    ;(println dz2)
    ;(println word2)

(defn make-dawg-z 
  "Make dawg using a zipper, keep track of last added word."
  ([word-list] 
    ; (make-daggad-z word-list (map-zip {:next {} :prev {} :final false})))
    (make-dawg-z word-list "" (map-zip {})))
  ([word-list last-word dz]
    (if-let [[first-word & more-words] word-list]
      (recur more-words first-word (add-dawg-z dz first-word last-word))
      (root dz))))
    
; this one consumes a lot of memory, too much for my 1GB machine.
(defn make-daggad-z
  "Make daggad using split-rev and make-dawg"
  [word-list]
  (->> word-list
       (map split-rev)
       flatten
       sort
       make-dawg-z))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; helper functions to test daggad/dawg
(defn is-word? [daggad word]
  "Helper to see if daggad contains word"
  (if-let [[first-letter & more] word]
    (recur (get daggad first-letter) more)
    (not (nil? (:f daggad)))))

(defn get-part-daggad [daggad word]
  "Helper to get part of a daggad"
  (if-let [[first-letter & more] word]
    (recur (get daggad first-letter) more)
    daggad))  

(defn is-word-old? [daggad word]
  "Helper to see if daggad contains word"
  (if-let [[first-letter & more] word]
    (recur (get (:n daggad) first-letter) more)
    (not (nil? (:f daggad)))))

(defn test-if-let [word]
  (if-let [[first-letter & more] word]
    (do 
      (println "If-let succeeded with word, first, more: " word first-letter more)
      (recur more))
    (println "If-let failed with word: " word)))

; (test-if-let "HOND")

; hier doall nodig, zoals in get-lines?
(defn get-gawd [filename]
  (with-open [rdr (reader filename)]
    (make-dawg-z (line-seq rdr))))

;(def dsp (make-daggad sp))
;(def dnl (make-daggad nl))

(defn -main
  [& args]
  (println "Main started")
  (def dnl (make-dawg-z (get-lines "/media/nas/media/Talen/Dictionaries/nl-daggad-sorted-1000.txt")))
  (println "dnl read")
  (println (is-word? dnl "AANVALLEN"))
  (println "Main ended"))
;)
