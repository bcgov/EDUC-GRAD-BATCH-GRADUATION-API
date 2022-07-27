package ca.bc.gov.educ.api.batchgraduation.util;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Map;

/**
 * The type JWT util.
 */
public class JwtUtil {

  private static final String FAMILYNAME = "family_name";
  private static final String GIVENNAME = "given_name";
  private JwtUtil() {
  }

  /**
   * Gets username string from object.
   *
   * @param jwt the JWT
   * @return the username string from jwt
   */
  public static String getUsername(Jwt jwt) {
    return (String) jwt.getClaims().get("preferred_username");
  }

  /**
   * Gets email string from object.
   *
   * @param jwt the JWT
   * @return the username string from jwt
   */
  public static String getEmail(Jwt jwt) {
    return (String) jwt.getClaims().get("email");
  }

  /**
   * Gets name string from object.
   *
   * @param jwt the JWT
   * @return the username string from jwt
   */
  public static String getName(Jwt jwt) {
    StringBuilder sb = new StringBuilder();
    if (isServiceAccount(jwt.getClaims())) {
      sb.append("Batch Process");
    } else {
      String givenName = (String) jwt.getClaims().get(GIVENNAME);
      if (StringUtils.isNotBlank(givenName)) {
        sb.append(givenName.charAt(0));
      }
      String familyName = (String) jwt.getClaims().get(FAMILYNAME);
      sb.append(familyName);
    }
    return sb.toString();
  }

  public static String getUserProperName(Jwt jwt) {
    StringBuilder sb = new StringBuilder();
    if (isServiceAccount(jwt.getClaims())) {
      sb.append("Batch Process");
    } else {
      String givenName = (String) jwt.getClaims().get(GIVENNAME);
      sb.append(givenName).append(" ");
      String familyName = (String) jwt.getClaims().get(FAMILYNAME);
      sb.append(familyName);
    }
    return sb.toString();
  }

  private static boolean isServiceAccount(Map<String, Object> claims) {
    return !claims.containsKey(FAMILYNAME);
  }
}
