(ns actors.core
  (:require [clojure.string :as str]
            [environ.core :refer [env]]
            [clj-http.client :as http]
            [clojure.data.json :as json]))

(def apikey 
  (env :apikey))

(defn searchUrl [type name] (str  "https://api.themoviedb.org/3/search/" type "?api_key=" apikey "&query=" name))
(defn creditsUrl [type id] (str  "https://api.themoviedb.org/3/" type "/" id "/credits?api_key=" apikey))

(def shows 
  (-> (slurp "shows.txt")
      str/split-lines))

(defn show-id [name]
  (prn (str "looking up show: " name))
  (-> (http/get (searchUrl "tv" name))
      :body
      json/read-str
      (get "results")
      first
      (get "id")))

(defn show-ids [shows]
  (pmap show-id shows))

(defn show-credits [id]
  (prn (str "looking up credits for: " id))
  (let [cast (-> (http/get (creditsUrl "tv" id))
                 :body
                 json/read-str
                 (get "cast"))]
    (map #(get % "name") cast)))

(defn cast-of-shows []
  (loop [slices (partition-all 20 shows)
         results []]
    (let [slice (first slices)]
      (if slice
        (do 
          (prn "loading a slice")
          (Thread/sleep 11000)  ;;avoid rate capping
          (recur (rest slices) 
                 (concat results (->> (show-ids slice)
                                      (pmap show-credits)
                                      flatten
                                      set))))
        results))))
