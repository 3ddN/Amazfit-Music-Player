package eduard.wink.amazfitmusicplayer;

import java.io.File;
import java.util.Comparator;

/**
 * Created by Eddy on 28.09.2018.
 */

public class FileNameComparator implements Comparator<File> {
    public int compare(File left, File right) {
            return left.getName().compareTo(right.getName());
    }
}
