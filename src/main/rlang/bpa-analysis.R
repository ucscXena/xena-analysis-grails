library(viper)
library(jsonlite)
library(stringr)
library(digest)
suppressMessages(library(R.utils))

# transform_tpm_to_gene_set_activity.R

# Usage:
# Rscript transform_tpm_to_gene_set_activity.R <genesets.gmt> <expression_logtpm.tsv> <outfile.tsv> ["BPA"]

# Inputs:
# genesets.gmt: Filepath to the file containing the gene sets in .gmt file format
# expression_logtpm.tsv: Filepath to the file containing gene expression data in log-transformed TPM, genes x samples
# outfile.tsv: Filepath to write output to
# one of: ["BPA"]: optional definition of gene set transformation method, default: "BPA"
# Output:
# gene set activity matrix, gene sets x samples, in .tsv format


# method "BPA" requires R Bioconductor package 'viper'
# to install 'viper' run the following commands
# if (!requireNamespace("BiocManager", quietly = TRUE))
#   install.packages("BiocManager")
# BiocManager::install("viper")


# Inputs for testing locally
#geneset_gmt_filepath <- "./Xena_manual_pathways.gmt"
#tmp_expr_data_filepath <- "./TCGA-CHOL_logtpm_forTesting.tsv"
#outfile <- "./test_outfile.tsv"
#analysis_method <- "BPA"

# get command line input
#args <- commandArgs(trailingOnly=TRUE)
#geneset_gmt_filepath <- args[1]
#tmp_expr_data_filepath <- args[2]
#outfile <- args[3]


#curl -v -F tpmdata=@test-data/TCGA-CHOL_logtpm_forTesting.tsv -F gmtdata=@test-data/Xena_manual_pathways.gmt http://localhost:8000/bpa_analysis
#* @post /bpa_analysis
bpa_analysis <- function(req){
  print('doing bpa_analysis')
  formContents <- Rook::Multipart$parse(req)
  input <- formContents$input

  outputFileName <- "output.tsv"
  if(!is.null(input) && input  == 'text'){
    print('handling web text input')

    # let's be lazy and just write the input text to a file and read that
    gmtFileName <- tempfile()
    #decodedGmtData <- gsub("\t","  ",URLdecode(formContents$gmtdata))
    decodedGmtData <- URLdecode(formContents$gmtdata)
    write(decodedGmtData,file=gmtFileName)

    tpmFileName <-  paste(formContents$tpmname,".tpm",sep="",collapse="")
    tpmFileNameCompress <- paste(tpmFileName,".gz",sep="",collapse="")
    gmtDigest <- digest(formContents$gmtdata, "md5", serialize = FALSE)
    outputFileName <- paste("output-",formContents$tpmname,gmtDigest,".tsv",sep="",collapse="")

    # TODO if this file analysis file exists, just return it without doing the analysis

    print("output file name")
    print(outputFileName)
    if(!file.exists(outputFileName)){
      if(!file.exists(tpmFileName)){
        input_url <- URLdecode(formContents$tpmurl)
        download.file(input_url, tpmFileNameCompress)
        gunzip(tpmFileNameCompress)
      }
      # NOTE: we'll have to do two of these and return two of these, or maybe we submit two analyses . . .
      print("doing analysis")
      # write an empty file out
      outputFile<-file(outputFileName)
      write("",outputFile)
      do_bpa_analysis(gmtFileName,tpmFileName,outputFileName)
      print("DID analysis")

    }
    else{
      print("file exists, returning ")
      print(outputFileName)
      outputFile<-file(outputFileName)
    }

  }
  else{
    print('handling file input')
    # write an empty file out
    outputFile<-file(outputFileName)
    write("",outputFile)
    do_bpa_analysis(formContents$gmtdata$tempfile,formContents$tpmdata$tempfile,outputFileName)
  }
  outputText <- read.csv(outputFile, sep = "\t")
  #print(outputText)
  list(outputText)
}

do_bpa_analysis <- function(geneset_gmt_filepath,tmp_expr_data_filepath,outfile){

  # only load viper package if method 'BPA' is used

  # read in pathways and bring into right format for viper aREA function

  print("gmt filepath")
  print(geneset_gmt_filepath)

  print("tpm filepath")
  print(tmp_expr_data_filepath)

  pws_list <- list()
  pws <- readLines(geneset_gmt_filepath)
  for(line in pws){
    linesplit <- strsplit(line,split="\t",fixed = T)[[1]]
    gs_name <- linesplit[1]
    gs_description <- linesplit[2]
    if(!is.na(gs_description) &&  gs_description != ""){
      gs_name <- paste0(gs_name," (",gs_description,")")
    }
    gs_genes <- linesplit[3:length(linesplit),drop = F]

    likelihood <- rep(1,times=length(gs_genes))
    tfmode <- rep(1,times=length(gs_genes))
    names(tfmode) <- gs_genes

    gs_list <- list(tfmode = tfmode,likelihood = likelihood)
    pws_list[[gs_name]] <- gs_list
  }

  # read in expression data
  tpm <- as.matrix(read.table(tmp_expr_data_filepath,sep = "\t", header = T, row.names = 1, check.names = F))

  # transform gene expression to pathway activities
  nes <- aREA(tpm, pws_list, minsize = 2)$nes

  # write pathway activity matrix to files
  write.table(nes, file = outfile, quote = F, sep='\t', col.names = NA, row.names = T)

}


# get command line input
args <- commandArgs(trailingOnly=TRUE)

print(args)
if( length(args)>2){
  geneset_gmt_filepath <- args[1]
  tmp_expr_data_filepath <- args[2]
  outfile <- args[3]
  if(length(args)>3){
    analysis_method <- args[4]
  }else{
    analysis_method <- "BPA"
  }
  do_bpa_analysis(geneset_gmt_filepath,tmp_expr_data_filepath,outfile )
}
if(length(args)<=2){
  print("No arguments provided")
}

