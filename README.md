# Java/Fixie HTTPS Demo

A simple example app showing how to use preemptive authentication when proxying HTTPs traffic via Fixie in Java.

[![Deploy](https://www.herokucdn.com/deploy/button.svg)](https://heroku.com/deploy)

## About the demo

The [Fixie](https://elements.heroku.com/addons/fixie) Heroku add-on provides the ability for apps to route their outbound requests via a static IP. It does this by providing a proxy server that is compatible with both HTTP and tunneled HTTPS traffic.

When using Java with the [Apache HTTP Client](https://hc.apache.org/httpcomponents-client-ga/) package, it is necessary to configure the library to perform [preemptive authentication](https://hc.apache.org/httpcomponents-client-ga/tutorial/html/authentication.html#d5e717) with the proxy.

Unfortunately, finding a fully working, up-to-date example that works with both HTTP *and* HTTPS is near impossible! Many of them will fail with an `org.apache.http.NoHttpResponseException` error.

The cause of the problem can be seen when examining the log output from the Apache HTTP client, for example:

```
DefaultHttpClientConnectionOperator - Connecting to velodrome.usefixie.com/52.202.116.113:80
DefaultHttpClientConnectionOperator - Connection established 192.168.1.32:52606<->52.202.116.113:80
headers - http-outgoing-0 >> CONNECT httpbin.org:443 HTTP/1.1
headers - http-outgoing-0 >> Host: httpbin.org:443
headers - http-outgoing-0 >> User-Agent: Apache-HttpClient/4.5.1 (Java/1.8.0_102)
DefaultManagedHttpClientConnection - http-outgoing-0: Close connection
DefaultManagedHttpClientConnection - http-outgoing-0: Shutdown connection
MainClientExec - Connection discarded
```

As this shows, when the attempt to `CONNECT` to the remote server is made, the `Proxy-Authorization` header that is needed to authenticate to Fixie, is not sent. Fixie immediately closes the connection, and no response is returned.

This demo app shows how the `HttpClientContext` class can be used to store preemptive authentication information and generate the correct `Proxy-Authorization` header for the Fixie proxy.

A sample of the log output from this app is shown below: note that it includes the correct header and therefore a tunnel via Fixie is successfully established.

```
DefaultHttpClientConnectionOperator - Connecting to velodrome.usefixie.com/52.202.116.113:80
DefaultHttpClientConnectionOperator - Connection established 192.168.1.32:53629<->52.202.116.113:80
headers - http-outgoing-0 >> CONNECT httpbin.org:443 HTTP/1.1
headers - http-outgoing-0 >> Host: httpbin.org:443
headers - http-outgoing-0 >> User-Agent: Apache-HttpClient/4.5.1 (Java/1.8.0_102)
headers - http-outgoing-0 >> Proxy-Authorization: Basic Zml4aWU6RzdCeE12V1ZYWHNLSUk2
headers - http-outgoing-0 << HTTP/1.0 200 OK
HttpAuthenticator - Authentication succeeded
ProxyAuthenticationStrategy - Caching 'basic' auth scheme for http://velodrome.usefixie.com:80
MainClientExec - Tunnel to target created.
```

**Note:** the code demonstrated in this app will also work for HTTP requests.

The code itself lives in the `showHttps` function in `src/main/java/Main.java` and it shows how to authenticate to the proxy and send a HTTPS request to `https://httpbin.org/ip`. When you run the run the demo app and click the link, you should see one of your Fixie IP addresses reported back.

### Dependencies

This demo has been built using version 4.5.2 of the Apache HTTP Client. If you are attempting to use a newer version and receiving errors, please [create an issue](https://github.com/robanderton/java-fixie-https-demo/issues/new).

The demo code **will not** work with older versions of the package!

## Running locally

Make sure you have [Java and Maven installed](https://devcenter.heroku.com/articles/getting-started-with-java#introduction) along with the [Heroku Toolbelt](https://toolbelt.heroku.com/).

```sh
$ git clone https://github.com/robanderton/java-fixie-https-demo.git
$ cd java-fixie-https-demo
$ mvn install
$ heroku local:start
```

The app should now be running on [localhost:5000](http://localhost:5000/).

**Note:** you will need to configure a `.env` file containing a `FIXIE_URL` value in order to run locally. See the Fixie [Local Setup](https://devcenter.heroku.com/articles/fixie#local-setup) documentation for more information.

## Deploying to Heroku

```sh
$ heroku create
$ heroku addons:create fixie:tricycle
$ git push heroku master
$ heroku open
```

## Thanks

Thanks to Sarb Billing for providing the fully working example that is the basis for this demo app.

Thanks also to [Joe Kutner](https://github.com/jkutner) for the [proxy-examples](https://github.com/kissaten/proxy-examples) app that I hacked around with when trying to get HTTPS to work.

And finally, the contributors to [this Stack Overflow](http://stackoverflow.com/questions/13288038/httpclient-4-2-2-and-proxy-with-username-password) discussion that helped immensely.
