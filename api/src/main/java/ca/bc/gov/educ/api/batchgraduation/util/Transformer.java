package ca.bc.gov.educ.api.batchgraduation.util;

import javax.xml.transform.TransformerException;
import java.io.InputStream;

public interface Transformer {

    public Object unmarshall(byte[] input, Class<?> clazz) throws TransformerException;

    public Object unmarshall(String input, Class<?> clazz) throws TransformerException;

    public Object unmarshall(InputStream input, Class<?> clazz);

    public String marshall(Object input);

    public String getAccept();

    public String getContentType();
}
