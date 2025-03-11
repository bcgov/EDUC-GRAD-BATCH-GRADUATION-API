package ca.bc.gov.educ.api.batchgraduation.controller;

import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ContextEventListener {

    private static ApplicationContext context;

    @EventListener
    public void onApplicationEvent(ContextRefreshedEvent event) {
        context = event.getApplicationContext();
    }

    public static ApplicationContext getApplicationContext() {
        return context;
    }
}
