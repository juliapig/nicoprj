; interactive commands

;(def top250 (slurp "http://www.imdb.com/chart/top"))
;werkt niet, 403: forbidden. Dus save-as vanuit browser gedaan.

(def top250 (slurp "~/nicoprj/mediatools/movie/data/imdb-top250.html"))
; werkt niet, mss ~

(def top250 (slurp "/home/nico/nicoprj/mediatools/movie/data/imdb-top250.html"))
; werkt

; splitten <tr>'s with re-seq
(def s (re-seq #"<tr.*?</tr" top250))

(count s) => 251, including first as header.

; read html functions? => enlive, seen before.

(def data (fetch-url "file:///home/nico/nicoprj/mediatools/movie/data/imdb-top250.html"))

; in a hierarchical structure now

(def l (html/select data [:tr]))
; (count l) => 253, dus 2 meer.

; wil filteren op alleen de 250.
; eerst simpel, selecteren

; (def l250 (drop 1 (take 251 l)))

(def l250 (take 250 (drop 1 l)))

(def m1 (first l250))

movie.core=> (map html/text (html/select m1 [:td]))
("1." "9.2" "The Shawshank Redemption (1994)" "757,326")

(def lm-imdb (map imdb-movie-to-hashmap l250))
      

(map html/text (html/select m1 [:td]))

(defn list-to-db! [lm db-name table-name]
  (create-table! lm db-name table-name)
  (insert-rows! lm db-name table-name))

; use lobos
(defn create-table! [lm db-name table-name]
 
)  
 
; use ClojureQL
(defn insert-rows! [lm db-name table-name]

)  

; lobos try-outs
(lobos.connectivity/open-global movie.ddl/db)

; 11-6-2012 NdV migrate in versie 1.0 lijkt verandert te zijn, of werkt nog niet, de functie migratie kan ik niet vinden.
; dus ofwel terug naar oudere versie, ofwel de tabellen even zelf maken met SQL en alleen DML met ClojureQL doen. Voel hier wel wat voor om er een beetje voortgang in te houden...

(movie.ddl/migrate)
; (lobos.connectivity/close-global)

;(use 'lobos.core 'lobos.connectivity 'lobos.migration 'lobos.migrations)
;(open-global clogdb)
;(migrate)
