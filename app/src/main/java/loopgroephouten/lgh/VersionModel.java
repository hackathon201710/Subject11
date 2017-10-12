package loopgroephouten.lgh;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by eddyspreeuwers on 10/11/17.
 */

public class VersionModel implements Parcelable{

    String name;
    String version;

    public VersionModel(Parcel in) {
        name = in.readString();
        version = in.readString();

    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

        dest.writeString(this.name);
        dest.writeString(this.version);
    }


    public static final Parcelable.Creator CREATOR = new Parcelable.Creator<VersionModel>() {
        public VersionModel createFromParcel(Parcel pc) {
            return new VersionModel(pc);
        }

        public VersionModel[] newArray(int size) {
            return new VersionModel[size];
        }
    };
    }