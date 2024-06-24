package ca.bc.gov.educ.api.batchgraduation.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Period;
import java.util.Calendar;
import java.util.Date;

public class EducGradBatchGraduationApiUtils {

    private static final Logger logger = LoggerFactory.getLogger(EducGradBatchGraduationApiUtils.class);
    private static final String ERROR_MSG = "Error : {}";

    private EducGradBatchGraduationApiUtils() {
        throw new IllegalStateException("Utility class");
    }
    
    public static HttpHeaders getHeaders (String accessToken)
    {
		HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("Content-Type", "application/json");
        httpHeaders.setBearerAuth(accessToken);
        return httpHeaders;
    }
    
    public static HttpHeaders getHeaders (String username,String password)
    {
		HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        httpHeaders.setBasicAuth(username, password);
        return httpHeaders;
    }

    public static String formatDate(Date date, String dateFormat) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormat);
        return simpleDateFormat.format(date);
    }

    public static Date parseDate(String dateString, String dateFormat) throws ParseException {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormat);
        Date date = new Date();

        try {
            date = simpleDateFormat.parse(dateString);
        } catch (ParseException e) {
            logger.error(ERROR_MSG,e.getMessage());
        }

        return date;
    }

    public static Date parsingTraxDate(String sessionDate) {
        String actualSessionDate = sessionDate + "/01";
        Date temp;
        Date sDate = null;
        try {
            temp = EducGradBatchGraduationApiUtils.parseDate(actualSessionDate, EducGradBatchGraduationApiConstants.DATE_FORMAT);
            String sDates = EducGradBatchGraduationApiUtils.formatDate(temp, EducGradBatchGraduationApiConstants.DEFAULT_DATE_FORMAT);
            sDate = toLastDayOfMonth(EducGradBatchGraduationApiUtils.parseDate(sDates, EducGradBatchGraduationApiConstants.DEFAULT_DATE_FORMAT));
        } catch (ParseException pe) {
            logger.error(ERROR_MSG,pe.getMessage());
        }
        return sDate;
    }

    public static int getDifferenceInDays(String date1, String date2) {
        Period diff = Period.between(
                LocalDate.parse(date1),
                LocalDate.parse(date2));
        return diff.getDays() + diff.getMonths()*30;
    }

    public static String getCurrentDate() {

        Date gradDate = new Date();
        DateFormat dateFormat = new SimpleDateFormat(EducGradBatchGraduationApiConstants.DEFAULT_DATE_FORMAT);
        return dateFormat.format(gradDate);
    }

    public static String getProgramCompletionDate(Date pcd) {
        DateFormat dateFormat = new SimpleDateFormat(EducGradBatchGraduationApiConstants.DEFAULT_DATE_FORMAT);
        return dateFormat.format(pcd);
    }

    private static Date toLastDayOfMonth(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        return cal.getTime();
    }
	
}
