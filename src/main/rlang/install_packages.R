# Title     : Install packages for docker
# Objective : Installes packages
# Created by: nathandunn
# Created on: 4/5/21

if (!requireNamespace("BiocManager", quietly = TRUE))
  install.packages("BiocManager")
BiocManager::install("viper")
install.packages("jsonlite")
install.packages("stringr")
install.packages("digest")
install.packages("R.utils")
