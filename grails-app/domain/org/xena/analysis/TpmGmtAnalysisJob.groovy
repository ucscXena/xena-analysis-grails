package org.xena.analysis

enum RunState {
  NOT_STARTED,
  RUNNING,
  FINISHED
}

class TpmGmtAnalysisJob {

    static constraints = {
      result nullable: true
    }

  String method
  Gmt gmt
  Cohort cohort
  TpmGmtResult result
  RunState runState = RunState.NOT_STARTED

}
