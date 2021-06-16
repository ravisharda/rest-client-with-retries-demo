# Java HTTP Client with Transparent Retries: Demo

This project demonstrates how JAX-RS client can be wrapped to provide transparent handling of retries when invoking HTTP requests. Handling retries transparently in the wrapper relieves the calling code from having to implement complex retry logic. 

## About the Source Code

The class [HttpClient](https://github.com/ravisharda/rest-client-with-retries-demo/blob/main/src/main/java/org/example/rs/http/retryingclient/HttpClient.java) wraps `javax.ws.rs.client.Client` and performs retries transparently using one of the following retry configs: 

* The wrapper's default retry config (which can be modified by the caller when instantiating the wrapper)
* The specified retry config

The [HttpClientTests](https://github.com/ravisharda/rest-client-with-retries-demo/blob/main/src/test/java/org/example/rs/http/retryingclient/HttpClientTests.java) class demos the usage of the wrapper. The tests use Wiremock to run a fake REST server and to provide stubbed responses. 
Each test then uses the wrapper to demonstrate a unit of functionality. 

## Future Enhancements

1. Support for POST requests
2. Support for additional request headers. Hint: Create an abstract class for requests with implementations for each method. 
3. Demonstration of use of call-specified retry config. 
4. Support for HTTP methods other than GET and POST. 
5. Support for modifying client properties other than connect timeout and read timeout. 
