package ca.bc.gov.educ.api.batchgraduation.listener;

import ca.bc.gov.educ.api.batchgraduation.model.*;

import java.util.List;
import java.util.Map;

public class SupportListener {
    private SupportListener() {
    }

    public static void psiPrintFile(List<PsiCredentialDistribution> yed4List, Long batchId, String usl, Map<String, DistributionPrintRequest> mapDist) {
        if(!yed4List.isEmpty()) {
            PsiCredentialPrintRequest tpReq = new PsiCredentialPrintRequest();
            tpReq.setBatchId(batchId);
            tpReq.setPsId(usl +" " +batchId);
            tpReq.setCount(yed4List.size());
            tpReq.setPsiList(yed4List);
            DistributionPrintRequest dist = new DistributionPrintRequest();
            dist.setPsiCredentialPrintRequest(tpReq);
            dist.setTotal(dist.getTotal()+1);
            mapDist.put(usl,dist);
        }
    }

    public static void transcriptPrintFile(List<StudentCredentialDistribution> yed4List, Long batchId, String usl, Map<String, DistributionPrintRequest> mapDist, String properName) {
        if(!yed4List.isEmpty()) {
            TranscriptPrintRequest tpReq = new TranscriptPrintRequest();
            tpReq.setBatchId(batchId);
            tpReq.setPsId(usl +" " +batchId);
            tpReq.setCount(yed4List.size());
            tpReq.setTranscriptList(yed4List);
            if(mapDist.get(usl) != null) {
                DistributionPrintRequest dist = mapDist.get(usl);
                dist.setTranscriptPrintRequest(tpReq);
                dist.setTotal(dist.getTotal()+1);
                dist.setProperName(properName);
                mapDist.put(usl,dist);
            }else{
                DistributionPrintRequest dist = new DistributionPrintRequest();
                dist.setTranscriptPrintRequest(tpReq);
                dist.setTotal(dist.getTotal()+1);
                dist.setProperName(properName);
                mapDist.put(usl,dist);
            }
        }
    }

    public static void certificatePrintFile(List<StudentCredentialDistribution> cList, Long batchId, String usl, Map<String,DistributionPrintRequest> mapDist, String certificatePaperType,String properName) {
        if(!cList.isEmpty()) {
            CertificatePrintRequest tpReq = new CertificatePrintRequest();
            tpReq.setBatchId(batchId);
            tpReq.setPsId(usl +" " +batchId);
            tpReq.setCount(cList.size());
            tpReq.setCertificateList(cList);
            if(mapDist.get(usl) != null) {
                DistributionPrintRequest dist = mapDist.get(usl);
                if(certificatePaperType.compareTo("YED2") == 0)
                    dist.setYed2CertificatePrintRequest(tpReq);
                if(certificatePaperType.compareTo("YEDR") == 0)
                    dist.setYedrCertificatePrintRequest(tpReq);
                if(certificatePaperType.compareTo("YEDB") == 0)
                    dist.setYedbCertificatePrintRequest(tpReq);
                dist.setTotal(dist.getTotal()+1);
                dist.setProperName(properName);
                mapDist.put(usl,dist);
            }else{
                DistributionPrintRequest dist = new DistributionPrintRequest();
                if(certificatePaperType.compareTo("YED2") == 0)
                    dist.setYed2CertificatePrintRequest(tpReq);
                if(certificatePaperType.compareTo("YEDR") == 0)
                    dist.setYedrCertificatePrintRequest(tpReq);
                if(certificatePaperType.compareTo("YEDB") == 0)
                    dist.setYedbCertificatePrintRequest(tpReq);
                dist.setTotal(dist.getTotal()+1);
                dist.setProperName(properName);
                mapDist.put(usl,dist);
            }
        }
    }

    public static void blankTranscriptPrintFile(List<BlankCredentialDistribution> yed4List, Long batchId, String usl, Map<String, DistributionPrintRequest> mapDist, String properName) {
        if(!yed4List.isEmpty()) {
            TranscriptPrintRequest tpReq = new TranscriptPrintRequest();
            tpReq.setBatchId(batchId);
            tpReq.setPsId(usl +" " +batchId);
            tpReq.setCount(yed4List.size());
            tpReq.setBlankTranscriptList(yed4List);
            if(mapDist.get(usl) != null) {
                DistributionPrintRequest dist = mapDist.get(usl);
                dist.setTranscriptPrintRequest(tpReq);
                dist.setTotal(dist.getTotal()+1);
                dist.setProperName(properName);
                mapDist.put(usl,dist);
            }else{
                DistributionPrintRequest dist = new DistributionPrintRequest();
                dist.setTranscriptPrintRequest(tpReq);
                dist.setTotal(dist.getTotal()+1);
                dist.setProperName(properName);
                mapDist.put(usl,dist);
            }
        }
    }

    public static void blankCertificatePrintFile(List<BlankCredentialDistribution> cList, Long batchId, String usl, Map<String,DistributionPrintRequest> mapDist, String certificatePaperType,String properName) {
        if(!cList.isEmpty()) {
            CertificatePrintRequest tpReq = new CertificatePrintRequest();
            tpReq.setBatchId(batchId);
            tpReq.setPsId(usl +" " +batchId);
            tpReq.setCount(cList.size());
            tpReq.setBlankCertificateList(cList);
            if(mapDist.get(usl) != null) {
                DistributionPrintRequest dist = mapDist.get(usl);
                if(certificatePaperType.compareTo("YED2") == 0)
                    dist.setYed2CertificatePrintRequest(tpReq);
                if(certificatePaperType.compareTo("YEDR") == 0)
                    dist.setYedrCertificatePrintRequest(tpReq);
                if(certificatePaperType.compareTo("YEDB") == 0)
                    dist.setYedbCertificatePrintRequest(tpReq);
                dist.setTotal(dist.getTotal()+1);
                dist.setProperName(properName);
                mapDist.put(usl,dist);
            }else{
                DistributionPrintRequest dist = new DistributionPrintRequest();
                if(certificatePaperType.compareTo("YED2") == 0)
                    dist.setYed2CertificatePrintRequest(tpReq);
                if(certificatePaperType.compareTo("YEDR") == 0)
                    dist.setYedrCertificatePrintRequest(tpReq);
                if(certificatePaperType.compareTo("YEDB") == 0)
                    dist.setYedbCertificatePrintRequest(tpReq);
                dist.setTotal(dist.getTotal()+1);
                dist.setProperName(properName);
                mapDist.put(usl,dist);
            }
        }
    }
}
