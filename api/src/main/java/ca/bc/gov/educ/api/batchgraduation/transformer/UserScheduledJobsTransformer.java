package ca.bc.gov.educ.api.batchgraduation.transformer;

import ca.bc.gov.educ.api.batchgraduation.entity.UserScheduledJobsEntity;
import ca.bc.gov.educ.api.batchgraduation.model.UserScheduledJobs;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


@Component
public class UserScheduledJobsTransformer {

    @Autowired
    ModelMapper modelMapper;

    public UserScheduledJobs transformToDTO (UserScheduledJobsEntity userScheduledJobsEntity) {
    	return modelMapper.map(userScheduledJobsEntity, UserScheduledJobs.class);
    }

    public UserScheduledJobs transformToDTO ( Optional<UserScheduledJobsEntity> userScheduledJobsEntity ) {
    	UserScheduledJobsEntity cae = new UserScheduledJobsEntity();
        if (userScheduledJobsEntity.isPresent())
            cae = userScheduledJobsEntity.get();

        return modelMapper.map(cae, UserScheduledJobs.class);
    }

	public List<UserScheduledJobs> transformToDTO (Iterable<UserScheduledJobsEntity> courseEntities ) {
		List<UserScheduledJobs> programList = new ArrayList<>();
        for (UserScheduledJobsEntity courseEntity : courseEntities) {
        	UserScheduledJobs program = modelMapper.map(courseEntity, UserScheduledJobs.class);
            programList.add(program);
        }
        return programList;
    }

    public UserScheduledJobsEntity transformToEntity(UserScheduledJobs gradBatchInfoDetails) {
        return modelMapper.map(gradBatchInfoDetails, UserScheduledJobsEntity.class);
    }
}
