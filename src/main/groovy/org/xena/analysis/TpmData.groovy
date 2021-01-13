package org.xena.analysis

class TpmData implements Serializable{

  private static final long serialVersionUID = -628789568975888036

  List<String> samples
  Map<String,List<String>> geneData = [:]
}
