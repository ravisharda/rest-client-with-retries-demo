# Java REST Client with Transparent Retries: Demo

This project demonstrates how retries can be handled transparently when using Java JAX-RS client. 

The class `HttpClient` wraps `javax.ws.rs.client.Client` and performs retries transparently using one of these retry configs: 
* The default retry config in the `HttpClient` (which can be modified by the caller)
* The caller specified retry config

The `HttpClientTests` demos the usage of the wrapper. The tests use Wiremock to run a fake REST server and to provide stubbed responses. 
Each test then uses the wrapper to demonstrate a unit of functionality. 
