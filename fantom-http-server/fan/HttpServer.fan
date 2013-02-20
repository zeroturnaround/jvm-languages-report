//used pods
using util
using concurrent
//interop
using [java]fanx.interop::Interop

//java stuff
using [java]java.net::Socket
using [java]java.net::ServerSocket

class HttpServerMain : AbstractMain
{
  @Opt { help = "Http port"; aliases=["p"] }
  Int port := 8080
  
  const ActorPool actorPool := ActorPool { maxThreads = 32 }
  
  override Int run()
  {
    serverSocket := ServerSocket(port)
    log.info("Listening for connections on port: $port")
    try {
      while(true) {
        socket := serverSocket.accept
        a := ServerActor(actorPool)
        //wrap a mutable socket to sign that we know what are we doing 
        a.send(Unsafe(socket))
      }
    }
    catch (Err e) {
      serverSocket.close
      log.err("Fatal error: $e.msg", e)
      return 1
    }
    throw Err("Shouldn't be here, above is an infinite loop or return. Consult the author :)")
  }
}

const class ServerActor : Actor {
  const static ContentType contentTypes := ContentType()
  const static Str OUT := "serverActor.out"
  
  const static Uri defaultFile := Uri("index.html")
  
  static Log log() { Log.get("ServerActor") }
  
  new make(ActorPool p) : super(p) {}
  
  override Obj? receive(Obj? msg) {
    // Unsafe is just a wrapper, get the socket
    log.info("Accepted a socket: $DateTime.now")
    Socket socket := ((Unsafe) msg).val
    //get streams
    InStream in := Interop.toFan(socket.getInputStream)
    OutStream out := Interop.toFan(socket.getOutputStream)
    try {
      // store output stream
      Actor.locals[OUT] = out
      query := in.readLine.split()
      method := query[0]
      file := query[1]
      // serve it
      sendFile(File(Uri.fromStr("." + file)), method);
    }
    catch (ServerErr e) {
      log.err("Unable to send file for download: $e.msg")
      sendHtml(e.returnCode, e.status, e.title, e.msg)
    }
    finally {
      out.close
      //clear it from locals
      Actor.locals[OUT] = null
    }
    
    //this one doesn't matter, we're flushed our response for good
    return null;
  }
  
  Void sendFile(File? file, Str method) {
    log.info("Serving ($method) file: $file")
    if(!"GET".equals(method) && !"HEAD".equals(method)) {
      throw ServerErr(501, "Not Implemented", "Not Implemented", "<h2>501 Not Implemented: ${method} method.</h2>")
    }
    
    if(file == null) {
      throw ServerErr(400, "Bad Request", "Bad Request", "Cannot parse query part")
    }
    
    if(file.isDir) {
      return sendFile(file.plus(defaultFile, true), method)
    }
    if(!file.exists) {
      throw ServerErr(404, "Not Found", "File not found", "Did my best, still cannot find the file: $file, sorry")
    }
    
    content := Buf()
    if("GET".equals(method)) {
      content = file.readAllBuf 
    }
    
    sendHttp(200, "OK", getContentType(file.name), content)
  }
  
  Void sendHtml(Int returnCode, Str status, Str title, Str body) {
    html := "<html>
             <head><title>$title</title></head>
             <body>
               $body
             </body>
             </html>"
    sendHttp(returnCode, status, "text/html", html.toBuf)
  }
  
  Void sendHttp(Int returnCode, Str status, Str contentType, Buf content) {
        OutStream out := Actor.locals[OUT]
        // mind the newlines
        header := 
                "HTTP/1.1 $returnCode $status
                 Server: Fantom HTTP Server 1.0
                 Date: ${DateTime.now}
                 Content-type: ${contentType}
                 Content-length: ${content.size}
                 
                 ".toBuf

        out.writeBuf(header)
        out.writeBuf(content)
  }
  
  // maybe a dict would be more readable
  Str getContentType(Str filename) {
    contentTypes[filename]
  }
}

const class ServerErr : Err {
  const Int returnCode
  const Str status
  const Str title
  new make(Int returnCode, Str status, Str title, Str body) : super(body) {
    this.returnCode = returnCode
    this.status = status
    this.title = title
  }
}

const class ContentType {
  @Operator Str get(Str name) {
    if (name.endsWith(".htm") || name.endsWith(".html")) {
      return "text/html"
    } else if (name.endsWith(".gif")) {
      return "image/gif"
    } else if (name.endsWith(".jpg") || name.endsWith(".jpeg")) {
      return "image/jpeg"
    } else if (name.endsWith(".class") || name.endsWith(".jar")) {
      return "applicaton/octet-stream"
    } else {
      return "text/plain"
    }
  }
}