# power_grid
Multi-Thread a Traversal to find the energization status of a power grid

Instructions
------------

1. Build it:

        mvn clean package

2. Copy target/power-grid-1.0-SNAPSHOT.jar to the plugins/ directory of your Neo4j server.

3. Download additional jars to the plugins/ directory of your Neo4j server.
   
        http://central.maven.org/maven2/org/roaringbitmap/RoaringBitmap/0.6.27/RoaringBitmap-0.6.27.jar

4. Configure Neo4j by adding a line to conf/neo4j.conf on version >= 3.0:

        dbms.unmanaged_extension_classes=net.geirina=/v1

5. Start Neo4j server.

6. Check that the extension is installed correctly over HTTP:

        :GET /v1/service/helloworld

7. Warm up the database: 

        :GET /v1/service/warmup

8. Run this command to add an index on Equipment(equipment_id):

        :POST /v1/schema/create

9. Run the command:

        :POST /v1/energization/ {"ids":["1"]}
        
10. Clear the results:
          
        :POST /v1/energization/clear

9. Try the SPI version:

        :POST /v1/energization/spi {"ids":["1"]}
        
10. Clear the results:
          
        :POST /v1/energization/clear
           
           
11. Try the streaming version:
           
        :POST /v1/energization/streaming {"ids":["1"]}           
        
12. Clear it again and try the multi-threaded version:
          
        :POST /v1/energization/clear
        :POST /v1/energization/multi {"ids":["1"]}
                           
13. Performance Testing:


        curl -u neo4j:swordfish -w "@format.txt" -o /dev/null -s -H "Content-Type: application/json" -X POST -d '{ "ids" : ["1"]}' http://localhost:7474/v1/energization
        curl -u neo4j:swordfish -H "Content-Type: application/json" -X POST http://localhost:7474/v1/energization/clear
        curl -u neo4j:swordfish -w "@format.txt" -o /dev/null -s -H "Content-Type: application/json" -X POST -d '{ "ids" : ["1"]}' http://localhost:7474/v1/energization/spi
        curl -u neo4j:swordfish -H "Content-Type: application/json" -X POST http://localhost:7474/v1/energization/clear
        curl -u neo4j:swordfish -w "@format.txt" -o /dev/null -s -H "Content-Type: application/json" -X POST -d '{ "ids" : ["1"]}' http://localhost:7474/v1/energization/streaming
        curl -u neo4j:swordfish -H "Content-Type: application/json" -X POST http://localhost:7474/v1/energization/clear
        curl -u neo4j:swordfish -w "@format.txt" -o /dev/null -s -H "Content-Type: application/json" -X POST -d '{ "ids" : ["1"]}' http://localhost:7474/v1/energization/multi
        