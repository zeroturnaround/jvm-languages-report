import java.net { ServerSocket, Socket }
import java.lang { Thread }

shared Integer port = 8080;

void run(){
   ServerSocket server = ServerSocket(port);
   print("Listening for connections on port " port "...");
   
   while(true){
       Socket socket = server.accept();
       print("New client connection accepted!");
       HTTPServer httpServer = HTTPServer(socket);
       
       Thread threadRunner = Thread(httpServer);
       threadRunner.start(); 
   }
}
