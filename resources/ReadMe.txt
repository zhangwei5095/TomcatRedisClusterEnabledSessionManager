/**
 * Redis Cluster Enabled Session Manager
 *
 * Redis session manager is the pluggable one. It uses to store sessions into Redis for easy distribution of HTTP Requests across a cluster of Tomcat servers.
 *
 * @author Ranjith
 * @since 1.0
 */

Pre-requisite:
--------------
1. jedis-3.0.0-SNAPSHOT.jar
2. commons-pool2-2.2.jar
3. commons-logging-1.1.jar
Note: Download all the above three jars and move it into tomcat/lib directory


Steps to be done,
-----------------
1. Move the downloaded jar (RedisClusterEnabledSessionManager-1.0.jar) to tomcat/lib directory
	$catalina.home/lib/RedisSessionManager.jar
	
2. Add tomcat system property "catalina.home"
	catalina.home="TOMCAT_LOCATION"

3. Configure redis credentials in redis.properties file and move the file to tomcat/conf directory
	tomcat/conf/redis.properties

4. Add the below two lines in tomcat/conf/context.xml
     <Valve className="com.r.tomcat.session.management.commons.SessionHandlerValve" />
     <Manager className="com.r.tomcat.session.management.redis.RedisSessionManager" />

5. Verify the session expiration time in tomcat/conf/web.xml
	<session-config>
        <session-timeout>70</session-timeout>
    </session-config>

Note:
-----
  * The Redis session manager supports, both single redis master and redis cluster based on the redis.properties configuration.