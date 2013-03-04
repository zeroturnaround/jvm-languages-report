(ns clojure-http-server.core
  (:require clojure.string)
  (:import
    java.util.Date
    [java.net ServerSocket SocketException ]   
    [java.io File RandomAccessFile PrintWriter BufferedReader InputStreamReader BufferedOutputStream]))

(def web-root ".")
(def default-file "index.html")

(defn print-to-stream [client-socket byte-content & params] 
  (let [out       (new BufferedOutputStream (.getOutputStream client-socket))
        print-out (new PrintWriter out)] 
    (doseq [param params] 
      (.println print-out param))
    (.println print-out)
    (.flush print-out)
    (.write out byte-content)
    (.flush out)))


(defn send-http-response 
  "Send data to client"
  [client-socket status content-type byte-content]
  ;; with-open will close the stream for you
  (print-to-stream client-socket 
                   byte-content
                   status
                   "Server: Clojure HTTP Server 1.0"
                   (new Date)
                   (str "Content-type: " content-type)
                   (str "Content-length " (count byte-content))))

(defn send-html-response
  "Html response"
  [client-socket status title body]
 (let [html (str "<HTML><HEAD><TITLE>" title "</TITLE></HEAD><BODY>" body "</BODY></HTML>")]
   (send-http-response client-socket status "text/html" (.getBytes html "UTF-8"))))


(defn read-file
  "Reads a binary file into a buffer"
[file-to-read]
(with-open [file (new RandomAccessFile file-to-read "r")]
  (let [buffer (byte-array (.length file-to-read))]
    (.read (clojure.java.io/input-stream file-to-read) buffer)
    buffer)))

(defn find-content-type
  "Simple file type mappings"
  [filename]
  (let [ext (.substring filename (.lastIndexOf filename ".") (count filename))] 
    (or 
      (-> #(some #{ext} (first %))         
        (filter    
          [[[".html" ".htm"] "text/html"]
           [[".gif"] "image/gif"]
           [[".jpg" ".jpeg"] "image/jpeg"]
           [[".class" ".jar"] "applicaton/octet-stream"]])
        first
        second)
      "text/plain")))

(defn not-implemented [client-socket http-method]  
  (send-html-response client-socket "HTTP/1.1 501 Not Implemented" "Not Implemented" (str "<h2>501 Not Implemented: " http-method " method.</h2>"))
  (println "501 Not Implemented:" http-method "method"))


(defn send-file
  "Reads a file from the file system and writes it to the socket"
  [client-socket file http-method retry]
  (let [dir? (.isDirectory file)]    
    (cond    
      (not= http-method "GET") 
      (not-implemented client-socket http-method)
      
      (not (.exists file)) 
      (send-html-response client-socket "HTTP/1.1 404 Not Found" "Found" (str "<h2>404 Not Found: " (.getName file) "</h2>"))

      (and dir? (not retry))
      (send-file client-socket (new File file default-file) http-method true)
      
      :else
      (let [content (read-file file) 
            content-type (find-content-type (.getName file))]
        (send-http-response client-socket "HTTP/1.0 200 OK" content-type content)
        (println "File" (.getPath file) "of type" content-type "returned")))))

(defn process-request
  "Parse the HTTP request and decide what to do"
  [client-socket]
  (let [reader     (clojure.java.io/reader client-socket) 
        first-line (.readLine reader) 
        tokens     (if first-line (clojure.string/split first-line #"\s+"))]
    (println "line" first-line "tokens" tokens)
    (let [http-method (clojure.string/upper-case (get tokens 0 "unknown"))]      
      (if (or (= http-method "GET") (= http-method "HEAD"))
        (let [file-requested-name (get tokens 1 "not-existing") 
              file-requested (new File web-root file-requested-name)]
          (send-file client-socket file-requested http-method false))        
        (not-implemented client-socket http-method)) )))


(defn respond-to-client 
  "A new http client is connected. Process the request" 
  [server-socket]
  (println "A client has connected" (new Date))
  (with-open [client-socket (.accept server-socket)]
    (process-request client-socket))  
  (println "Connection closed"))

(defn new-worker
  "Spawn a new thread"
  [server-socket]
  (.start (new Thread (fn [] (respond-to-client server-socket)))))

(defn -main
  "The main method that runs via lein run"
  []
  (let [port 8080 
        server-socket (new ServerSocket port)]    
    (println "Listening for connections on port" port)
    (while true
      (new-worker server-socket))))
