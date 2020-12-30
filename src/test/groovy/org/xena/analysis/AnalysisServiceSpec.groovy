package org.xena.analysis

import grails.testing.services.ServiceUnitTest
import org.grails.web.json.JSONArray
import org.grails.web.json.JSONObject
import spock.lang.Specification

class AnalysisServiceSpec extends Specification implements ServiceUnitTest<AnalysisService>{

    def setup() {
    }

    def cleanup() {
    }

    void "extract gene set names"() {
        expect:"gene set names to be extracted"
        println new File(".").absolutePath
        File file = new File("./src/test/data/resultA.json")

        println file.exists()
        def jsonObject = new JSONObject(file.text)
        def genes = jsonObject.data.collect{ it.geneset}
        assert genes.size()==9
        assert genes[0]=="Notch signaling (GO:0007219)"
        assert genes[8]=="Nucleotide excision repair (GO:0006281)"

//        def genes = service.getGeneSetNames(new JSONObject(file.text))
        println "genes: ${genes}"
    }

  void "convert string array to a float array"(){
    expect:
    def input = [ "0.22332","5.2323424","-2.23234324"]
    def output = input.collect{ Float.parseFloat(it) }
    println output
  }

  void "combine arrays"(){
    expect:
    def a = ["a","a1"]
    def b = ["b","b1"]
    def c = [a,b].flatten()
    assert c == ["a","a1","b","b1"]
  }

  void "convert mean map to values"(){
    given:
    def inputA = new JSONObject(new File("src/test/data/inputA.json").text)
    def inputB = new JSONObject(new File("src/test/data/inputB.json").text)
    assert inputA.samples.size() == 89
    assert inputB.samples.size() == 548
    assert inputA.data.size() == 9
    assert inputB.data.size() == 9
    assert inputA.data[0].data.size() == 89
    assert inputB.data[0].data.size() == 548

    when:
    def valuesA = AnalysisService.extractValuesByCohort(inputA)

    then:
    assert valuesA.size() == 9
    assert valuesA[0].size() == 89


    when:
    def values = AnalysisService.extractValues(inputA,inputB)

    then:
    assert values.size() == 2
    assert values[0].size() == 9
    assert values[1].size() == 9
    assert values[0][0].size() == 89
    assert values[1][0].size() == 548
  }

  void "get data statistics"(){

    given:
    def input = new JSONArray(new File("src/test/data/fullInputDataSet.json").text)
    assert input.length()==2
    assert input[0].length()==9
    assert input[0][0].length()==89
    assert input[1].length()==9
    assert input[1][0].length()==548

    when:
    def values = AnalysisService.getDataStatisticsPerGeneSet(input)

    then:
    assert values.size()==9
    assert Math.abs(values[0].mean - 4.023820362794348) < 0.00001
    assert Math.abs(values[0].variance - 0.0464846114492264) < 0.00001
    assert Math.abs(values[5].mean - 2.0414515651491367) < 0.00001
    assert Math.abs(values[5].variance - 0.05060722132448916) < 0.00001
    assert Math.abs(values[8].mean - 4.231456808634223) < 0.00001
    assert Math.abs(values[8].variance - 0.012692699004756416) < 0.00001

  }

  void "small data statistics"(){

    given:
    def input = new JSONArray(new File("src/test/data/smallInputData.json").text)
    assert input.length()==2
    assert input[0].length()==2
    assert input[0][0].length()==3
    assert input[1].length()==2
    assert input[1][0].length()==3

    when:
    def values0 = AnalysisService.getValuesForIndex(input,0)
    def values1 = AnalysisService.getValuesForIndex(input,1)
    println "values 0 $values0"
    println "values 1 $values1"

    then:
    assert values0==[3,4,5,7,-2,3]
    assert values1==[-1,-2,-3,-8,-9,-10]

    when:
    def values = AnalysisService.getDataStatisticsPerGeneSet(input)
    println values

    then:
    assert values.size()==2
    assert Math.abs(values[0].mean - (3 + 4 + 5 + 7 -2 +3 ) / 6.0) < 0.01
    assert Math.abs(values[0].variance - 9.06666666) < 0.01
    assert Math.abs(values[1].mean - (-1-2-3 -8-9-10) / 6.0) < 0.01
    assert Math.abs(values[1].variance - 15.5) < 0.01

  }

  void "get values for index"(){
    given:
    def input = new JSONArray(new File("src/test/data/fullInputDataSet.json").text)
//    def inputValues = [inputValuesA,inputValuesB]

    when:
    def values = AnalysisService.getValuesForIndex(input,0)

    then:
    assert values.size() == 89 + 548

  }

  void "input values"(){
    expect:
    def input = new JSONArray(new File("src/test/data/inputDataStats.json").text)
    def dataStatistics = new JSONArray(new File("src/test/data/dataDats.json").text) as List
    def values = AnalysisService.getZSampleScores(input,dataStatistics)
//    println "output values"
//    println values
    assert input.size()==9
    assert values.size()==9
    assert input[0].size()==89
    assert values[0].size()==89
    assert Math.abs(values[0][0]+15.37768369837939) < 0.0001
    assert Math.abs(values[0][1]+33.00384044872373) < 0.0001
    assert Math.abs(values[0][88]+32.813116629986624) < 0.0001

  }

  void "calc z pathway scores"(){
    expect:
    def input = new JSONArray(new File("src/test/data/inputPathwaySampleScores.json").text) as List
    def values = AnalysisService.getZPathwayScoresForCohort(input)
    assert values.size()==50
  }

  void "sample z-scores"(){

    expect:
    def input = new JSONArray(new File("src/test/data/sampleZScores.json").text)
    assert input.size()==2
    assert input[0].size()==50
    assert input[0][0].size()==89
    assert input[1].size()==50
    assert input[1][0].size()==548
    def values = AnalysisService.getZPathwayScores(input)
    assert values.size()==2
    assert values[0].size()==50
    assert values[1].size()==50

  }

  void "handle gmt data"(){

    expect:
    def gmtData = new File("src/test/data/gmtData.gmt").text
    def meanMap = new JSONObject(new File("src/test/data/meanMap.json").text) as Map
    JSONArray outputArray = AnalysisService.generateResult(gmtData,meanMap)
    assert outputArray.length()==9


  }

}

