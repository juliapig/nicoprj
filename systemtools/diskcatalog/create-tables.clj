#!/bin/bash lein-exec

; create-tables.clj - Create tables if not exists already in database.

(load-file "../../clojure/lib/def-libs.clj") 
(load-file "lib-diskcat.clj")

(defn table-exists?
  "Return true if table already exists in db-spec. table is a :keyword"
  [db-spec table]
  (= 1 (count (jdbc/query db-spec
    (str "select name from sqlite_master where type='table' and name = '" (name table) "'")))))

(defn create-table
  "Create table if not exists yet. table is a :keyword."
  [db-spec table & specs]
  (if-not (table-exists? db-spec table)
    (jdbc/db-do-commands db-spec
      (apply jdbc/create-table-ddl table specs))))

(defn create-tables
  "Create tables if not exist yet"
  [db-spec opts]
  (create-table db-spec :file
    [:id "integer primary key"]
    [:fullpath "varchar"]
    [:folder "varchar"]
    [:filename "varchar"]
    [:filesize "integer"]
    [:ts_cet "varchar"]
    [:md5 "varchar"]
    [:goal "varchar"]
    [:importance "varchar"]
    [:computer "varchar"]
    [:srcbak "varchar"]
    [:action "varchar"])
  (jdbc/db-do-commands db-spec
    "create index if not exists ix_file_1 on file (filesize)"
    "create index if not exists ix_file_2 on file (filename)")
  (create-table db-spec :stats 
    [:id "integer primary key"]
    [:ts_cet "TEXT DEFAULT (strftime('%Y-%m-%d %H:%M:%S','now', 'localtime'))"]
    [:nfiles "integer"]
    [:ngbytes "float"]
    [:ngoal "integer"]
    [:nimportance "integer"]
    [:nsrcbak "integer"]
    [:naction "integer"]
    [:notes "varchar"])
  (create-table db-spec :action
    [:id "integer primary key"]
    [:ts_cet "TEXT DEFAULT (strftime('%Y-%m-%d %H:%M:%S','now', 'localtime'))"]
    [:action "varchar"]
    [:fullpath_orig "varchar"]
    [:fullpath_other "varchar"]
    [:notes "varchar"]))

(defn main [args]
  (when-let [opts (my-cli args #{:database}
        ["-h" "--help" "Print this help"
              :default false :flag true]
        ["-p" "--projectdir" "Project directory" :default "~/projecten/diskcatalog"]
        ["-db" "--database" "Database path" :default "~/projecten/diskcatalog/bigfiles.db"])]
    (let [db-spec (db-spec-path db-spec-sqlite (:database opts))]
       (create-tables db-spec opts))))

(main *command-line-args*)

