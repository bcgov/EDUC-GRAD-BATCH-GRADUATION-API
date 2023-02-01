package ca.bc.gov.educ.api.batchgraduation.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class DistributionPrintRequest {

	private TranscriptPrintRequest transcriptPrintRequest;
	private SchoolDistributionRequest schoolDistributionRequest;
	private CertificatePrintRequest yed2CertificatePrintRequest;
	private CertificatePrintRequest yedbCertificatePrintRequest;
	private CertificatePrintRequest yedrCertificatePrintRequest;
	private PsiCredentialPrintRequest psiCredentialPrintRequest;
	private String properName;
	private int total=0;

	public List<StudentCredentialDistribution> getMergedListOfCertificates() {
		List<StudentCredentialDistribution>	result = new ArrayList<>();
		if(yed2CertificatePrintRequest != null && yed2CertificatePrintRequest.getCertificateList() != null) {
			result.addAll(yed2CertificatePrintRequest.getCertificateList());
		}
		if(yedbCertificatePrintRequest != null && yedbCertificatePrintRequest.getCertificateList() != null) {
			result.addAll(yedbCertificatePrintRequest.getCertificateList());
		}
		if(yedrCertificatePrintRequest != null && yedrCertificatePrintRequest.getCertificateList() != null) {
			result.addAll(yedrCertificatePrintRequest.getCertificateList());
		}
		return result;
	}
}
