package ca.bc.gov.educ.api.batchgraduation.util;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;

import ca.bc.gov.educ.api.batchgraduation.model.GraduationStatus;

@Service
@Scope(proxyMode = ScopedProxyMode.DEFAULT)
public class GradDataStore {

	private static final ThreadLocal<Map<String,Integer>> mapProgram = ThreadLocal.<Map<String,Integer>>withInitial(() -> {return new HashMap<String,Integer>();});
	private static final ThreadLocal<List<GraduationStatus>> processedList = ThreadLocal.<List<GraduationStatus>>withInitial(() -> {return new LinkedList<GraduationStatus>();});
	public void addProgram(String program) {
		 if(mapProgram != null) {
		        if(mapProgram.get().get(program) != null) {
		        	mapProgram.get().put(program, mapProgram.get().get(program) + 1);
		        }else {
		        	mapProgram.get().put(program, 1);
		        }
	        }else {
	        	mapProgram.get().put(program, 1);
	        }
	}
	
	public void addProcessedItem(GraduationStatus item) {
		processedList.get().add(item);
	}
	
	public int getSizeOfProcessedItem() {
		return processedList.get().size();
	}
	
	public ThreadLocal<Map<String, Integer>> getProgramMap() {
		return mapProgram;
	}
	
    public void clear() {
    	mapProgram.get().clear();    
    	processedList.get().clear();
    }
}