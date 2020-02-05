package ai.connectus.opensync.util;

public class SslUtil {
    
    /**
     * Examples:<br>
     * <code>
     * subject=CN = PP302X-EX, C = US, O = Plume Design Inc., L = Palo Alto, ST = CA, emailAddress = support@plumewifi.com<br>
     * subject=C = CA, ST = Ontario, L = Ottawa, O = WhizControl Canada Inc, OU = Research and Develpoment, CN = dev-ap-0001, emailAddress = devadmin@123wlan.com
     * </code>
     * @param subjectDn
     * @return Value of the CN attribute of the supplied subject DN, or empty string if it cannot be extracted
     */
    public static String extractCN(String subjectDn) {
        if(subjectDn==null || subjectDn.isEmpty()) {
            return "";
        }
        String[] attrs = subjectDn.split(",");
        String tmp;
        int idx;
        for(String attr : attrs) {
            tmp = attr.trim();
            if(tmp.startsWith("CN")) {
                idx = tmp.indexOf('=');
                if(idx>0) {
                    return tmp.substring(idx + 1).trim();
                } else {
                    return "";
                }
            }
        }
        
        return "";

    }
}
