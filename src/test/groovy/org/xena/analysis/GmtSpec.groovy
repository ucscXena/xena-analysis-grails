package org.xena.analysis

import grails.converters.JSON
import grails.testing.gorm.DomainUnitTest
import org.grails.web.json.JSONArray
import org.grails.web.json.JSONObject
import spock.lang.Specification

class GmtSpec extends Specification implements DomainUnitTest<Gmt> {

    def setup() {
    }

    def cleanup() {
    }

    void "convert default BPA"() {
      given:
      File inputFile = new File("src/test/data/defaultBpaGeneSet.json")
      File outputFile = new File("src/test/data/defaultBpaGeneSet.gmt")
      outputFile.write("")
      JSONArray geneSetArray = JSON.parse(inputFile.text)
      println "# of gene sets ${geneSetArray.size()}"

      when:
      int count = 0
      for(def obj in geneSetArray){
//        println "processing ${obj.golabel}"
//        println "genes is ${obj.gene.size()}"
        List<String> lineList = new ArrayList<>()
        lineList.add(obj.golabel)
        lineList.add(obj.goid ?: "")
        for(def gene in obj.gene){
          lineList.add(gene)
        }
        String outputString = lineList.join("\t")+"\n"
//        outputFile.write(lineList.join("\t"))
//        outputFile.write("\n")
        outputFile.append(outputString)
        ++count
      }
      println "last gene set written: $count"


      then:
      assert count==geneSetArray.size()
//      assert outputFile.text.readLines().size()==geneSetArray.size()

    }
}
