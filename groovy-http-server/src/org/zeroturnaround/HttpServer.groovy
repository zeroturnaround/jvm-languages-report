package org.zeroturnaround

class HttpServer implements Runnable {
    final static webRoot = new File(".")
    final static defaultFile = "index.html"

    private final socket;

    HttpServer(socket) {
        this.socket = socket;
    }

    @Override
    void run() {
        println "Connection opened. (${new Date()})"

        def input = new BufferedReader(new InputStreamReader(socket.inputStream))
        def requestLine = input.readLine()
        if (requestLine != null) {
            def parse = new StringTokenizer(requestLine)
            def method = parse.nextToken().toUpperCase()

            if (method != "GET" && method != "HEAD") {
                sendHtmlResponse(
                        status: "HTTP/1.1 501 Not Implemented",
                        title: "Not Implemented",
                        body: "<h2>501 Not Implemented: ${method} method.</h2>")

                println "501 Not Implemented: ${method} method."
            } else {
                def fileRequested = new URLDecoder().decode(parse.nextToken(), "UTF-8")
                sendFile(new File(webRoot, fileRequested), method)
            }
        }

        println "Connection closed."
    }

    private def lineSeparator = System.getProperty("line.separator");

    private def sendHttpResponse(status, contentType, content) {
        def out = new BufferedOutputStream(socket.outputStream)

        def header = [status,
                "Server: Groovy HTTP Server 1.0",
                "Date: ${new Date()}",
                "Content-type: ${contentType}",
                "Content-length: ${content.length}"]
                .join(lineSeparator)
                .concat(lineSeparator + lineSeparator)
                .bytes

        out.write(header);
        out.flush()

        out.write(content)
        out.flush()

        socket.outputStream.close()
    }

    private def sendHtmlResponse(params) {
        def html = """\
<html>
    <head><title>${params.title}</title></head>
    <body>
        ${params.body}
    </body>
</html>"""

        sendHttpResponse(params.status, "text/html", html.getBytes("UTF-8"))
    }

    private def sendFile(file, method, isRetry = false) {
        def isDir = file.isDirectory()

        if (isDir && !isRetry) {
            sendFile(new File(file, defaultFile), method, true)
            return
        }

        if (file.exists() && !isDir) {
            def content = (method == "GET") ? file.readBytes() : new byte[0]

            def contentType = getContentType(file.name)

            sendHttpResponse(
                    "HTTP/1.0 200 OK",
                    contentType,
                    content)

            println "File ${file.path} of type ${contentType} returned."
        } else {
            sendHtmlResponse(
                    status: "HTTP/1.0 404 Not Found",
                    title: "File Not Found",
                    body: "<h2>404 File Not Found: ${file.name} </h2>")

            println "404 File Not Found: ${file.name}"
        }
    }

    private def getContentType(filename) {
        if (filename.endsWith(".htm") || filename.endsWith(".html")) {
            return "text/html"
        } else if (filename.endsWith(".gif")) {
            return "image/gif"
        } else if (filename.endsWith(".jpg")
                || filename.endsWith(".jpeg")) {
            return "image/jpeg"
        } else if (filename.endsWith(".class")
                || filename.endsWith(".jar")) {
            return "applicaton/octet-stream"
        } else {
            return "text/plain"
        }
    }


    public static void main(String[] args) {
        def port = 8080
        def server = new ServerSocket(port)

        println "Listening for connections on port ${port}..."

        while (true) {
            def socket = server.accept()
            def thread = new Thread(new HttpServer(socket))
            thread.start()
        }
    }
}
