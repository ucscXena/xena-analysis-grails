

## Xena-Analysis-Grails

![Java CI](https://github.com/ucscXena/xena-analysis-grails/workflows/Java%20CI/badge.svg)

Analysis-server for [XenaGoWidget](https://github.com/ucscXena/XenaGoWidget).

Allows analysis of arbitrary GMT files and samples against existing cohorts.

## Setup

Currently no easy way to add, but setup Postgres to run locally with
no username / password and to create the database `xena-analysis`.

However, we can configure other database relatively easily by editing the `application.yml` file.


## Setup

- Install jdk8+

- Install Postgresql and configure in `application.yml`


To download all of the tpm files you need to run the unit tests:

    ./gradlew test-app 

Once downloaded the tests will be fast.

- Install R 4.0 +

Example for Ubuntu 16 (https://cran.r-project.org/bin/linux/ubuntu/README.html)

    sudo add-apt-repository 'deb https://cloud.r-project.org/bin/linux/ubuntu xenial-cran40/'
    sudo apt-get update
    sudo apt-get install r-base



- Install R dependencies

At the top-level directory enter the `R` console:

     if (!requireNamespace("BiocManager", quietly = TRUE))
        install.packages("BiocManager")
     BiocManager::install("viper")
     install.packages("jsonlite")
     install.packages("stringr")
     install.packages("digest")
     install.packages("R.utils")

Quit and save the workspace.

## Running

    ./gradlew run-app


## Testing

    ./gradlew build

