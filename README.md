# How to

#### Run the application
The application should be run as a SpringBootApplication. Below is a quick guide on how to do that via IntelliJ:
* Edit Configuration 
   * Add New Configuration (Spring Boot)
     * Change the **Main class** to **ing.assessment.INGAssessment**
       * Run the app.

#### Connect to the H2 database
Access the following url: **http://localhost:8080/h2-console/**
 * **Driver Class**: _**org.h2.Driver**_
 * **JDBC URL**: _**jdbc:h2:mem:testdb**_
 * **User Name**: _**sa**_
 * **Password**: **_leave empty_**

In order to access any endpoint, you first need to generate a Bearer Token 
using the localhost:8080/auth/generate endpoint (with no parameters for ease of use)
and include the generated token in the header of all subsequent requests.
Example: Authorization Bearer 'tokenExample'.

For testing purposes, PRODUCT table is already populated with several entries,
which you can check by running SELECT * FROM PRODUCT in /h2-console.

Also, for easier testing you can find unit tests for OrderService as well as
a complete Postman Collection along with variables.
You can find the postman collection at root/postman_collection

Few notes regarding the business logic:
- for orders > 500 ron, free delivery
- for orders > 1000 ron, free delivery + apply a 10% discount
- default delivery cost is 30

- if all products in the same location, delivery time is 2 days
- for each product in a different location, increase delivery time with 2 extra days
- a product is going to be delivered from the first location until it's stock is depleted.
Then, the next location is going to be used for delivery until it is also depleted,
and so on.

Possible improvements/optimizations:
- Prevent race conditions/concurrent modifications;
- Improve logs to display a Correlation-id generated from the start of a request using a filter, 
this way the logs are much more helpful, as they can be followed through all the steps of a request 
in the absence of another id;
- Separate layer for the DTO - completely decouple controller and service through an object mapper service 
that converts objects to DTOs and the other way around.


