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
    def inputA = new JSONArray(new File("src/test/data/inputDataStats.json").text)
    assert inputA.length()==9
    assert inputA[0].length()==89

    when:
    def values = AnalysisService.getDataStatisticsPerGeneSet(inputA)

    then:
    assert values.size()==9
    assert values[0].mean == 4.211647650561798
    assert values[0].variance == 0.019968438525575474


  }

}

