import java.net { Socket }
import java.io { File, BufferedReader, InputStreamReader, PrintWriter, BufferedOutputStream, FileInputStream }
import java.util { StringTokenizer, Date }
import java.lang { Runnable }
import ceylon.interop.java { createByteArray }

doc "My little HTTP server"
shared class HTTPServer(Socket socket) satisfies Runnable {
	
	shared File webRoot = File(".");
	shared String defaultFile = "index.html";
	
	
	shared actual void run() {
		value bufferedReader = BufferedReader(InputStreamReader(socket.inputStream));
		value printWriter = PrintWriter(socket.outputStream);
		value bufferedOutputStream = BufferedOutputStream(socket.outputStream);
		
		String? input = bufferedReader.readLine();
		
		if(!exists input){ 
		  return;
		}	
			
		value parse = StringTokenizer(input);
		value method = parse.nextToken().uppercased;
		variable String fileRequested := parse.nextToken().lowercased;
		
		// methods other than GET and HEAD are not implemented
		if (!method.equals("GET") && !method.equals("HEAD")) {
		  // send Not Implemented message to client
          printWriter.println("HTTP/1.0 501 Not Implemented");
          printWriter.println("Server: Java HTTP Server 1.0");
          printWriter.println("Date: " Date() "");
          printWriter.println("Content-Type: text/html");
          printWriter.println(); // blank line between headers and content
          printWriter.println("<HTML>");
          printWriter.println("<HEAD><TITLE>Not Implemented</TITLE></HEAD>");
          printWriter.println("<BODY>");
          printWriter.println("<H2>501 Not Implemented: " method " method.</H2>");
          printWriter.println("</BODY></HTML>");
          printWriter.flush();
          return;
		}
		
		if (fileRequested.endsWith("/")) {
			// append default file name to request
			 fileRequested += defaultFile;
		}
		
		// create file object
		value file = File(webRoot, fileRequested);
		// get length of file
		value fileLength = file.length();

		// get the file's MIME content type
		String content = getContentType(fileRequested);
		
		
		// if request is a GET, send the file content
		if (method.equals("GET")) {
		  Array<Integer> fileData = createByteArray(fileLength);
		  //Array<Integer> fileData = arrayOfSize { size = fileLength; element = 0; };
		  
	      value fileIn = FileInputStream(file);	
	      fileIn.read(fileData);
		
		  // send HTTP headers
		  printWriter.println("HTTP/1.0 200 OK");
		  printWriter.println("Server: Java HTTP Server 1.0");
		  printWriter.println("Date: " Date() "");
		  printWriter.println("Content-type: " content "");
		  printWriter.println("Content-length: " file.length() "");
		  printWriter.println(); // blank line between headers and content
		  printWriter.flush(); // flush character output stream buffer

		  bufferedOutputStream.write(fileData, 0, fileLength);
		  bufferedOutputStream.flush(); // flush binary output stream buffer
	    }
	}
	
	String getContentType(String fileRequested) {
		if (fileRequested.endsWith(".htm") || fileRequested.endsWith(".html")) {
			return "text/html";
		} else if (fileRequested.endsWith(".gif")) {
			return "image/gif";
		} else if (fileRequested.endsWith(".jpg")
				|| fileRequested.endsWith(".jpeg")) {
			return "image/jpeg";
		} else if (fileRequested.endsWith(".class")
				|| fileRequested.endsWith(".jar")) {
			return "applicaton/octet-stream";
		} else {
			return "text/plain";
		}
	}
	
} 





