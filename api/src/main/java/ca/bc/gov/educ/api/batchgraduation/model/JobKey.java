package ca.bc.gov.educ.api.batchgraduation.model;

import lombok.Data;

@Data
public class JobKey {
    private int jId;
    private String jobUser;
    private String jobName;
    private String cronExpression;

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        JobKey jobKey = (JobKey) o;

        if (jId != jobKey.jId) {
            return false;
        }
        if (!jobName.equals(jobKey.jobName)) {
            return false;
        }
        return jobUser.equals(jobKey.jobUser);
    }

    @Override
    public int hashCode()
    {
        int result = jobUser.hashCode();
        result = 31 * result + jId;
        return result;
    }

}
