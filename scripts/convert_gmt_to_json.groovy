#!/usr/bin/env groovy
import groovy.json.JsonBuilder
import groovy.json.JsonOutput

def file = new File("9606-go_tcga-bp-all_converted.gmt")

//def builder = new JsonBuilder()

StringBuilder stringBuilder =new StringBuilder()
List entries = []
file.splitEachLine("\t"){ List<String> columns ->
    Map map = new HashMap([golabel:columns[0],goid:columns[1],gene:columns.subList(2,columns.size())])
    entries.add(map)
//    def output = JsonOutput.toJson(map)
//    println output
//    stringBuilder.append(output)
}


def outputFile = new File("tgac.json")
outputFile.write(JsonOutput.toJson(entries))
