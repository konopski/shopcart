# ShopCart

##example curls

### login verification

    curl -v -XGET -HX-USER:lk localhost:7070/whoami

### add product to cart

    curl -HX-USER:kp -Haccept:application/json -XPUT localhost:7070/order -d "{\"product\":\"new one\", \"quantity\":5}"

### query cart

    curl -HX-USER:kp -XGET localhost:7070/order

### delete product

    curl -HX-USER:kp -Haccept:application/json -XDELETE localhost:7070/order -d "{\"product\":\"new one\"}"

### delete all products in cart

    curl -HX-USER:kp -XDELETE localhost:7070/order/all



Finatra requires either [maven](http://maven.apache.org/) or [sbt](http://www.scala-sbt.org/release/docs/Getting-Started/Setup.html) to build and run your app.

## SBT Instructions

### Runs your app on port 7070

    sbt run

### Testing

    sbt test

### Packaging (fatjar)

    sbt assembly


## Maven Instructions

### Runs your app on port 7070

    mvn scala:run

### Testing

    mvn test

### Packaging (fatjar)

    mvn package


## Heroku

### To put on heroku

    heroku create
    git push heroku master

### To run anywhere else

    java -jar target/*-0.0.1-SNAPSHOT-jar-with-dependencies.jar
