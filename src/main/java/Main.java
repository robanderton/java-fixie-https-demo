import org.apache.http.*;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URL;

public class Main extends HttpServlet {
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    if (req.getRequestURI().endsWith("/connect")) {
      showHttps(req,resp);
    } else {
      resp.getWriter().print("<p><a href='/connect'>Connect using HTTPS</a></p>");
    }
  }

  private void showHttps(HttpServletRequest request, HttpServletResponse resp)
      throws ServletException, IOException {

    // Extract proxy information from the config var
    final URL proxyUrl = new URL(System.getenv("FIXIE_URL"));
    final String proxyUserInfo = proxyUrl.getUserInfo();
    final String proxyUsername = proxyUserInfo.substring(0, proxyUserInfo.indexOf(':'));
    final String proxyPassword = proxyUserInfo.substring(proxyUserInfo.indexOf(':') + 1);

    final HttpHost proxyHost = new HttpHost(
      proxyUrl.getHost(),
      proxyUrl.getPort(),
      proxyUrl.getProtocol());

    // Configure the HTTP client to use the proxy
    final CloseableHttpClient httpClient = HttpClientBuilder.create()
      .setProxy(proxyHost)
      .build();

    try {
      // Initialise credentials for the proxy
      final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
      credentialsProvider.setCredentials(
        new AuthScope(proxyHost.getHostName(), proxyHost.getPort()),
        new UsernamePasswordCredentials(proxyUsername, proxyPassword));

      // Configure preemptive authentication to the proxy
      final BasicScheme basicScheme = new BasicScheme();
      basicScheme.processChallenge(
        new BasicHeader(HttpHeaders.PROXY_AUTHENTICATE, "Basic "));

      final AuthCache authCache = new BasicAuthCache();
      authCache.put(proxyHost, basicScheme);

      // Configure context to perform preemptive authentication
      final HttpClientContext clientContext = HttpClientContext.create();
      clientContext.setCredentialsProvider(credentialsProvider);
      clientContext.setAuthCache(authCache);

      final HttpHost targetHost = new HttpHost("httpbin.org", 443, "https");
      final HttpRequest targetRequest = new HttpGet("/ip");

      System.out.println("Executing request to " + targetHost + " via "+ proxyHost);

      // Send the HTTP request using the authenticated context
      HttpResponse targetResponse = httpClient.execute(
        targetHost,
        targetRequest,
        clientContext);

      HttpEntity entity = targetResponse.getEntity();

      System.out.println("- Fixie HTTPS --------------------------");
      System.out.println(targetResponse.getStatusLine());
      Header[] headers = targetResponse.getAllHeaders();
      for (int i = 0; i < headers.length; i++) {
          System.out.println(headers[i]);
      }
      System.out.println("----------------------------------------");

      resp.getWriter().print("Your IP is: ");
      resp.getWriter().print(EntityUtils.toString(entity));
    } catch (Exception e) {
      resp.getWriter().println("There was an error: ");
      resp.getWriter().println(e);
    } finally {
      httpClient.close();
    }
  }

  public static void main(String[] args) throws Exception{
    Server server = new Server(Integer.valueOf(System.getenv("PORT")));
    ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
    context.setContextPath("/");
    server.setHandler(context);
    context.addServlet(new ServletHolder(new Main()),"/*");
    server.start();
    server.join();
  }

}
