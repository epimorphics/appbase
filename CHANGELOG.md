# Changelog

All notable changes to this project from 2026-03-11 onward will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [4.0.8] - 2026-09-03

* Update appbase 4.0.3 to 4.0.4 to address CVEs
* Update tomcat 11.0.22 to 11.0.25 to address CVEs
* Update org.apache.thrift:libthrift 0.23.0 to 0.24.0 to address CVEs

## [4.0.7] - 2026-08-19

* Update micrometer-core to 1.15.12
* Update lib to 4.0.3 to pull in httpclient5, httpcore5-h2 updates
* Update jetty to 12.1.10

## [4.0.6] - 2026-07-09

* Update shiro-core and shiro-web to 2.2.1
* Update transitive dependency on jackson-database

## [4.0.5] - 2026-05-27

* pass incoming x-request-id header to remote sparql source, obtained via MDC `request_id` parameter

## [4.0.4] - 2026-05-19

### Security

* Update tomcat to 11.0.21 to address CVEs

## [4.0.3] - 2026-05-11

### Security

* Update transitive dependency on apache.org.thrift:libthrift to 0.23.0 to address CVE.
* Update commons-lang3 to 3.20.0

## [4.0.2] - 2026-04-28

### Security

* Update transitive dependency on org.bouncycastle:bcprov-jdk18on to 1.84 to address dependabot-reported vulnerabilities.

## [4.0.1] - 2026-03-24

### Security

* Bump Tomcat 11.0.14 to 11.0.18
* Update transitive dependencies org.apache.shiro:shiro-core and org.apache.shiro:shiro-web 2.0.5 to 2.1.0
* Update transitive dependencies org.eclipse.jetty.ee10:jetty-ee10-servlet, org.eclipse.jetty.ee10:jetty-ee10-servlets and org.eclipse.jetty:jetty-security 12.1.1 to 12.1.7
