#!/usr/bin/env groovy
scriptDir = new File(getClass().protectionDomain.codeSource.location.path).parent
//evaluate(new File("${scriptDir}/convert_gmt_file.groovy"))

def cli = new CliBuilder(usage: 'convert_gmt_file.groovy <options>')
cli.setStopAtNonOption(true)
cli.inputfile('TSV file of GMT ending in .gmt', required: true, args: 1)
//cli.ourputfile('converted output TSV file of GMT', required: false, args: 1)
//cli.mappingfile('gene mappings', required: false, args: 1)
//cli.output('Admin password', required: false, args: 1)
//cli.password('Admin username', required: false, args: 1)

options = cli.parse(args)
String inputFileName = options.inputfile
println "input file path: '${inputFileName}'"

assert inputFileName.endsWith(".gmt")

File inputFile = new File(inputFileName)

String outputFileName = inputFileName.substring(0,inputFileName.length()-4)+"_converted.gmt"

// delete the file
File outputFile = new File(outputFileName)
outputFile.write("")

println outputFile.absolutePath

File mappingFile = new File("${scriptDir}/uniprot_mapping.tsv")

assert mappingFile.exists()
assert inputFile.exists()

Map<String,String> uniprotMap = new HashMap<>()
mappingFile.splitEachLine("\t"){
  uniprotMap.put(it[0],it[1])
}

println "uniprot map size: ${uniprotMap.size()}"

def handleRawGene(String input,StringBuilder stringBuilder){
  if(input.contains("%MF%")){
    String[] values = input.split("%MF%")
    stringBuilder.append(values[0]).append("\t").append(values[1])
  }
  else
  if(input.contains("%BP%")){
    String[] values = input.split("%BP%")
    stringBuilder.append(values[0]).append("\t").append(values[1])
  }
  else
  if(input.contains("%CC%")){
    String[] values = input.split("%CC%")
    stringBuilder.append(values[0]).append("\t").append(values[1])
  }
  else{
    stringBuilder.append(input)
  }
}

StringBuilder stringBuilder = new StringBuilder()
inputFile.splitEachLine("\t"){ List<String> stringList ->
  handleRawGene(stringList[0],stringBuilder)
  stringList.each {String it ->
    String key = it.split(":")[1]
    String value = uniprotMap.containsKey(key) ? uniprotMap.get(key) : null
    if(value!=null){
      stringBuilder.append("\t")
      stringBuilder.append(value)
    }
  }
  stringBuilder.append("\n")
}

outputFile.write(stringBuilder.toString())

