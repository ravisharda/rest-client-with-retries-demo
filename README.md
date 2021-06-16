# Java HTTP Client with Transparent Retries

This project demonstrates how JAX-RS client can be wrapped to provide transparent handling of retries when invoking HTTP requests. Handling retries transparently in the wrapper relieves the calling code from having to implement complex retry logic. 

## About the Source Code

The class [HttpClient](https://github.com/ravisharda/rest-client-with-retries-demo/blob/main/src/main/java/org/example/rs/http/retryingclient/HttpClient.java) wraps `javax.ws.rs.client.Client` and adds automatic retries on top of its functions. While it uses a JAX-RS Client internally, it can be used not only for REST API calls but also for other types of HTTP APIs like SOAP APIs. 

The wrapper performs retries transparently using one of the following retry configurations: 

* The wrapper's default retry config (which can be modified by the caller when instantiating the wrapper)
* The specified retry config for each HTTP operation

The wrapper uses [Resilience4j](https://resilience4j.readme.io/docs/retry) for retries. 

The [HttpClientTests](https://github.com/ravisharda/rest-client-with-retries-demo/blob/main/src/test/java/org/example/rs/http/retryingclient/HttpClientTests.java) class demos the usage of the wrapper. The tests use [WireMock](http://wiremock.org/) for running a REST API server that provides stubbed responses. 
Each test then uses the wrapper to demonstrate a unit of functionality. 

## Enhancement Backlog

1. Support for POST requests.
2. Support for additional request headers. Hint: Create an abstract class for requests with implementations for each method. 
3. Demonstration of use of call-specified retry config. 
4. Support for HTTP methods other than GET and POST. 
5. Support for modifying client properties other than connect timeout and read timeout. 
