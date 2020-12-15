package org.xena.analysis

import org.grails.web.json.JSONArray
import org.grails.web.json.JSONObject

class OutputHandler {

  static printMemory(){
    println "max memory: ${Runtime.getRuntime().maxMemory() / 1.0E6}"
    println "total memory: ${Runtime.getRuntime().totalMemory() / 1.0E6}"
    println "free memory: ${Runtime.getRuntime().freeMemory() / 1.0E6}"
  }

  static File convertTsvFromFile(File inputfile) {

    JSONArray jsonArray = new JSONArray()
    JSONArray samplesJsonArray = new JSONArray()
    println "converting"
    inputfile.readLines().eachWithIndex {line , lineNumber->
      if(lineNumber>0 && line.trim().length()>0){
        List<String> entries = line.split("\\t")
        def obj = new JSONObject()
        obj.geneset = entries[0]
        obj.data = entries.subList(1, entries.size()) as List<Float>
        jsonArray.add(obj)
      }
      if(lineNumber==0){
        List<String> sampleList = line.split('\t')
        sampleList.subList(1, sampleList.size()).each {
          samplesJsonArray.add(it)
        }
      }
    }
    println "converted and returning"

    File tempOutputFile = File.createTempFile("output",".json")
    println "created file "
    println "delete on exit "
    def jsonObject = new JSONObject(
      [
        samples: samplesJsonArray
        , data : jsonArray
      ]
    )
    println "created object"
    printMemory()
    jsonArray = null
    samplesJsonArray = null
    inputfile = null
    System.gc()
    println "run GC"
    printMemory()


    tempOutputFile.write(jsonObject.toString())
    println "file is written"
    println tempOutputFile.size()
    return tempOutputFile
  }

  static JSONObject convertTsv(String tsvInput) {

    List<String> lines = tsvInput.split("\\n")
    println "# of lines ${lines.size()}"
    List<String> rawData = lines.subList(1, lines.size())
    println "# of raw data ${rawData.size()}"
    List<String> data = rawData.findAll({ d ->
      d.trim().length() > 0
    })
    println "trimmed data ${data.size()}"
    JSONArray jsonArray = new JSONArray()
    data.eachWithIndex { d, i ->
      List<String> entries = d.split("\\t")
      def obj = new JSONObject()
      obj.geneset = entries[0]
      obj.data = entries.subList(1, entries.size()) as List<Float>
//      if (i < 4) {
//        println "d: ${d}"
//        println "entries: ${entries.size()}"
//        println "geneset: ${entries[0]}"
//        println "data: ${obj.data}"
//      }
      jsonArray.add(obj)
    }

    List<String> sampleList = lines[0].split('\t')
    JSONArray samplesJsonArray = new JSONArray()
    sampleList.subList(1, sampleList.size()).each {
      samplesJsonArray.add(it)
    }

    return new JSONObject(
      [
        samples: samplesJsonArray
        , data : jsonArray
      ]
    )
  }



}
