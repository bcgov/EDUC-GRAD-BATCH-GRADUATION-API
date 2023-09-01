package org.springframework.batch.core.repository.dao;

import ca.bc.gov.educ.api.batchgraduation.util.JsonTransformer;
import org.springframework.batch.core.repository.ExecutionContextSerializer;
import org.springframework.util.Assert;
import org.springframework.util.Base64Utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

public class DefaultExecutionContextSerializer implements ExecutionContextSerializer {

    JsonTransformer jsonTransformer;

    public DefaultExecutionContextSerializer() {
        jsonTransformer = new JsonTransformer();
        jsonTransformer.initMapper();
    }

    public void serialize(Map<String, Object> context, OutputStream out) throws IOException {
        Assert.notNull(context, "context is required");
        Assert.notNull(out, "OutputStream is required");
        byte[] serializedContext = jsonTransformer.marshall(context).getBytes();
        String base64EncodedContext = Base64Utils.encodeToString(serializedContext);
        out.write(base64EncodedContext.getBytes());
    }

    public Map<String, Object> deserialize(InputStream inputStream) throws IOException {
        String base64EncodedContext = new String(inputStream.readAllBytes());
        byte[] decodedContext = Base64Utils.decodeFromString(base64EncodedContext);
        return (Map)jsonTransformer.unmarshall(decodedContext, HashMap.class);
    }
}
