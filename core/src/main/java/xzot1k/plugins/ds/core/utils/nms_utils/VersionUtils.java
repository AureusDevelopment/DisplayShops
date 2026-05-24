package xzot1k.plugins.ds.core.utils.nms_utils;

public class VersionUtils {

    public static Version getVersion(String version, String compareVersion) {
        if (version == null || compareVersion == null) {
            return Version.UNKNOWN;
        }

        version = version.replaceAll("[^0-9.]+", "").trim();
        compareVersion = compareVersion.replaceAll("[^0-9.]+", "").trim();

        if (version.isEmpty() || compareVersion.isEmpty()) {
            return Version.UNKNOWN;
        }

        String[] primaryVersion = version.split("\\.");
        String[] compareToVersion = compareVersion.split("\\.");

        int max = Math.max(primaryVersion.length, compareToVersion.length);
        for (int i = 0; i <= max; ++i) {
            String number = i >= primaryVersion.length ? "0" : "1" + primaryVersion[i];
            if (compareToVersion.length <= i) {
                if (compareToVersion.length == i && compareToVersion.length == max) {
                    break;
                }
                return Version.NEWER_VERSION;
            }
            if (ParseUtils.getInt(number) > ParseUtils.getInt("1" + compareToVersion[i])) {
                return Version.NEWER_VERSION;
            }
            if (ParseUtils.getInt(number) < ParseUtils.getInt("1" + compareToVersion[i])) {
                return Version.OLDER_VERSION;
            }
        }
        return Version.SAME_VERSION;
    }

    public enum Version {
        OLDER_VERSION, NEWER_VERSION, SAME_VERSION, UNKNOWN
    }

}
