package org.xena.analysis

import grails.testing.services.ServiceUnitTest
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
}

