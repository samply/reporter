# Change Log
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/)
and this project adheres to [Semantic Versioning](http://semver.org/).

## [2.0.0 - unreleased]
### Changed
- **Breaking:** Java 25 is required (Docker image eclipse-temurin:25-jre). Projects using the
  reporter library (e.g. dktk-reporter) must build and run on Java 25.
- Spring Boot 4.1.2 (Spring Framework 7, Jackson 3)
- springdoc-openapi 3.2.0 (api-docs are now OpenAPI 3.1.0)
- Apache POI 6.0.0, Guava 33.8.0, commons-csv 1.14.1, maven-model 3.9.16
- Maven Wrapper 3.9.16 (.mvn/ was missing from the repository)
- GitHub Actions updated, trivy-action pinned

### Removed
- SnakeYAML 2.0 workaround and springdoc-openapi-common 1.7.0

## [1.1.9 - 2024-04-11]
### Fixed
- Finalize report generation correctly
- Read files with configuration charset
- Write files with configuration charset

### Added
- Export expiration in days
- Export contact id
- Ignore template parameter
- Tomcat max-http-header-size
- Open API docs
- CreateWebclient in ExporterClient

### Changed
- FHIR_PATH to FHIR_SEARCH
- github build configuration
- converter for template

## [1.1.8 - 2023-10-9]
### Fixed
- Fetch script file for column scripts
- Double Quote in exports

### Changed
- Remove dktk reports (moved to dktk-reporter)

## [1.1.7 - 2023-10-9]
### Added
- File Path in scripts
- Ignore lines in script
- Two jar files (dependency + Spring Boot)

### Changed
- Rename FHIR_QUERY to FHIR_PATH
- Better error management in report generation

## [1.1.6 - 2023-10-2]
### Changed
- Move back to https://oss.sonatype.org/

## [1.1.5 - 2023-10-2]
### Changed
- Move to https://s01.oss.sonatype.org

## [1.1.4 - 2023-10-2]
### Added
- buildnumber-maven-plugin
- maven-source-plugin
- scm

## [1.1.3 - 2023-09-29]
### Changed
- Generate reporter-<version>.jar instead of reporter.jar

## [1.1.2 - 2023-09-29]
### Added
- Java Doc Plugin

## [1.1.1 - 2023-09-28]
### Added
- Nexus Staging Plugin
- Apache License in pom.xml

## [1.1.0 - 2023-09-28]
### Added
- Gitignore
- Dockerignore
- build.yml
- Dockerfile
- Exporter Client
- Download export
- Unzip files
- Templates
- Thymeleaf support
- Variables Replacer
- Exporter configuration in template
- Exporter template in quality report template (optional)
- Template in request
- Thymeleaf Engine
- Groovy-Templates Engine
- Report Generator
- Header format
- External Sheet Utils
- Report Meta Info File 
- Download Report
- Remove temporal files
- Report Status
- Context and CellContext for Engines
- Report examples
- Logs for report generation
- Multilevel comparable
- Request logs
- Exporter logs
- Export URL in Request
- HTTP Request and Connection Timeout
- HTTP TCP configuration
- Not validated
- CORS support
- Report Meta Info Paging
- Get all Template IDs
- Download report template
- Split Excel file if too large and zip
- Numeric and string cell values
- Running Report Manager
- Clear Logger Buffer
- Sort Source Paths in Context
- Manage Exception during report generation
- Custom Template ID
- Deployment in Maven Central Repository
- GPG Sign


### Changed
- Project name: quality-report-generator to reporter
- Bugfix: Exporter Template
- Bugfix: Patients pro Attribute
- Bugfix: Create sheet if it does not exist

### Fixed
- Intellij Warnings
- Retries of webclient managed by webclient retrywhen avoiding stackoverflow by recursive function
