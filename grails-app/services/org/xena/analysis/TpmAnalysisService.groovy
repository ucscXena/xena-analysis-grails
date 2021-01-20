package org.xena.analysis

import grails.async.Promise
import grails.gorm.transactions.Transactional
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
//import org.grails.web.json.JSONObject
import org.springframework.scheduling.annotation.Scheduled

import java.text.SimpleDateFormat

import static grails.async.Promises.task


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

    int jobsRunning = TpmGmtAnalysisJob.countByRunState(RunState.RUNNING)
    log.info("jobs running: ${jobsRunning} vs $MAX_JOB_SIZE")
    if(jobsRunning>=MAX_JOB_SIZE) {
      log.info("already have "+jobsRunning + " max allowed is $MAX_JOB_SIZE")
      return
    }
    int numJobsToRun = TpmGmtAnalysisJob.countByRunState(RunState.NOT_STARTED)
    log.info("number of jobs to run $numJobsToRun")
    TpmGmtAnalysisJob jobToRun = TpmGmtAnalysisJob.findByRunState(RunState.NOT_STARTED)
    log.info("job to run: $jobToRun")
    if(jobToRun){
      log.info "running job, $jobToRun.cohort.name and $jobToRun.gmt.name"

//      Promise p = task {
        analysisService.doBpaAnalysis2(jobToRun)
//      }

    }
    else{
      log.info "No job found to run "
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


