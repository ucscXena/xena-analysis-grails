
echo "TCGA_Acute_Myeloid_Leukemia__LAML_.tpm  `head -1 TCGA_Acute_Myeloid_Leukemia__LAML_.tpm |  tr '\t' '\n' | wc -l` "

TPM_TRIM_FILES=`ls *_trim.tpm`

for TPM_FILE in $TPM_TRIM_FILES; do 
    echo "$TPM_FILE `head -1 $TPM_FILE | tr '\t' '\n' | wc -l ` "
done 
