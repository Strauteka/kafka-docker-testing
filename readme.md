# Testing kafka in localhost

## kafka-consumer:  
 - cd into folder
 - mvn clean install
 -  java -jar .\target\kafka-consumer-jar-with-dependencies.jar \<topic\> \<client-id\>  \<group-id\> 
 - - java -jar .\target\kafka-consumer-jar-with-dependencies.jar test-topic test-consumer test-group

## kafka-producer
- cd into folder
- mvn clean install
- java -jar .\target\kafka-producer-jar-with-dependencies.jar \<topic\> \<client-id\>  \<partition\> 
- - java -jar .\target\kafka-producer-jar-with-dependencies.jar test-topic test-producer 1


<strong>Set variable docker-compose.env => VAR_EXTERNAL_IP</strong>  
RUN ZOO:  
`docker-compose -f docker-compose.yml -p test --env-file docker-compose.env up`

CHECK BROKERS:  
`docker run -it --rm --network=host edenhill/kafkacat:1.6.0 -b localhost:9092 -L -X broker.address.family=v4`


INFO:  
https://github.com/wurstmeister/kafka-docker/issues/630

https://www.logicbig.com/tutorials/misc/kafka/admin-api-getting-started.html

https://kafka.apache.org/081/documentation.html

https://gist.github.com/devshawn/bf5d5afff02ea332d80fbe730e6d8e58

https://github.com/conduktor/kafka-stack-docker-compose