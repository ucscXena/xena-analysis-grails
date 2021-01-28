package org.xena.analysis

import grails.async.Promise
import grails.async.PromiseList
import grails.gorm.transactions.Transactional
//import groovy.transform.CompileStatic
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
  final int MAX_JOB_SIZE =  5
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
    log.info("jobs running: ${jobsRunning} vs $MAX_JOB_SIZE")
    if(jobsRunning>=MAX_JOB_SIZE) {
      log.info("already have "+jobsRunning + " max allowed is $MAX_JOB_SIZE")
      return
    }
    int numJobsToRun = TpmGmtAnalysisJob.executeQuery("select count(*) from TpmGmtAnalysisJob t where t.runState = :runState",[runState:RunState.NOT_STARTED])[0] as int
    log.info("number of jobs to run $numJobsToRun")
    def foundJobsToRun = TpmGmtAnalysisJob.executeQuery("select t from TpmGmtAnalysisJob t where t.runState = :runState order by t.createdDate",[runState:RunState.NOT_STARTED,max:MAX_JOB_SIZE - jobsRunning])
    log.info("job to try and run run: ${foundJobsToRun.size()}")
    for(TpmGmtAnalysisJob jobToRun in foundJobsToRun){
//      TpmGmtAnalysisJob jobToRun = foundJobsToRun.first()
      log.info "running job, $jobToRun.cohort.name and $jobToRun.gmt.name"
      new Thread(){
        @Override
        void run() {
          super.run()
          log.info "running task , $jobToRun.cohort.name and $jobToRun.gmt.name"
          analysisService.setJobState(jobToRun,RunState.RUNNING)
          log.info "set job to running, $jobToRun.cohort.name and $jobToRun.gmt.name"
          analysisService.doBpaAnalysis2(jobToRun)
          log.info "did analysis, setting to finished, $jobToRun.cohort.name and $jobToRun.gmt.name"
          analysisService.setJobState(jobToRun,RunState.FINISHED)
          log.info "set to finished, $jobToRun.cohort.name and $jobToRun.gmt.name"
        }
      }.start()
      log.info "running thread, $jobToRun.cohort.name and $jobToRun.gmt.name"
//      Promise p = task {
//      }
//      println "adding promise to promise list: "
//      promiseList.add(p)
//      println "ADDED promise to promise list: "
    }
////    println "promise list size: ${promiseList.}"
//    promiseList.onComplete {
//      println "finished"
//    }
//    List promiseResultList = promiseList.get()
//    println promiseResultList
//    else{
//      log.debug "No job found to run "
//    }

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


