package ca.bc.gov.educ.api.batchgraduation.model;

import lombok.Data;

@Data
public class DistributionPrintRequest {

	private TranscriptPrintRequest transcriptPrintRequest;
	private CertificatePrintRequest yed2CertificatePrintRequest;
	private CertificatePrintRequest yedbCertificatePrintRequest;
	private CertificatePrintRequest yedrCertificatePrintRequest;
	private int total=0;
}
