

## Xena-Analysis-Grails

![Java CI](https://github.com/ucscXena/xena-analysis-grails/workflows/Java%20CI/badge.svg)

Analysis-server for [XenaGoWidget](https://github.com/ucscXena/XenaGoWidget).

Allows analysis of arbitrary GMT files and samples against existing cohorts.

## Setup

Currently no easy way to add, but setup Postgres to run locally with
no username / password and to create the database `xena-analysis`.

However, we can configure other database relatively easily by editing the `application.yml` file.




## Running

    ./gradlew run-app


## Testing

    ./gradlew build

