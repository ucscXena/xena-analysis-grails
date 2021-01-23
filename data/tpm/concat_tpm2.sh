#!/bin/bash
TPM_FILES=`ls *.tpm | grep -v TCGA_ALL  | grep -v trim | grep -v .gz`
#rm -f TCGA_ALL_fomratted.tpm
#touch TCGA_ALL_fomratted.tpm
echo $TPM_FILES
#echo "paste $TPM_FILES > TCGA_ALL.tpm"
#paste $TPM_FILES > TCGA_ALL.tpm

IS_FIRST=TRUE
TPM_TRIM_FILES=()
for TPM_FILE in $TPM_FILES
do
   echo "Processings $TPM_FILE with $IS_FIRST"
   if [ "$IS_FIRST" == "TRUE" ]; then
	   TPM_TRIM_FILES+=("${TPM_FILE}")
	   echo "Processing first $TPM_FILE"
	   IS_FIRST="FALSE"
   else
	   echo "Processing other $TPM_FILE"
#       cut -f2- $TPM_FILE > "${TPM_FILE}_trim.tpm"
	   echo "adding: ${TPM_FILE}_trim.tpm"
	   TPM_TRIM_FILES+=" ${TPM_FILE}_trim.tpm"
	   echo "after adding trimmed files: ${TPM_TRIM_FILES}"
   fi
done
echo "Trim files" 
echo $TPM_TRIM_FILES

paste $TPM_TRIM_FILES > TCGA_ALL_fomratted2.tpm
#for TPM_FILE in $TPM_FILES
#do
#   if IS_FIRST==0; then
#       cp $TPM_FILE TCGA_ALL.tpm
#   else
#       cut -f2- $TPM_FILE > "${TPM_FILE}_trim.tpm"
#       TPM_TRIM_FILES+=("${TPM_FILE}_trim.tpm")
#   fi
#done


