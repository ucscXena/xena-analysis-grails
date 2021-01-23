package org.xena.analysis

import grails.gorm.transactions.Transactional
//import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
//import org.grails.web.json.JSONObject
import org.springframework.scheduling.annotation.Scheduled
import java.text.SimpleDateFormat


@Slf4j
//@CompileStatic
@Transactional
class TpmAnalysisService {

  static lazyInit = false
//  static List<TpmGmtAnalysisJob> analysisServiceJobs = []
  final int MAX_JOB_SIZE =  1
  int counter = 0

  AnalysisService analysisService

  @Scheduled(fixedDelay = 60000L)
  void executeEveryTen() {
    log.info "Simple Job every 60 seconds :{}", new SimpleDateFormat("dd/M/yyyy hh:mm:ss").format(new Date())
  }

  @Scheduled(fixedDelay = 10000L)
  void checkJobQueue() {
    log.info("checking job queue: "+ counter)
    ++counter

    int jobsRunning = TpmGmtAnalysisJob.executeQuery("select count(*) from TpmGmtAnalysisJob t where t.runState = :runState",[runState:RunState.RUNNING])[0] as int
    log.debug("jobs running: ${jobsRunning} vs $MAX_JOB_SIZE")
    if(jobsRunning>=MAX_JOB_SIZE) {
      log.info("already have "+jobsRunning + " max allowed is $MAX_JOB_SIZE")
      return
    }
    int numJobsToRun = TpmGmtAnalysisJob.executeQuery("select count(*) from TpmGmtAnalysisJob t where t.runState = :runState",[runState:RunState.NOT_STARTED])[0] as int
    log.info("number of jobs to run $numJobsToRun")
//    TpmGmtAnalysisJob jobToRun = TpmGmtAnalysisJob.findByRunState(RunState.NOT_STARTED)
//    def foundJobToRun = TpmGmtAnalysisJob.createCriteria().list {
//      eq("runState",RunState.NOT_STARTED)
//      maxResults(MAX_JOB_SIZE)
//    }
    def foundJobToRun = TpmGmtAnalysisJob.executeQuery("select t from TpmGmtAnalysisJob t where t.runState = :runState",[runState:RunState.NOT_STARTED])
    log.debug("job to run: $foundJobToRun")
    if(foundJobToRun){
      TpmGmtAnalysisJob jobToRun = foundJobToRun.first()
      log.info "running job, $jobToRun.cohort.name and $jobToRun.gmt.name"
//      Promise p = task {
        analysisService.doBpaAnalysis2(jobToRun)
//      }
    }
    else{
      log.debug "No job found to run "
    }

  }

  void loadTpmForGmtFiles(Gmt gmt) {
    Cohort.all.each {
      Date now = new Date()
      TpmGmtAnalysisJob analysisServiceJob = new TpmGmtAnalysisJob()
      analysisServiceJob.gmt = gmt
      analysisServiceJob.cohort = it
      analysisServiceJob.createdDate = now
      analysisServiceJob.lastUpdated = now
//      analysisServiceJob.method = "BPA Gene Expression"
      analysisServiceJob.save(insert: true, failOnError: true)
//      analysisServiceJobs.add(analysisServiceJob)
    }
  }
}


