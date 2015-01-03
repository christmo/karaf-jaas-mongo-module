Karaf JAAS Mongo LoginModule
=======================

A Karaf JAAS module using mongodb as the user and roles database.


## Installation 

Installation is quite simple, the only 2 modules to install are the mongo driver itself and the mongo jaas login module.

```sh

# install mongo driver
karaf@root> osgi:install -s mvn:org.mongodb/mongo-java-driver/2.12.0
Bundle ID: 213

# ensure mongo driver loads early in the process, say at startlevel 30
karaf@root> osgi:bundle-level --force 213 30


# install the mongo jaas login module
karaf@root> osgi:install -s mvn:org.apache.karaf.jaas/jaas-mongo-module/1.0.0
Bundle ID: 214

# ensure mongo jaas login module loads after the mongo driver, say at startlevel 31
karaf@root> osgi:bundle-level --force 214 31

```

## Usage

A working example for a very simple rest client can be found in the mongo-auth-test blueprint project.

1. configure a new jaas realm (see `jaas-mongo.xml` in example):

```xml
<?xml version="1.0" encoding="UTF-8"?>
<blueprint xmlns="http://www.osgi.org/xmlns/blueprint/v1.0.0"
           xmlns:ext="http://aries.apache.org/blueprint/xmlns/blueprint-ext/v1.0.0"
           xmlns:jaas="http://karaf.apache.org/xmlns/jaas/v1.0.0"
           xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
           xsi:schemaLocation="
           http://www.osgi.org/xmlns/blueprint/v1.0.0 http://www.osgi.org/xmlns/blueprint/v1.0.0/blueprint.xsd
           http://aries.apache.org/blueprint/xmlns/blueprint-ext/v1.0.0 http://aries.apache.org/schemas/blueprint-ext/blueprint-ext.xsd
           http://karaf.apache.org/xmlns/jaas/v1.0.0 http://karaf.apache.org/xmlns/jaas/v1.0.0">

  <type-converters>
    <!-- ueber important, without this one the jass config is minced on windows -->
    <bean class="org.apache.karaf.jaas.modules.properties.PropertiesConverter"/>
  </type-converters>

  <!-- Bean to allow the $[karaf.base] property to be correctly resolved -->
  <ext:property-placeholder placeholder-prefix="$[" placeholder-suffix="]"/>

  <jaas:config name="mongo" rank="0">
    <jaas:module className="org.apache.karaf.jaas.modules.mongo.MongoLoginModule" flags="required">
      mongo.db.url = localhost:27017
      mongo.db.name = SomeSecureDB
      mongo.user.attributes = email,phone
      debug = true
    </jaas:module>
  </jaas:config>

</blueprint>
```

2. use the new jass realm in your web setup, e.g. the below is to wire CXF JAX-RS together:

```xml
<?xml version="1.0"?>
<blueprint xmlns="http://www.osgi.org/xmlns/blueprint/v1.0.0"
           xmlns:cxf="http://cxf.apache.org/blueprint/core"
           xmlns:jaxrs="http://cxf.apache.org/blueprint/jaxrs"
           xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
           xsi:schemaLocation="
           http://www.osgi.org/xmlns/blueprint/v1.0.0 http://www.osgi.org/xmlns/blueprint/v1.0.0/blueprint.xsd
           http://cxf.apache.org/blueprint/core http://cxf.apache.org/schemas/blueprint/core.xsd
           http://cxf.apache.org/blueprint/jaxrs http://cxf.apache.org/schemas/blueprint/jaxrs.xsd">

  <bean id="authenticationFilter" class="org.apache.cxf.jaxrs.security.JAASAuthenticationFilter">
    <!-- Name of the JAAS Context -->
    <property name="contextName" value="mongo"/>
    <property name="realmName" value="TEST Co"/>
    <!-- Hint to the filter on how to have Principals representing users and roles separated while initializing a SecurityContext -->
    <property name="roleClassifier" value="GroupPrincipal"/>
    <property name="roleClassifierType" value="classname"/>
  </bean>

  <jaxrs:server id="echoResource" address="/rest/echo">
    <jaxrs:serviceBeans>
      <bean class="org.apache.karaf.jaas.modules.mongo.test.EchoServiceImpl"/>
    </jaxrs:serviceBeans>
    <jaxrs:providers>
      <ref component-id="authenticationFilter"/>
    </jaxrs:providers>
  </jaxrs:server>

  </blueprint>
```

## TODO List

1. configurable password / certificate database login
2. configurable userid/password attribute names
3. improve error handling and reporting
4. write better unit tests
