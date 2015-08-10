# Tomcat-Redis-Cluster-Enabled-Session-Manager

Redis session manager is pluggable one. It uses to store sessions into Redis for easy distribution of HTTP Requests across a cluster of Tomcat servers. Sessions are implemented as as non-sticky i.e, each request is forwarded to any server in round-robin manner.
 
The HTTP Requests session setAttribute(name, value) method stores the session into Redis (must be Serializable) immediately and the session getAttribute(name) method request directly from Redis. Also, the inactive sessions has been removed based on the session time-out configuration.
 
It supports, both single redis master and redis cluster based on the redis.properties configuration.

Going forward, we no need to enable sticky session (JSESSIONID) in Load balancer.

## Supports:
   * Apache Tomcat 7


## Pre-requisite:
1. jedis-3.0.0-SNAPSHOT.jar
2. commons-pool2-2.2.jar
3. commons-logging-1.1.jar

##### Note: Download all the above three jars and move it into tomcat/lib directory



####Steps to be done,
1. Move the downloaded jar (RedisSessionManager.jar) to tomcat/lib directory
	* **$catalina.home/lib/RedisSessionManager.jar**
	
2. Add tomcat system property "catalina.home"
	* **catalina.home="TOMCAT_LOCATION"**

3. Extract downloaded jar (RedisSessionManager.jar) to configure redis credentials in redis.properties file and move the file to tomcat/conf directory
	* **tomcat/conf/redis.properties**

4. Add the below two lines in tomcat/conf/context.xml
	* **&#60;Valve className="com.r.tomcat.session.management.commons.SessionHandlerValve" &#47;&#62;**
	* **&#60;Manager className="com.r.tomcat.session.management.redis.RedisSessionManager" &#47;&#62;**

5. Verify the session expiration time in tomcat/conf/web.xml
	* **&#60;session-config&#62;**
	* 	**&#60;session-timeout&#62;70&#60;&#47;session-timeout&#62;**
	* **&#60;&#47;session-config&#62;**

###Note:
  * The Redis session manager supports, both single redis master and redis cluster based on the redis.properties configuration.
