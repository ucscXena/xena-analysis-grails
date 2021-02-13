#!/usr/bin/env groovy
import groovy.json.JsonSlurper

def file = new File("old_default_mapping.json")
def geneSetJson = new JsonSlurper().parse(file)

StringBuilder stringBuilder = new StringBuilder()


for(def geneSet in geneSetJson){
    if(geneSet.golabel.contains("Pancan")){
        stringBuilder.append(geneSet.golabel).append("\t").append(geneSet.goid?: "").append("\t")
        for(def gene in geneSet.gene){
            stringBuilder.append("\t").append(gene)
        }
        stringBuilder.append("\n")
    }
}


def outputFile = new File("pancan.gmt")
outputFile.write(stringBuilder.toString())
