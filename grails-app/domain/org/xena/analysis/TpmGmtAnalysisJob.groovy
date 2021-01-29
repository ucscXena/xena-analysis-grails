package org.xena.analysis

enum RunState {
  NOT_STARTED,
  RUNNING,
  ERROR,
  FINISHED
}

class TpmGmtAnalysisJob {

  Gmt gmt
  Cohort cohort
  RunState runState = RunState.NOT_STARTED
  Date lastUpdated
  Date createdDate
  String errorMessage

  static constraints = {
    errorMessage nullable: true
  }

}
