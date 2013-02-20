package com.zt

import java.util.StringTokenizer
import java.util.Date
import java.net.Socket
import java.net.ServerSocket
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.BufferedOutputStream
import java.io.File
import java.io.PrintWriter
import org.apache.commons.io.FileUtils

class HttpServer implements Runnable{
	val WEB_ROOT = "."
    val DEFAULT_FILE = "index.html"
	val Socket socket
	
 	new(Socket socket){
 		this.socket = socket
 	}

	def static void main(String[] args){
	    val PORT = 8080
	
	    val serverConnect = new ServerSocket(PORT)
	
	    println("\nListening for connections on port " + PORT + "...\n")
	
	    while (true){
	        val socket = serverConnect.accept()
	        val thread = new Thread(new HttpServer(socket))
	        thread.start()
	    }
  	}
  	
    override void run(){
        println("Connection opened. (" + new Date() + ")")

        val input = new BufferedReader(new InputStreamReader(socket.getInputStream()))
        val requestLine = input.readLine()
        
        if (requestLine != null){
            val StringTokenizer parse = new StringTokenizer(requestLine)
            val String method  = parse.nextToken().toUpperCase()

            if (method != "GET" && method != "HEAD"){
                socket.sendHtmlResponse(
                      "HTTP/1.1 501 Not Implemented",
                      "Not Implemented",
                      "<H2>501 Not Implemented: " + method + " method.</H2>")

                println("501 Not Implemented: " + method + " method.")
            }
            else{
                socket.sendFile(new File(WEB_ROOT, parse.nextToken()), method, false)
            }
        }
        println("Connection closed.\n")
    }

    def sendHttpResponse(Socket socket, String status, String contentType, byte[] content){
        val outStream = socket.getOutputStream()
        val out = new BufferedOutputStream(outStream)
        val printOut = new PrintWriter(out)
                
        printOut.println(status)
        printOut.println("Server: Xtend HTTP Server 1.0")
    	printOut.println("Date: " + new Date())
	    printOut.println("Content-type: " + contentType)
	    printOut.println("Content-length: " + content.size)
		printOut.println()
        printOut.flush()

        out.write(content)
        out.flush()

        outStream.close()
    }


    def sendHtmlResponse(Socket socket, String status, String title, String body){
        val html =
'''<HTML>
        <HEAD><TITLE>«title»</TITLE></HEAD>
        <BODY>
            «body»
        </BODY>
</HTML>'''

        socket.sendHttpResponse(status, "text/html", html.toString.getBytes("UTF-8"))
    }

	def String getFileExtension(File file){
		val dotPosition = file.name.lastIndexOf(".")
		return file.name.substring(dotPosition + 1) 	
	}	

    def sendFile(Socket socket, File file, String method, boolean isRetry /*= false*/) {
        val isDir = file.isDirectory()

        if (isDir && !isRetry){
            socket.sendFile(new File(file.path + File::separator + DEFAULT_FILE), method, true)
            return null
        }

        if (file.exists() && !isDir){
            val byte[] content = if (method == "GET") file.readBytes() else newArrayList() as byte[]
            val contentType = fileExtToContentType(file.fileExtension) 
            
            socket.sendHttpResponse("HTTP/1.0 200 OK", contentType, content)
            println("File " + file.path + " of type " + contentType + " returned.")
        }
        else{
            socket.sendHtmlResponse(
                    "HTTP/1.0 404 Not Found",
                    "File Not Found",
                    "<H2>404 File Not Found: " + file.getPath() + "</H2>")

            println("404 File Not Found: " + file)
        }
    }

    def byte[] readBytes(File file){
    	FileUtils::readFileToByteArray(file)
    }
    
    def String fileExtToContentType(String ext){
    	switch ext {
        case ext == "htm" || ext == "html" : "text/html"
        case ext == "gif" : "image/gif"
        case ext == "jpg" || ext == "jpeg" : "image/jpeg"
        case ext =="class" || ext == "jar" : "applicaton/octet-stream"
        default : "text/plain"
   		}
	}
}