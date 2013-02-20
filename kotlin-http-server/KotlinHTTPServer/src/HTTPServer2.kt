package httpServer2
/**
 * Created with IntelliJ IDEA.
 * User: maples
 * Date: 15/01/2013
 * Time: 12:49
 */
import java.io.*
import java.net.*
import java.util.StringTokenizer
import java.util.Date
import java.util.concurrent.Executors

fun main(args : Array<String>)
{
    val PORT : Int = 8080

    val serverConnect = ServerSocket(PORT)

    System.out.println("\nListening for connections on port " + PORT + "...\n")

    val pool = Executors.newCachedThreadPool()

    while (true)
    {
        val socket = serverConnect.accept()
        pool.execute(runnable { process(socket) })
    }
}

val WEB_ROOT : File = File(".")
val DEFAULT_FILE : String = "index.html"
val lineSeparator = System.getProperty("line.separator")!!;

fun process(connect: Socket) : Unit {
    System.out.println("Connection opened. (" + Date() + ")")

    val input = BufferedReader(InputStreamReader(connect.getInputStream()!!))

    input use
    {
        val requestLine = input.readLine()
        if (requestLine != null)
        {
            val parse : StringTokenizer = StringTokenizer(requestLine)
            val method : String = parse.nextToken().toUpperCase()

            if (method != "GET" && method != "HEAD")
            {
                connect.sendHtmlResponse(
                      status = "HTTP/1.1 501 Not Implemented",
                      title = "Not Implemented",
                      body = "<H2>501 Not Implemented: " + method + " method.</H2>")

                System.out.println("501 Not Implemented: " + method + " method.")
            }
            else
            {
                val fileRequested = URLDecoder.decode(parse.nextToken(), "UTF-8")
                connect.sendFile(File(WEB_ROOT, fileRequested), method)
            }
        }
    }

    System.out.println("Connection closed.\n")
}

fun Socket.sendHttpResponse(status: String,
                                    contentType: String,
                                    content: ByteArray)
{
    val outStream = getOutputStream()!!
    val out = BufferedOutputStream(outStream)

    val header = array(status,
            "Server: Kotlin HTTP Server 1.0",
            "Date: " + Date(),
            "Content-type: " + contentType,
            "Content-length: " + content.size)
            .makeString(separator = lineSeparator, postfix = lineSeparator + lineSeparator)
            .getBytes()

    out.write(header);
    out.flush()

    out.write(content)
    out.flush()

    outStream.close()
}


private fun Socket.sendHtmlResponse(status: String,
                                    title : String,
                                    body : String)
{
    val html =
"""<HTML>
    <HEAD><TITLE>$title</TITLE></HEAD>
    <BODY>
        $body
    </BODY>
</HTML>"""

    this.sendHttpResponse(status, "text/html", html.toByteArray("UTF-8"))
}

fun Socket.sendFile(file: File, method: String, isRetry: Boolean = false) {
    val isDir = file.isDirectory()

    if (isDir && !isRetry)
    {
        sendFile(file = File(file.path + File.separator + DEFAULT_FILE),
                method = method,
                isRetry = true)
        return
    }

    if (file.exists() && !isDir)
    {
        val content = if (method == "GET") file.readBytes() else ByteArray(0)
        val contentType = fileExtToContentType(file.extension)

        sendHttpResponse(
                status = "HTTP/1.0 200 OK",
                contentType = contentType,
                content = content)

        System.out.println("File " + file.path + " of type " + contentType + " returned.")
    }
    else
    {
        sendHtmlResponse(
                status = "HTTP/1.0 404 Not Found",
                title = "File Not Found",
                body = "<H2>404 File Not Found: " + file.getPath() + "</H2>")

        System.out.println("404 File Not Found: " + file)
    }
}

fun fileExtToContentType(extension : String) : String = when {
    extension == "htm" || extension == "html" -> "text/html"
    extension == "gif" -> "image/gif"
    extension == "jpg" || extension == "jpeg" -> "image/jpeg"
    extension =="class" || extension == "jar" -> "applicaton/octet-stream"
    else -> "text/plain"
}