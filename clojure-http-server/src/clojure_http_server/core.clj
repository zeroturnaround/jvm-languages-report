(ns clojure-http-server.core
 (:require [clojure.string])
 (:import (java.net ServerSocket SocketException )
  (java.util Date)
  (java.io File RandomAccessFile PrintWriter BufferedReader InputStreamReader BufferedOutputStream)))

(def web-root ".")
(def default-file "index.html")

(defn send-http-response 
 "Send data to client"
 [client-socket status content-type byte-content]
 (let
  [out-stream (.getOutputStream client-socket) out (new BufferedOutputStream out-stream) print-out (new PrintWriter out)]
  (do
   (.println print-out status)
   (.println print-out "Server: Clojure HTTP Server 1.0")
   (.println print-out (new Date))
   (.println print-out (str "Content-type: " content-type))
   (.println print-out (str "Content-length " (count byte-content)))
   (.println print-out)
   (.flush print-out)
   (.write out byte-content)
   (.flush out)
   (.close out-stream)
  )))

(defn send-html-response
 "Html response"
 [client-socket status title body]
 (let [html (str "<HTML><HEAD><TITLE>" title "</TITLE></HEAD><BODY>" body "</BODY></HTML>")]
  (send-http-response client-socket status "text/html" (.getBytes html "UTF-8"))
 ))


(defn get-reader
 "Create a Java reader from the input stream of the client socket"
 [client-socket]
 (new BufferedReader (new InputStreamReader (.getInputStream client-socket))))

(defn read-file
"Reads a binary file into a buffer"
[file-to-read]
(with-open [file (new RandomAccessFile file-to-read "r")]
  (let [x (byte-array (.length file-to-read))]
    (do
    (.read file x)
    x))))

(defn find-content-type
"Simple file type mappings"
[filename]
(cond 
 (or (.endsWith filename ".html") (.endsWith filename ".htm")) "text/html"
 (.endsWith filename ".gif") "image/gif"
 (or (.endsWith filename ".jpg") (.endsWith filename ".jpeg")) "image/jpeg"
 (or (.endsWith filename ".class") (.endsWith filename ".jar")) "applicaton/octet-stream"
 :else "text/plain"))

(defn send-file
 "Reads a file from the file system and writes it to the socket"
 [client-socket file http-method retry]
 (let [is-dir (.isDirectory file)]
  (if (and is-dir (not retry))
   (send-file client-socket (new File file default-file) http-method true))
  (if (and (.exists file) (not is-dir))
   ( let [content (if (= http-method "GET") (read-file file) (byte-array 0)) content-type (find-content-type (.getName file))]
     (do
      (send-http-response client-socket "HTTP/1.0 200 OK" content-type content)
      (println (str "File " (.getPath file) " of type " content-type " returned")
      )))
   (do
    (send-html-response client-socket "HTTP/1.1 501 Not Found" "File Not Found" (str "<h2>404 File Not Found: " (.getPath file) "</h2>"))
    (println (str "501 Not Implemented: " http-method " method")))
  )))

(defn process-request
 "Parse the HTTP request and decide what to do"
 [client-socket]
 (let [reader (get-reader client-socket) first-line (.readLine reader) tokens (clojure.string/split first-line #"\s+")]
  (let [http-method (clojure.string/upper-case (get tokens 0 "unknown"))]
   (if (or (= http-method "GET") (= http-method "HEAD"))
   (let [file-requested-name (get tokens 1 "not-existing") 
   file-requested 
   (new File web-root file-requested-name)]
    (send-file client-socket file-requested http-method false)
    )

    (do
     (send-html-response client-socket "HTTP/1.1 501 Not Implemented" "Not Implemented" (str "<h2>501 Not Implemented: " http-method " method.</h2>"))
     (println (str "501 Not Implemented: " http-method " method")))
   )

  )))


(defn respond-to-client 
 "A new http client is connected. Process the request" [client-socket]
 (do (println "A client has connected" (new Date))
  (process-request client-socket)
  (.close client-socket)
  (println "Connection closed")))

(defn new-worker
"Spawn a new thread"
[client-socket]
(.start (new Thread (fn [] (respond-to-client client-socket)))))


(defn -main
 "The main method that runs via lein run"
 []
 (let [port 8080 server-socket (new ServerSocket port)]
  (do 
   (println (str "Listening for connections on port " port))
   (while true
   (let [client-socket (.accept server-socket)]
   (new-worker client-socket))))))


