/**
* Web worker: an object of this class executes in its own new thread
* to receive and respond to a single HTTP request. After the constructor
* the object executes on its "run" method, and leaves when it is done.
*
* One WebWorker object is only responsible for one client connection. 
* This code uses Java threads to parallelize the handling of clients:
* each WebWorker runs in its own thread. This means that you can essentially
* just think about what is happening on one client at a time, ignoring 
* the fact that the entirety of the webserver execution might be handling
* other clients, too. 
*
* This WebWorker class (i.e., an object of this class) is where all the
* client interaction is done. The "run()" method is the beginning -- think
* of it as the "main()" for a client interaction. It does three things in
* a row, invoking three methods in this class: it reads the incoming HTTP
* request; it writes out an HTTP header to begin its response, and then it
* writes out some HTML content for the response content. HTTP requests and
* responses are just lines of text (in a very particular format). 
*
**/

import java.net.Socket;
import java.lang.Runnable;
import java.io.*;
import java.util.Date;
import java.text.DateFormat;
import java.util.TimeZone;

public class WebWorker implements Runnable
{

private Socket socket;

/**
* Constructor: must have a valid open socket
**/
public WebWorker(Socket s)
{
   socket = s;
}

/**
* Worker thread starting point. Each worker handles just one HTTP 
* request and then returns, which destroys the thread. This method
* assumes that whoever created the worker created it with a valid
* open socket object.
**/
public void run()
{
   System.err.println("Handling connection...");
   try {
      InputStream  is = socket.getInputStream();
      OutputStream os = socket.getOutputStream();
      String url = "" + readHTTPRequest(is);
      String ContentType = "";
      if(url.contains(".jpg")) ContentType += "image/jpeg"; 
      else if(url.contains(".png")) ContentType += "image/png";
      else if(url.contains(".gif")) ContentType += "image/gif";
      else if(url.contains(".ico")) ContentType += "image/x-icon";
      else ContentType += "text/html";
      writeHTTPHeader(os, ContentType);
      writeContent(os, url, ContentType);
      os.flush();
      socket.close();
   } catch (Exception e) {
      System.err.println("Output error: "+e);
   }
   System.err.println("Done handling connection.");
   return;
}


/**
* Read the HTTP request header.
* @param is is the InputStream to read from
* @return is the full path of html file from url request
**/
private String readHTTPRequest(InputStream is)
{
   String path = "";
   String line;
   BufferedReader r = new BufferedReader(new InputStreamReader(is));
   while (true) {
      try {
         while (!r.ready()) Thread.sleep(1);
         line = r.readLine();
         if(line.contains("GET ")) {
            path = line.substring(4); // ignores port id
            for(int i = 0; i < path.length(); i++) {
               if(path.charAt(i) == ' ') {
                  path = path.substring(0, i);
                  break; // removes "HTTP/1.1" from file path
               }
            }  
         }
         System.err.println("Request line: ("+line+")");
         if (line.length()==0) break;
      } catch (Exception e) {
         System.err.println("Request error: "+e);
         break;
      }
   }
   // attach the path from home directory to current directory as a string
   return "" + System.getProperty("user.dir") + path;
}

/**
* Write the HTTP header lines to the client network connection.
* @param os is the OutputStream object to write to
* @param contentType is the string MIME content type (e.g. "text/html")
**/
private void writeHTTPHeader(OutputStream os, String contentType) throws Exception
{
   Date d = new Date();
   DateFormat df = DateFormat.getDateTimeInstance();
   df.setTimeZone(TimeZone.getTimeZone("GMT"));
   os.write("HTTP/1.1 200 OK\n".getBytes());
   os.write("Date: ".getBytes());
   os.write((df.format(d)).getBytes());
   os.write("\n".getBytes());
   os.write("Server: Alex's very own server\n".getBytes());
   os.write("Connection: close\n".getBytes());
   os.write("Content-Type: ".getBytes());
   os.write(contentType.getBytes());
   os.write("\n\n".getBytes()); // HTTP header ends with 2 newlines
   return;
}

/**
* Delivers the HTML file to the client network connection.
* @param os is the OutputStream object to write to
* @param path is the full path to file as a String
**/
private void writeContent(OutputStream os, String path, String contentType) throws Exception
{
   File fname;   // the html file to read from
   String content = ""; // the content of fname
   fname = new File(path);
   Date d = new Date();
   DateFormat df = DateFormat.getDateTimeInstance();
   BufferedReader buffer = null;
   try {
      buffer = new BufferedReader(new FileReader(fname));
   }catch (FileNotFoundException e) {
      os.write("<html>".getBytes());
      os.write("<body><h3>404 not found</h3></body>".getBytes());
      os.write("</html>".getBytes());
      throw e;
   }
   if(contentType.equals("text/html")) {
      while ((content = buffer.readLine()) != null) {
         if(content.contains("<cs371date>")) {
            // substitute <cs371date> with date tag
            content += df.format(d);
         }else if(content.contains("<cs371server>")) {
            // substitute <cs371server> with personalized server name tag
            content += "\ni-cant-believe-this-works.alex.gov";
         }
         os.write(content.getBytes());
         os.write("\n".getBytes());
      }
   }else if(contentType.contains("image")) {
      FileInputStream imgIn = new FileInputStream(fname);
      byte imgArr[] = new byte [(int) fname.length()]; // convert image to bytes
      imgIn.read(imgArr);
      DataOutputStream imgOut = new DataOutputStream(os);
      imgOut.write(imgArr);
   }
}

} // end class
