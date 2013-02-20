package com.zt;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;
import org.apache.commons.io.FileUtils;
import org.eclipse.xtend2.lib.StringConcatenation;
import org.eclipse.xtext.xbase.lib.CollectionLiterals;
import org.eclipse.xtext.xbase.lib.Conversions;
import org.eclipse.xtext.xbase.lib.Exceptions;
import org.eclipse.xtext.xbase.lib.InputOutput;
import org.eclipse.xtext.xbase.lib.ObjectExtensions;

@SuppressWarnings("all")
public class HttpServer implements Runnable {
  private final String WEB_ROOT = ".";
  
  private final String DEFAULT_FILE = "index.html";
  
  private final Socket socket;
  
  public HttpServer(final Socket socket) {
    this.socket = socket;
  }
  
  public static void main(final String[] args) {
    try {
      final int PORT = 8080;
      ServerSocket _serverSocket = new ServerSocket(PORT);
      final ServerSocket serverConnect = _serverSocket;
      String _plus = ("\nListening for connections on port " + Integer.valueOf(PORT));
      String _plus_1 = (_plus + "...\n");
      InputOutput.<String>println(_plus_1);
      boolean _while = true;
      while (_while) {
        {
          final Socket socket = serverConnect.accept();
          HttpServer _httpServer = new HttpServer(socket);
          Thread _thread = new Thread(_httpServer);
          final Thread thread = _thread;
          thread.start();
        }
        _while = true;
      }
    } catch (Exception _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  public void run() {
    try {
      Date _date = new Date();
      String _plus = ("Connection opened. (" + _date);
      String _plus_1 = (_plus + ")");
      InputOutput.<String>println(_plus_1);
      InputStream _inputStream = this.socket.getInputStream();
      InputStreamReader _inputStreamReader = new InputStreamReader(_inputStream);
      BufferedReader _bufferedReader = new BufferedReader(_inputStreamReader);
      final BufferedReader input = _bufferedReader;
      final String requestLine = input.readLine();
      boolean _notEquals = ObjectExtensions.operator_notEquals(requestLine, null);
      if (_notEquals) {
        StringTokenizer _stringTokenizer = new StringTokenizer(requestLine);
        final StringTokenizer parse = _stringTokenizer;
        String _nextToken = parse.nextToken();
        final String method = _nextToken.toUpperCase();
        boolean _and = false;
        boolean _notEquals_1 = ObjectExtensions.operator_notEquals(method, "GET");
        if (!_notEquals_1) {
          _and = false;
        } else {
          boolean _notEquals_2 = ObjectExtensions.operator_notEquals(method, "HEAD");
          _and = (_notEquals_1 && _notEquals_2);
        }
        if (_and) {
          String _plus_2 = ("<H2>501 Not Implemented: " + method);
          String _plus_3 = (_plus_2 + " method.</H2>");
          this.sendHtmlResponse(this.socket, 
            "HTTP/1.1 501 Not Implemented", 
            "Not Implemented", _plus_3);
          String _plus_4 = ("501 Not Implemented: " + method);
          String _plus_5 = (_plus_4 + " method.");
          InputOutput.<String>println(_plus_5);
        } else {
          String _nextToken_1 = parse.nextToken();
          File _file = new File(this.WEB_ROOT, _nextToken_1);
          this.sendFile(this.socket, _file, method, false);
        }
      }
      InputOutput.<String>println("Connection closed.\n");
    } catch (Exception _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  public void sendHttpResponse(final Socket socket, final String status, final String contentType, final byte[] content) {
    try {
      final OutputStream outStream = socket.getOutputStream();
      BufferedOutputStream _bufferedOutputStream = new BufferedOutputStream(outStream);
      final BufferedOutputStream out = _bufferedOutputStream;
      PrintWriter _printWriter = new PrintWriter(out);
      final PrintWriter printOut = _printWriter;
      printOut.println(status);
      printOut.println("Server: Xtend HTTP Server 1.0");
      Date _date = new Date();
      String _plus = ("Date: " + _date);
      printOut.println(_plus);
      String _plus_1 = ("Content-type: " + contentType);
      printOut.println(_plus_1);
      int _size = ((List<Byte>)Conversions.doWrapArray(content)).size();
      String _plus_2 = ("Content-length: " + Integer.valueOf(_size));
      printOut.println(_plus_2);
      printOut.println();
      printOut.flush();
      out.write(content);
      out.flush();
      outStream.close();
    } catch (Exception _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  public void sendHtmlResponse(final Socket socket, final String status, final String title, final String body) {
    try {
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("<HTML>");
      _builder.newLine();
      _builder.append("        ");
      _builder.append("<HEAD><TITLE>");
      _builder.append(title, "        ");
      _builder.append("</TITLE></HEAD>");
      _builder.newLineIfNotEmpty();
      _builder.append("        ");
      _builder.append("<BODY>");
      _builder.newLine();
      _builder.append("            ");
      _builder.append(body, "            ");
      _builder.newLineIfNotEmpty();
      _builder.append("        ");
      _builder.append("</BODY>");
      _builder.newLine();
      _builder.append("</HTML>");
      final CharSequence html = _builder;
      String _string = html.toString();
      byte[] _bytes = _string.getBytes("UTF-8");
      this.sendHttpResponse(socket, status, "text/html", _bytes);
    } catch (Exception _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  public String getFileExtension(final File file) {
    String _name = file.getName();
    final int dotPosition = _name.lastIndexOf(".");
    String _name_1 = file.getName();
    int _plus = (dotPosition + 1);
    return _name_1.substring(_plus);
  }
  
  public String sendFile(final Socket socket, final File file, final String method, final boolean isRetry) {
    String _xblockexpression = null;
    {
      final boolean isDir = file.isDirectory();
      boolean _and = false;
      if (!isDir) {
        _and = false;
      } else {
        boolean _not = (!isRetry);
        _and = (isDir && _not);
      }
      if (_and) {
        String _path = file.getPath();
        String _plus = (_path + File.separator);
        String _plus_1 = (_plus + this.DEFAULT_FILE);
        File _file = new File(_plus_1);
        this.sendFile(socket, _file, method, true);
        return null;
      }
      String _xifexpression = null;
      boolean _and_1 = false;
      boolean _exists = file.exists();
      if (!_exists) {
        _and_1 = false;
      } else {
        boolean _not_1 = (!isDir);
        _and_1 = (_exists && _not_1);
      }
      if (_and_1) {
        String _xblockexpression_1 = null;
        {
          byte[] _xifexpression_1 = null;
          boolean _equals = ObjectExtensions.operator_equals(method, "GET");
          if (_equals) {
            byte[] _readBytes = this.readBytes(file);
            _xifexpression_1 = _readBytes;
          } else {
            ArrayList<Object> _newArrayList = CollectionLiterals.<Object>newArrayList();
            _xifexpression_1 = ((byte[]) ((byte[])Conversions.unwrapArray(_newArrayList, byte.class)));
          }
          final byte[] content = _xifexpression_1;
          String _fileExtension = this.getFileExtension(file);
          final String contentType = this.fileExtToContentType(_fileExtension);
          this.sendHttpResponse(socket, "HTTP/1.0 200 OK", contentType, content);
          String _path_1 = file.getPath();
          String _plus_2 = ("File " + _path_1);
          String _plus_3 = (_plus_2 + " of type ");
          String _plus_4 = (_plus_3 + contentType);
          String _plus_5 = (_plus_4 + " returned.");
          String _println = InputOutput.<String>println(_plus_5);
          _xblockexpression_1 = (_println);
        }
        _xifexpression = _xblockexpression_1;
      } else {
        String _xblockexpression_2 = null;
        {
          String _path_1 = file.getPath();
          String _plus_2 = ("<H2>404 File Not Found: " + _path_1);
          String _plus_3 = (_plus_2 + "</H2>");
          this.sendHtmlResponse(socket, 
            "HTTP/1.0 404 Not Found", 
            "File Not Found", _plus_3);
          String _plus_4 = ("404 File Not Found: " + file);
          String _println = InputOutput.<String>println(_plus_4);
          _xblockexpression_2 = (_println);
        }
        _xifexpression = _xblockexpression_2;
      }
      _xblockexpression = (_xifexpression);
    }
    return _xblockexpression;
  }
  
  public byte[] readBytes(final File file) {
    try {
      byte[] _readFileToByteArray = FileUtils.readFileToByteArray(file);
      return _readFileToByteArray;
    } catch (Exception _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  public String fileExtToContentType(final String ext) {
    String _switchResult = null;
    boolean _matched = false;
    if (!_matched) {
      boolean _or = false;
      boolean _equals = ObjectExtensions.operator_equals(ext, "htm");
      if (_equals) {
        _or = true;
      } else {
        boolean _equals_1 = ObjectExtensions.operator_equals(ext, "html");
        _or = (_equals || _equals_1);
      }
      if (_or) {
        _matched=true;
        _switchResult = "text/html";
      }
    }
    if (!_matched) {
      boolean _equals_2 = ObjectExtensions.operator_equals(ext, "gif");
      if (_equals_2) {
        _matched=true;
        _switchResult = "image/gif";
      }
    }
    if (!_matched) {
      boolean _or_1 = false;
      boolean _equals_3 = ObjectExtensions.operator_equals(ext, "jpg");
      if (_equals_3) {
        _or_1 = true;
      } else {
        boolean _equals_4 = ObjectExtensions.operator_equals(ext, "jpeg");
        _or_1 = (_equals_3 || _equals_4);
      }
      if (_or_1) {
        _matched=true;
        _switchResult = "image/jpeg";
      }
    }
    if (!_matched) {
      boolean _or_2 = false;
      boolean _equals_5 = ObjectExtensions.operator_equals(ext, "class");
      if (_equals_5) {
        _or_2 = true;
      } else {
        boolean _equals_6 = ObjectExtensions.operator_equals(ext, "jar");
        _or_2 = (_equals_5 || _equals_6);
      }
      if (_or_2) {
        _matched=true;
        _switchResult = "applicaton/octet-stream";
      }
    }
    if (!_matched) {
      _switchResult = "text/plain";
    }
    return _switchResult;
  }
}
