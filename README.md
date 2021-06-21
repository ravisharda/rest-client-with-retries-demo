# Java HTTP Client with Transparent Retries

This project demonstrates how JAX-RS client can be wrapped to provide transparent handling of retries when invoking HTTP requests. Handling retries transparently in the wrapper relieves the calling code from having to implement complex retry logic. 

## About the Source Code

The class [HttpApiClient](https://github.com/ravisharda/rest-client-with-retries-demo/blob/main/src/main/java/org/example/rs/http/retryingclient/HttpApiClient.java) wraps `javax.ws.rs.client.Client` and adds automatic retries on top of its functions. While it uses a JAX-RS Client internally, it can be used not only for REST API calls but also for other types of HTTP APIs like SOAP APIs. 

The wrapper performs retries transparently using one of the following retry configurations: 

* The wrapper's default retry config (which can be modified by the caller when instantiating the wrapper)
* The specified retry config for each HTTP operation

The wrapper uses [Resilience4j](https://resilience4j.readme.io/docs/retry) for retries. 

The [HttpApiClientTests](https://github.com/ravisharda/rest-client-with-retries-demo/blob/main/src/test/java/org/example/rs/http/retryingclient/HttpApiClientTests.java) class demos the usage of the wrapper. The tests use [WireMock](http://wiremock.org/) for running a REST API server that provides stubbed responses. 
Each test then uses the wrapper to demonstrate a unit of functionality. 

## Enhancement Backlog

1. Configuring URL and query parameters 
2. Automatic API pagination
3. HTTP POST-based requests
4. Setting additional request headers and client properties
5. Support for asynchronous API requests
